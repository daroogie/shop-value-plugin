package com.daroogie.shopvalue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("shopvalue")
public interface ShopValueConfig extends Config
{
    @ConfigItem(
        keyName = "showShopValue",
        name = "Enable Shop Value Tooltip",
        description = "Master toggle to show dynamic shop buy and sell prices on tooltips."
    )
    default boolean showShopValue()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showMarkdownWarning",
        name = "Show Markdown & Alch Warning",
        description = "Shows dynamic sell price degradation and flags if general store offer is lower than High Alch."
    )
    default boolean showMarkdownWarning()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showShopTotalValue",
        name = "Show Total Shop Value",
        description = "Displays the total GP value of all items currently in stock inside the shop interface."
    )
    default boolean showShopTotalValue()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showIronmanOverstock",
        name = "Ironman Overstock Warning",
        description = "Highlights items stocked above base shop default capacity that Ironmen cannot buy."
    )
    default boolean showIronmanOverstock()
    {
        return true;
    }
}