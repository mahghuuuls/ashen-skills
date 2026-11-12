package com.mahghuuuls.ashenskills.mixin;

import com.mahghuuuls.ashenskills.client.render.ThirdPersonPoseApplier;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a skill clip pose a player's body. The pose is added at the end of the game's own pose
 * setup, so the clip is put on top of whatever the game and other mods set before.
 */
@Mixin(ModelBiped.class)
public abstract class ModelBipedMixin {

    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void ashenskills$beforeGamePose(
            float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scaleFactor, Entity entity, CallbackInfo callback) {
        ThirdPersonPoseApplier.beforeGamePose((ModelBiped) (Object) this, entity);
    }

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
    private void ashenskills$applySkillPose(
            float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scaleFactor, Entity entity, CallbackInfo callback) {
        ThirdPersonPoseApplier.apply((ModelBiped) (Object) this, entity);
    }
}
