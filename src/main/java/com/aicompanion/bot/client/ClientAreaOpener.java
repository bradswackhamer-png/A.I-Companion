package com.aicompanion.bot.client;

import net.minecraft.client.Minecraft;

public class ClientAreaOpener {
    public static void openScreen() {
        Minecraft.getInstance().setScreenAndShow(new AreaConfigScreen());
    }
}
