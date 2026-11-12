package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.client.ClientSwitches;
import com.mahghuuuls.ashenskills.client.animation.PlaybackRegistry;
import com.mahghuuuls.ashenskills.client.animation.clip.Bone;
import com.mahghuuuls.ashenskills.client.animation.clip.Pose;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the user's own weapon arm while a skill clip plays: the game's drawing of the main hand is
 * replaced by the player's arm, posed by the clip, with the weapon in its hand exactly as the game
 * holds it in third person. The other hand, and both hands when no clip plays, are the game's own.
 *
 * <p>One bone drives the picture: the main arm. Its position is where the shoulder is, seen from
 * the eyes, in model units (1/16 block): x to the right, y up, z toward the viewer. Its rotation
 * has the same meaning as on the body model.
 */
public final class FirstPersonSkillRenderer {

    private static final float MODEL_UNIT = 0.0625F;

    private FirstPersonSkillRenderer() {
    }

    public static void register() {
        if (ClientSwitches.isLeftOut(ClientSwitches.FIRST_PERSON_VIEW)) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new FirstPersonSkillRenderer());
    }

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Pose pose = PlaybackRegistry.poseFor(player.getEntityId(), View.FIRST_PERSON);
        if (pose == null) {
            return;
        }
        EnumHandSide side = player.getPrimaryHand();
        if (side == EnumHandSide.LEFT) {
            pose = pose.mirrored();
        }
        Bone armBone = side == EnumHandSide.LEFT ? Bone.LEFT_ARM : Bone.RIGHT_ARM;
        float[] rotation = pose.rotationRadians(armBone);
        double[] shoulder = pose.position(armBone);
        Render<AbstractClientPlayer> render = minecraft.getRenderManager().getEntityRenderObject(player);
        if (rotation == null || shoulder == null || !(render instanceof RenderPlayer)) {
            return;
        }
        event.setCanceled(true);
        ModelPlayer model = ((RenderPlayer) render).getMainModel();
        boolean left = side == EnumHandSide.LEFT;
        boolean sleeveWorn = player.isWearing(left ? EnumPlayerModelParts.LEFT_SLEEVE : EnumPlayerModelParts.RIGHT_SLEEVE);
        drawArmWithWeapon(
                minecraft, player, event.getItemStack(), left,
                left ? model.bipedLeftArm : model.bipedRightArm,
                sleeveWorn ? (left ? model.bipedLeftArmwear : model.bipedRightArmwear) : null,
                rotation, shoulder);
        PlaybackRegistry.notePoseApplied(View.FIRST_PERSON);
    }

    /**
     * Every value changed on the shared body model is put back, and the matrix is restored.
     *
     * @param sleeve the outer skin layer of the arm, or null when the player has it switched off
     */
    private static void drawArmWithWeapon(
            Minecraft minecraft, AbstractClientPlayer player, ItemStack weapon, boolean left,
            ModelRenderer arm, ModelRenderer sleeve, float[] rotation, double[] shoulder) {
        float pointX = arm.rotationPointX;
        float pointY = arm.rotationPointY;
        float pointZ = arm.rotationPointZ;
        float angleX = arm.rotateAngleX;
        float angleY = arm.rotateAngleY;
        float angleZ = arm.rotateAngleZ;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(shoulder[0] * MODEL_UNIT, shoulder[1] * MODEL_UNIT, shoulder[2] * MODEL_UNIT);
            // The body model is built upside down and mirrored; the game turns it the same way.
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            arm.rotationPointX = 0.0F;
            arm.rotationPointY = 0.0F;
            arm.rotationPointZ = 0.0F;
            arm.rotateAngleX = rotation[0];
            arm.rotateAngleY = rotation[1];
            arm.rotateAngleZ = rotation[2];
            minecraft.getTextureManager().bindTexture(player.getLocationSkin());
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            // Skins may have see-through sleeve pixels; the game draws its own first-person arm blended too.
            GlStateManager.enableBlend();
            arm.render(MODEL_UNIT);
            if (sleeve != null) {
                ModelPlayer.copyModelAngles(arm, sleeve);
                sleeve.render(MODEL_UNIT);
            }
            GlStateManager.disableBlend();
            if (!weapon.isEmpty()) {
                // The same steps as the game's held-item layer, so the weapon sits in the hand as in third person.
                arm.postRender(MODEL_UNIT);
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
                minecraft.getItemRenderer().renderItemSide(
                        player, weapon,
                        left ? TransformType.THIRD_PERSON_LEFT_HAND : TransformType.THIRD_PERSON_RIGHT_HAND, left);
            }
        } finally {
            GlStateManager.popMatrix();
            arm.rotationPointX = pointX;
            arm.rotationPointY = pointY;
            arm.rotationPointZ = pointZ;
            arm.rotateAngleX = angleX;
            arm.rotateAngleY = angleY;
            arm.rotateAngleZ = angleZ;
            if (sleeve != null) {
                ModelPlayer.copyModelAngles(arm, sleeve);
            }
        }
    }
}
