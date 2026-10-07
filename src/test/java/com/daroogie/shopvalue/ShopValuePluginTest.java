package com.daroogie.shopvalue;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShopValuePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ShopValuePlugin.class);
        RuneLite.main(args);
    }
}