package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

public class ShopValueOverlay extends Overlay
{
    private final Client client;
    private final ShopValueConfig config;
    private final TooltipManager tooltipManager;
    private final ItemManager itemManager;

    @Inject
    public ShopValueOverlay(Client client, ShopValueConfig config, TooltipManager tooltipManager, ItemManager itemManager)
    {
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.config = config;
        this.tooltipManager = tooltipManager;
        this.itemManager = itemManager;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showShopValue())
        {
            return null;
        }

        // 1. Verify Shop Interface Parent
        Widget shopWidget = client.getWidget(InterfaceID.SHOP, 0);
        if (shopWidget == null || shopWidget.isHidden())
        {
            return null;
        }

        // 2. Get Hovered Target
        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries.length == 0)
        {
            return null;
        }

        MenuEntry topEntry = menuEntries[menuEntries.length - 1];
        Widget targetWidget = topEntry.getWidget();
        if (targetWidget == null)
        {
            return null;
        }

        int targetItemId = targetWidget.getItemId();
        if (targetItemId <= 0)
        {
            return null;
        }

        // Canonical item composition lookup
        ItemComposition itemComp = itemManager.getItemComposition(targetItemId);
        int canonicalId = itemComp.getId();
        int baseValue = itemComp.getPrice();
        if (baseValue <= 0)
        {
            return null;
        }

        // 3. Directly Scan Shop Item Container Widget (Child 16 is the shop item grid)
        int currentStock = 0;
        Widget shopItemGrid = client.getWidget(InterfaceID.SHOP, 16);
        if (shopItemGrid != null)
        {
            Widget[] items = shopItemGrid.getChildren();
            if (items != null)
            {
                for (Widget itemWidget : items)
                {
                    if (itemWidget != null && itemWidget.getItemId() > 0)
                    {
                        ItemComposition shopItemComp = itemManager.getItemComposition(itemWidget.getItemId());
                        if (shopItemComp.getId() == canonicalId || itemWidget.getItemId() == targetItemId)
                        {
                            currentStock += itemWidget.getItemQuantity();
                        }
                    }
                }
            }
        }

        int interfaceGroup = WidgetUtil.componentToInterface(targetWidget.getId());
        boolean isBuyingFromShop = (interfaceGroup == InterfaceID.SHOP);

        // 4. Check Shop Title (Child 2 contains shop name string)
        boolean isGeneralStore = false;
        Widget titleWidget = client.getWidget(InterfaceID.SHOP, 2);
        if (titleWidget != null && titleWidget.getText() != null)
        {
            if (titleWidget.getText().toLowerCase().contains("general store"))
            {
                isGeneralStore = true;
            }
        }

        double changePerItem = isGeneralStore ? 0.004 : 0.03;
        int calculatedPrice;
        String prefix;

        if (isBuyingFromShop)
        {
            double priceMultiplier = 1.30 + (changePerItem * Math.max(0, 1 - currentStock));
            calculatedPrice = (int) Math.max(1, Math.floor(baseValue * priceMultiplier));
            prefix = "Shop Sell Price: ";
        }
        else
        {
            double baseMultiplier = 0.40;
            double priceMultiplier = baseMultiplier - (changePerItem * currentStock);
            
            double minFloor = 0.10;
            if (priceMultiplier < minFloor)
            {
                priceMultiplier = minFloor;
            }

            calculatedPrice = (int) Math.max(1, Math.floor(baseValue * priceMultiplier));
            prefix = "Shop Buy Price: ";
        }

        // 5. Construct Tooltip Text
        String formattedPrice = QuantityFormatter.quantityToRSDecimalStack(calculatedPrice);
        String stockText = currentStock > 0 ? " (Stock: " + currentStock + ")" : " (Out of Stock)";

        String tooltipText = ColorUtil.wrapWithColorTag(
            prefix + formattedPrice + " gp" + stockText,
            new Color(255, 215, 0)
        );

        tooltipManager.add(new Tooltip(tooltipText));
        return null;
    }
}