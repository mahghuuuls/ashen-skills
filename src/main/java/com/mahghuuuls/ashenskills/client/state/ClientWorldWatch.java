package com.mahghuuuls.ashenskills.client.state;

import com.mahghuuuls.ashenskills.client.animation.PlaybackRegistry;
import com.mahghuuuls.ashenskills.client.fx.SkillEffects;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps what the client remembers about skills from outliving its reason: everything is forgotten
 * when the player is in no world, and phase reports that ended without a closing message are
 * dropped once a second.
 */
public final class ClientWorldWatch {

    private static final int TICKS_BETWEEN_CLEANUPS = 20;

    private int ticksUntilCleanup = TICKS_BETWEEN_CLEANUPS;

    private ClientWorldWatch() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientWorldWatch());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().player == null) {
            ClientSkillState.clear();
            PlaybackRegistry.unpin();
            SkillEffects.reset();
        } else if (--ticksUntilCleanup <= 0) {
            ticksUntilCleanup = TICKS_BETWEEN_CLEANUPS;
            ClientSkillState.dropEndedPhases();
        }
    }
}
