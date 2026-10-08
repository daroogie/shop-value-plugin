package com.daroogie.shopvalue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern SELL_QUANTITY_PATTERN = Pattern.compile("^Sell\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

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

        // 2. Check Shop Title (Child 2) to determine store type
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

        // 3. Scan Shop Grid (Child 16) for Total Shop Value
        long totalShopValue = 0;
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
                        int itemId = itemWidget.getItemId();
                        int stock = itemWidget.getItemQuantity();

                        if (stock > 0)
                        {
                            ItemComposition itemComp = itemManager.getItemComposition(itemId);
                            int baseVal = itemComp.getPrice();
                            double priceMultiplier = Math.max(0.10, 0.40 - (changePerItem * stock));
                            int sellVal = (int) Math.max(1, Math.floor(baseVal * priceMultiplier));
                            totalShopValue += (long) sellVal * stock;
                        }
                    }
                }
            }
        }

        // 4. Get Hovered Target
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
        int highAlch = itemComp.getHaPrice();
        if (baseValue <= 0)
        {
            return null;
        }

        // Scan target stock in shop grid
        int currentStock = 0;
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

        int calculatedPrice;
        int nextSalePrice = 0;
        String prefix;

        if (isBuyingFromShop)
        {
            double priceMultiplier = 1.30 + (changePerItem * Math.max(0, 1 - currentStock));
            calculatedPrice = (int) Math.max(1, Math.floor(baseValue * priceMultiplier));
            prefix = "Shop Sell Price: ";
        }
        else
        {
            // Current Offer Price
            double baseMultiplier = 0.40;
            double priceMultiplier = Math.max(0.10, baseMultiplier - (changePerItem * currentStock));
            calculatedPrice = (int) Math.max(1, Math.floor(baseValue * priceMultiplier));

            // Next Sale Price (after selling 1 more item)
            double nextMultiplier = Math.max(0.10, baseMultiplier - (changePerItem * (currentStock + 1)));
            nextSalePrice = (int) Math.max(1, Math.floor(baseValue * nextMultiplier));

            prefix = "Shop Buy Price: ";
        }

        // 5. Construct Tooltip Text
        StringBuilder sb = new StringBuilder();

        String formattedPrice = QuantityFormatter.quantityToRSDecimalStack(calculatedPrice);
        String stockText = currentStock > 0 ? " (Stock: " + currentStock + ")" : " (Out of Stock)";

        sb.append(prefix).append(formattedPrice).append(" gp").append(stockText);

        // Minimum Price Floor Warnings & Remaining Sales Calculation
        if (!isBuyingFromShop)
        {
            int minFloor = config.minSellThreshold();

            // Dynamic calculation: How many more can be sold before price drops below minFloor
            double minMultiplier = (double) minFloor / baseValue;
            int maxSalesBeforeFloor = (int) Math.floor((0.40 - minMultiplier) / changePerItem) - currentStock;

            sb.append("</br>Next Sale: ").append(nextSalePrice).append(" gp");

            if (maxSalesBeforeFloor > 0)
            {
                sb.append("</br><col=00ff00>Can sell ").append(maxSalesBeforeFloor).append(" more before <").append(minFloor).append("gp</col>");
            }
            else
            {
                sb.append("</br><col=ff0000>Warning: At/Below ").append(minFloor).append("gp floor!</col>");
            }

            // Check if hovering a bulk menu option (e.g., "Sell 1", "Sell 5", "Sell 10", "Sell 50")
            String option = topEntry.getOption();
            if (option != null)
            {
                Matcher matcher = SELL_QUANTITY_PATTERN.matcher(option.trim());
                if (matcher.find())
                {
                    try
                    {
                        int amount = Integer.parseInt(matcher.group(1));
                        if (amount > 1)
                        {
                            sb.append(calculateBulkSellInfo(baseValue, currentStock, changePerItem, amount));
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        // Ignore overflow
                    }
                }
            }
        }

        // General Store Markdown vs. High Alch Warning
        if (config.showMarkdownWarning() && !isBuyingFromShop)
        {
            sb.append("</br>High Alch: ").append(QuantityFormatter.quantityToRSDecimalStack(highAlch)).append(" gp");
            if (calculatedPrice < highAlch)
            {
                sb.append(" <col=ff0000>(Below High Alch!)</col>");
            }
        }

        // Ironman Stock Warning
        if (config.showIronmanOverstock() && isBuyingFromShop && currentStock > 0)
        {
            sb.append("</br><col=ffa500>Ironman Stock Available: ").append(currentStock).append("</col>");
        }

        // Total Shop Value
        if (config.showShopTotalValue())
        {
            int safeTotal = (int) Math.min(totalShopValue, Integer.MAX_VALUE);
            sb.append("</br><col=00ff00>Shop Total Stock: ")
              .append(QuantityFormatter.quantityToRSDecimalStack(safeTotal))
              .append(" gp</col>");
        }

        String tooltipText = ColorUtil.wrapWithColorTag(sb.toString(), new Color(255, 215, 0));
        tooltipManager.add(new Tooltip(tooltipText));

        return null;
    }

    private String calculateBulkSellInfo(int baseValue, int currentStock, double changePerItem, int quantity)
    {
        long totalRevenue = 0;
        double baseMultiplier = 0.40;

        for (int i = 0; i < quantity; i++)
        {
            double priceMultiplier = Math.max(0.10, baseMultiplier - (changePerItem * (currentStock + i)));
            int itemPrice = (int) Math.max(1, Math.floor(baseValue * priceMultiplier));
            totalRevenue += itemPrice;
        }

        int avgPrice = (int) (totalRevenue / quantity);
        
        return "</br><col=ffff00>Sell " + quantity + " Total: " 
            + QuantityFormatter.quantityToRSDecimalStack((int) Math.min(totalRevenue, Integer.MAX_VALUE)) 
            + " gp (Avg: " + avgPrice + " gp/ea)</col>";
    }
}