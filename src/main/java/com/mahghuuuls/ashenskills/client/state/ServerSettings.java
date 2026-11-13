package com.mahghuuuls.ashenskills.client.state;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.SettingsView;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Makes this client use the pack configuration of the server it plays on, and its own again after
 * it leaves. In a single-player world the server is this same program and already uses this
 * installation's files, so nothing is replaced there.
 */
public final class ServerSettings {

    private ServerSettings() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ServerSettings());
    }

    /** The server's configuration arrived. Called on the client's main thread. */
    public static void received(SettingsView view) {
        boolean ownServer = Minecraft.getMinecraft().isIntegratedServerRunning();
        if (ownServer) {
            useOwnFiles();
        } else {
            SkillRegistry.useServerValues(view.appliedTo(SkillRegistry.registered()));
            WeaponTypes.useServerRules(view.typeRules());
        }
        if (Diagnostics.enabled()) {
            for (SkillDefinition skill : SkillRegistry.all()) {
                Diagnostics.log("SETTINGS_VIEW", "source={} skill={} cooldown={} weaponTypes={}",
                        ownServer ? "ownFiles" : "server", skill.id(), skill.number(SkillNumber.COOLDOWN),
                        skill.weaponTypes());
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Fired on the network thread. Done on the client's main thread, after any view that
        // arrived just before: otherwise that view would be applied after this and stay.
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                useOwnFiles();
            }
        });
    }

    private static void useOwnFiles() {
        SkillRegistry.restoreOwnValues();
        WeaponTypes.restoreLocalRules();
    }
}
