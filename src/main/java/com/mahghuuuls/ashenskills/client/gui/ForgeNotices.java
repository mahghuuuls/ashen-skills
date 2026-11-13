package com.mahghuuuls.ashenskills.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Lets a player at an open Ashen Forge read why the Forge refused an apply. The server shows that
 * line above the hotbar, a place the Forge window covers, so the open window shows it too. Lines
 * of anything else are left alone.
 */
public final class ForgeNotices {

    private static final String REFUSAL_KEYS = "ashenskills.forge.refused.";

    private ForgeNotices() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ForgeNotices.class);
    }

    @SubscribeEvent
    public static void onLineAboveHotbar(ClientChatReceivedEvent event) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (event.getType() == ChatType.GAME_INFO && screen instanceof ForgeScreen
                && event.getMessage() instanceof TextComponentTranslation
                && ((TextComponentTranslation) event.getMessage()).getKey().startsWith(REFUSAL_KEYS)) {
            ((ForgeScreen) screen).showNotice(event.getMessage().getUnformattedText());
        }
    }
}
