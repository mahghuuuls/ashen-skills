package com.mahghuuuls.ashenskills.client.fx;

import com.mahghuuuls.ashenskills.client.animation.PlaybackRegistry;
import com.mahghuuuls.ashenskills.client.animation.clip.Bone;
import com.mahghuuuls.ashenskills.client.animation.clip.Pose;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Moves the user's camera with a skill: the tilt, recoil and shake a first-person clip writes on
 * its camera bone, scaled by the player's {@code cameraEffectIntensity}. Clips are written for
 * intensity 1; at 0 the camera is not touched. Only the own first-person view is moved.
 */
public final class CameraEffects {

    private CameraEffects() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CameraEffects());
    }

    /**
     * @param clipDegrees what the clip asks for at full intensity: pitch, yaw, roll
     * @param intensity the player's setting, 0 to 1
     * @return the degrees to add to the camera: pitch, yaw, roll
     */
    static float[] scaled(double[] clipDegrees, double intensity) {
        double factor = Math.max(0.0, Math.min(1.0, intensity));
        return new float[] {
            (float) (clipDegrees[0] * factor), (float) (clipDegrees[1] * factor), (float) (clipDegrees[2] * factor)
        };
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || event.getEntity() != minecraft.player
                || minecraft.gameSettings.thirdPersonView != 0) {
            return;
        }
        double intensity = Settings.general().getDouble(SettingKey.CAMERA_EFFECT_INTENSITY);
        if (intensity <= 0) {
            return;
        }
        Pose pose = PlaybackRegistry.poseFor(minecraft.player.getEntityId(), View.FIRST_PERSON);
        if (pose != null && minecraft.player.getPrimaryHand() == EnumHandSide.LEFT) {
            pose = pose.mirrored();
        }
        double[] camera = pose == null ? null : pose.rotationDegrees(Bone.CAMERA);
        if (camera == null) {
            return;
        }
        float[] added = scaled(camera, intensity);
        event.setPitch(event.getPitch() + added[0]);
        event.setYaw(event.getYaw() + added[1]);
        event.setRoll(event.getRoll() + added[2]);
    }
}
