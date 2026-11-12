package com.mahghuuuls.ashenskills.mixin;

import com.mahghuuuls.ashenskills.client.render.HeldItemPose;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a skill clip turn the weapon inside the hand in the body view. Added where the game has
 * moved to the arm and has not yet placed the item, so the game's own placing follows the turn.
 */
@Mixin(LayerHeldItem.class)
public abstract class LayerHeldItemMixin {

    @Inject(
            method = "renderHeldItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V",
                    ordinal = 0))
    private void ashenskills$turnItemInHand(
            EntityLivingBase holder, ItemStack stack, ItemCameraTransforms.TransformType transformType,
            EnumHandSide side, CallbackInfo callback) {
        HeldItemPose.applyInBodyView(holder, side);
    }
}
