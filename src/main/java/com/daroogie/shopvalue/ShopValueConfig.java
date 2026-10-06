package com.daroogie.shopvalue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("shopvalue")
public interface ShopValueConfig extends Config
{
    @ConfigItem(
        keyName = "showShopValue",
        name = "Show Shop Value",
        description = "Displays estimated shop purchase prices on item tooltips when a shop is open."
    )
    default boolean showShopValue()
    {
        return true;
    }
}