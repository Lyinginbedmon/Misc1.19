package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.examplemod.capabilities.PlayerData;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(HumanoidModel.class)
public class PlayerModelMixin<T extends LivingEntity>
{
	@Inject(method = "setupAnim", at = @At("RETURN"))
	public void setupAnim(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo)
	{
		if(player.getType() == EntityType.PLAYER && PlayerData.getCapability((Player)player).isPraying())
			performPrayer((HumanoidModel<?>)(Object)this, ageInTicks);
	}
	
	// TODO Proper praying animation instead of Evoker wololo
	private static void performPrayer(HumanoidModel<?> model, float ageInTicks)
	{
        model.rightArm.z = 0.0F;
        model.rightArm.x = -5.0F;
        model.leftArm.z = 0.0F;
        model.leftArm.x = 5.0F;
        model.rightArm.xRot = Mth.cos(ageInTicks * 0.6662F) * 0.25F;
        model.leftArm.xRot = Mth.cos(ageInTicks * 0.6662F) * 0.25F;
        model.rightArm.zRot = 2.3561945F;
        model.leftArm.zRot = -2.3561945F;
        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;
	}
}
