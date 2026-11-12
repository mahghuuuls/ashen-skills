package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.client.animation.PlaybackRegistry;
import com.mahghuuuls.ashenskills.client.animation.clip.Bone;
import com.mahghuuuls.ashenskills.client.animation.clip.Pose;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHandSide;

/**
 * Puts a clip's pose on a player's body model, after the game has set the model's own angles. It
 * runs for the body and for each armor layer, which are separate models posed the same way.
 *
 * <p>Only what the game sets again for every draw is written: the rotation of the arms and legs on
 * all three axes, and of head and body around x and y. The game never resets the z rotation of
 * head and body or the place of a part, so writing those would leave the model bent after the
 * clip. A clip's position tracks are therefore for the first-person view only.
 */
public final class ThirdPersonPoseApplier {

    /** Distance of a shoulder from the middle of the body, in model units. */
    private static final float SHOULDER_OFFSET = 5.0F;

    private ThirdPersonPoseApplier() {
    }

    /**
     * Called before the game poses a biped model. While a clip plays for a player, the game's own
     * attack swing is left out: it would twist the body and move the shoulders under the clip, and
     * show through on every bone the clip does not name. The game sets the swing, and the sneak
     * pose, again for each draw.
     */
    public static void beforeGamePose(ModelBiped model, Entity entity) {
        if (!(entity instanceof AbstractClientPlayer) || isOwnBodyUnseen(entity)) {
            return;
        }
        if (model.swingProgress > 0.0F && PlaybackRegistry.isPlaying(entity.getEntityId())) {
            model.swingProgress = 0.0F;
        }
        // A player under a sneak lock stands up, but others learn it a moment late; until then the
        // model would mix the sneak pose with a clip made for a standing body.
        if (model.isSneak && ClientSkillState.isSneakLocked(entity.getEntityId())) {
            model.isSneak = false;
        }
    }

    /** Called after the game posed a biped model; does nothing unless a clip plays for a player. */
    public static void apply(ModelBiped model, Entity entity) {
        if (!(entity instanceof AbstractClientPlayer) || isOwnBodyUnseen(entity)) {
            return;
        }
        Pose pose = PlaybackRegistry.poseFor(entity.getEntityId(), View.THIRD_PERSON);
        if (pose == null) {
            return;
        }
        if (((AbstractClientPlayer) entity).getPrimaryHand() == EnumHandSide.LEFT) {
            pose = pose.mirrored();
        }
        rotate(model.bipedRightArm, pose.rotationRadians(Bone.RIGHT_ARM), true);
        rotate(model.bipedLeftArm, pose.rotationRadians(Bone.LEFT_ARM), true);
        rotate(model.bipedRightLeg, pose.rotationRadians(Bone.RIGHT_LEG), true);
        rotate(model.bipedLeftLeg, pose.rotationRadians(Bone.LEFT_LEG), true);
        rotate(model.bipedHead, pose.rotationRadians(Bone.HEAD), false);
        ModelBiped.copyModelAngles(model.bipedHead, model.bipedHeadwear);
        float[] body = pose.rotationRadians(Bone.BODY);
        if (body != null) {
            rotate(model.bipedBody, body, false);
            // The shoulders sit on the body: when it turns, they go around with it, as in the game's own swing.
            float sine = (float) Math.sin(body[1]);
            float cosine = (float) Math.cos(body[1]);
            model.bipedRightArm.rotationPointZ = sine * SHOULDER_OFFSET;
            model.bipedRightArm.rotationPointX = -cosine * SHOULDER_OFFSET;
            model.bipedLeftArm.rotationPointZ = -sine * SHOULDER_OFFSET;
            model.bipedLeftArm.rotationPointX = cosine * SHOULDER_OFFSET;
        }
        PlaybackRegistry.notePoseApplied(View.THIRD_PERSON);
    }

    /**
     * In first person the user's body is not drawn. The game still poses the body model to draw
     * the bare hand, and that hand belongs to the first-person view, not to the body clip.
     */
    private static boolean isOwnBodyUnseen(Entity entity) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return entity == minecraft.getRenderViewEntity() && minecraft.gameSettings.thirdPersonView == 0;
    }

    private static void rotate(ModelRenderer part, float[] radians, boolean withZ) {
        if (radians == null) {
            return;
        }
        part.rotateAngleX = radians[0];
        part.rotateAngleY = radians[1];
        if (withZ) {
            part.rotateAngleZ = radians[2];
        }
    }
}
