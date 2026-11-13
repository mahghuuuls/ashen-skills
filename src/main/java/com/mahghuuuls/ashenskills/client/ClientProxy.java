package com.mahghuuuls.ashenskills.client;

import com.mahghuuuls.ashenskills.ashenforge.ForgeContainer;
import com.mahghuuuls.ashenskills.client.gui.ForgeScreen;
import com.mahghuuuls.ashenskills.client.hud.SkillHud;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.animation.ClipLibrary;
import com.mahghuuuls.ashenskills.client.animation.PoseControls;
import com.mahghuuuls.ashenskills.client.fx.CameraEffects;
import com.mahghuuuls.ashenskills.client.fx.SkillEffects;
import com.mahghuuuls.ashenskills.client.hud.RefusalDisplay;
import com.mahghuuuls.ashenskills.client.input.SkillInputLock;
import com.mahghuuuls.ashenskills.client.input.SkillKeyHandler;
import com.mahghuuuls.ashenskills.client.render.FirstPersonSkillRenderer;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.client.state.ClientWorldWatch;
import com.mahghuuuls.ashenskills.client.state.ServerSettings;
import com.mahghuuuls.ashenskills.config.SettingsView;
import com.mahghuuuls.ashenskills.proxy.CommonProxy;
import net.minecraft.client.Minecraft;

/** Client side of {@link CommonProxy}. Loaded by name on the physical client only. */
public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        SkillKeyHandler.register();
        ClipLibrary.register();
        ClientWorldWatch.register();
        SkillInputLock.register();
        FirstPersonSkillRenderer.register();
        CameraEffects.register();
        ServerSettings.register();
        SkillHud.register();
    }

    @Override
    public Object forgeScreen(ForgeContainer forge) {
        return new ForgeScreen(forge);
    }

    @Override
    public void onSettingsView(final SettingsView view) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ServerSettings.received(view);
            }
        });
    }

    @Override
    public void onPoseControl(
            final byte action, final String skillId, final SkillPhase phase, final double seconds, final int cameraView) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                PoseControls.handle(action, skillId, phase, seconds, cameraView);
            }
        });
    }

    @Override
    public void onPhaseUpdate(
            final int entityId, final String skillId, final byte phaseCode, final int elapsedTicks, final int lengthTicks,
            final boolean attackLocked, final boolean jumpLocked, final boolean sneakLocked) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientSkillState.setPhase(
                        entityId, skillId, phaseCode, elapsedTicks, lengthTicks, attackLocked, jumpLocked,
                        sneakLocked);
            }
        });
    }

    @Override
    public void onEffectCue(
            final byte kind, final int userEntityId, final int useId, final String skillId,
            final double x, final double y, final double z, final int particles) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                SkillEffects.play(kind, userEntityId, useId, skillId, x, y, z, particles);
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
