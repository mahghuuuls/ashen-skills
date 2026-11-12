package com.mahghuuuls.ashenskills.client.animation;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.network.PoseControlMessage;
import net.minecraft.client.Minecraft;

/**
 * The animation check controls of this client: hold a clip at a time, write the current pose to
 * the log, and choose the camera view, so an animation can be checked with still pictures and
 * numbers. They act only when this client's {@code animationCheckControls} setting is on; an
 * operator's request does nothing otherwise.
 */
public final class PoseControls {

    private PoseControls() {
    }

    /**
     * @param action one of the action codes of {@link PoseControlMessage}
     * @param phase for a pin, the phase whose clip to hold
     * @param seconds for a pin, the time in the clip to hold
     * @param cameraView for a view change: 0 first person, 1 from behind, 2 from the front
     */
    public static void handle(byte action, String skillId, SkillPhase phase, double seconds, int cameraView) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            return;
        }
        if (!Settings.general().getBoolean(SettingKey.ANIMATION_CHECK_CONTROLS)) {
            AshenSkillsMod.LOGGER.info("[POSE_CONTROL] ignored: animationCheckControls is off on this client");
            return;
        }
        switch (action) {
            case PoseControlMessage.PIN:
                if (phase != null) {
                    PlaybackRegistry.pin(skillId, phase, seconds);
                    AshenSkillsMod.LOGGER.info("[POSE_CONTROL] pin skill={} phase={} seconds={}", skillId, phase, seconds);
                }
                break;
            case PoseControlMessage.UNPIN:
                PlaybackRegistry.unpin();
                AshenSkillsMod.LOGGER.info("[POSE_CONTROL] unpin");
                break;
            case PoseControlMessage.DUMP:
                for (View view : View.values()) {
                    AshenSkillsMod.LOGGER.info("[POSE_DUMP] view={} cameraView={} {}",
                            view.fileName(), minecraft.gameSettings.thirdPersonView,
                            PlaybackRegistry.describe(minecraft.player.getEntityId(), view));
                }
                break;
            case PoseControlMessage.VIEW:
                if (cameraView >= 0 && cameraView <= 2) {
                    minecraft.gameSettings.thirdPersonView = cameraView;
                    AshenSkillsMod.LOGGER.info("[POSE_CONTROL] cameraView={}", cameraView);
                }
                break;
            default:
                break;
        }
    }
}
