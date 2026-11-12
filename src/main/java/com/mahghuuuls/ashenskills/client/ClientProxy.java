package com.mahghuuuls.ashenskills.client;

import com.mahghuuuls.ashenskills.client.hud.RefusalDisplay;
import com.mahghuuuls.ashenskills.client.input.SkillKeyHandler;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.proxy.CommonProxy;
import net.minecraft.client.Minecraft;

/** Client side of {@link CommonProxy}. Loaded by name on the physical client only. */
public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        SkillKeyHandler.register();
    }

    @Override
    public void onPhaseUpdate(
            final int entityId, final String skillId, final byte phaseCode, final int elapsedTicks, final int lengthTicks) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientSkillState.setPhase(entityId, skillId, phaseCode, elapsedTicks, lengthTicks);
            }
        });
    }

    @Override
    public void onRefusal(final String messageKey) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                RefusalDisplay.show(messageKey);
            }
        });
    }

    @Override
    public void onCooldownState(final long remainingMillis, final long lengthMillis) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientSkillState.setOwnCooldown(remainingMillis, lengthMillis);
            }
        });
    }
}
