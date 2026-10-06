package com.example;

import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Shop Value Tooltip",
    description = "Displays shop buy prices on item tooltips while in a shop window",
    tags = {"shop", "tooltip", "prices"}
)
public class ShopValuePlugin extends Plugin
{
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ShopValueOverlay overlay;

    @Inject
    private ShopValueConfig config;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    @Provides
    ShopValueConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ShopValueConfig.class);
    }
}