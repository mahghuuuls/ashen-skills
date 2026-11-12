package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.client.animation.PlaybackRegistry;
import com.mahghuuuls.ashenskills.client.animation.clip.Bone;
import com.mahghuuuls.ashenskills.client.animation.clip.Pose;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;

/**
 * Turns the weapon inside the hand as a clip's item bone says. The game always holds an item at a
 * right angle to the arm; a clip can lay it in line with the arm, or flat for a side slash. The
 * turn is made around the middle of the fist, so the handle stays in the hand.
 *
 * <p>Both views use this: the body view from inside the game's own held-item drawing, and the
 * first-person view from the mod's drawing of the weapon arm. It must be called when the drawing
 * position is at the arm (after the arm's own move and turn) and before the game's steps that
 * place the item.
 */
public final class HeldItemPose {

    /**
     * The middle of the fist, seen from the arm's joint: on the arm's center line (one model unit
     * to the side of the joint), eight units down the arm. In the game's own hold the handle
     * passes through this point, so a turn around it keeps the handle inside the fist. The point
     * where the game starts to draw the item is not used: it lies at the edge of the hand, and a
     * turn around it carries the handle out of the fist.
     */
    private static final float FIST_ALONG_ARM = 0.5F;
    private static final float FIST_SIDE = 0.0625F;

    private HeldItemPose() {
    }

    /**
     * For the game's held-item drawing of the body view. Does nothing unless a clip with an item
     * bone plays for a player and the hand is that player's weapon hand.
     */
    public static void applyInBodyView(EntityLivingBase holder, EnumHandSide side) {
        if (!(holder instanceof AbstractClientPlayer)) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) holder;
        if (player.getPrimaryHand() != side || ThirdPersonPoseApplier.isOwnBodyUnseen(player)) {
            // The body pose is left alone in the same cases, so arm and weapon stay together.
            return;
        }
        Pose pose = PlaybackRegistry.poseFor(player.getEntityId(), View.THIRD_PERSON);
        if (pose == null) {
            return;
        }
        boolean left = side == EnumHandSide.LEFT;
        turnAroundGrip((left ? pose.mirrored() : pose).rotationDegrees(Bone.ITEM), left);
    }

    /**
     * @param degrees the item bone's rotation around the arm's x, y and z axis, or null for none
     * @param left whether the weapon is in the left hand
     */
    static void turnAroundGrip(double[] degrees, boolean left) {
        if (degrees == null) {
            return;
        }
        float side = left ? FIST_SIDE : -FIST_SIDE;
        GlStateManager.translate(side, FIST_ALONG_ARM, 0.0F);
        GlStateManager.rotate((float) degrees[2], 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate((float) degrees[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) degrees[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-side, -FIST_ALONG_ARM, 0.0F);
    }
}
