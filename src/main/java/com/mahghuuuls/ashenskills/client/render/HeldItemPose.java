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
 * turn is made around the grip, so the handle stays in the fist.
 *
 * <p>Both views use this: the body view from inside the game's own held-item drawing, and the
 * first-person view from the mod's drawing of the weapon arm. It must be called when the drawing
 * position is at the arm (after the arm's own move and turn) and before the game's steps that
 * place the item.
 */
public final class HeldItemPose {

    /**
     * Where the game puts an item's grip, seen from the arm's joint: down the arm and a little
     * forward. These are the game's own held-item offsets (1/16, 0.125, -0.625) after its two
     * placing turns; the first-person drawing uses the same game steps unturned.
     */
    private static final float GRIP_ALONG_ARM = 0.625F;
    private static final float GRIP_FORWARD = -0.125F;
    private static final float GRIP_SIDE = 0.0625F;

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
        float side = left ? GRIP_SIDE : -GRIP_SIDE;
        GlStateManager.translate(side, GRIP_ALONG_ARM, GRIP_FORWARD);
        GlStateManager.rotate((float) degrees[2], 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate((float) degrees[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) degrees[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-side, -GRIP_ALONG_ARM, -GRIP_FORWARD);
    }
}
