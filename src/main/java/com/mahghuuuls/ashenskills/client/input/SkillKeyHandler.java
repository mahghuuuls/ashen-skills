package com.mahghuuuls.ashenskills.client.input;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.SkillInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * The skill key. Tells the server when the key goes down and when it comes up; the server decides
 * what that means.
 */
public final class SkillKeyHandler {

    private final KeyBinding skillKey;
    private boolean wasDown;

    private SkillKeyHandler(KeyBinding skillKey) {
        this.skillKey = skillKey;
    }

    public static void register() {
        String defaultKeyName =
                Settings.defaults().general(SettingKey.SKILL_KEY.jsonName()).defaultValue().getAsString();
        int defaultKeyCode = Keyboard.getKeyIndex(defaultKeyName);
        if (defaultKeyCode == Keyboard.KEY_NONE) {
            AshenSkillsMod.LOGGER.error(
                    "Default skill key '{}' is not a key name; the skill key starts unbound", defaultKeyName);
        }
        KeyBinding binding = new KeyBinding(
                "key.ashenskills.skill",
                KeyConflictContext.IN_GAME,
                defaultKeyCode,
                "key.categories.ashenskills");
        ClientRegistry.registerKeyBinding(binding);
        MinecraftForge.EVENT_BUS.register(new SkillKeyHandler(binding));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            wasDown = false;
            return;
        }
        boolean down = skillKey.isKeyDown() && minecraft.currentScreen == null;
        if (down != wasDown) {
            wasDown = down;
            AshenNetwork.channel().sendToServer(
                    new SkillInputMessage(down ? SkillInputMessage.KEY_DOWN : SkillInputMessage.KEY_UP));
        }
    }
}
