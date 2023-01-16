package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.examplemod.api.event.LivingConsumableEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
	@Shadow
	public UseAnim getUseAnimation() { return UseAnim.NONE; }
	
	@Inject(method = "finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	public void finishUsingItem(Level world, LivingEntity living, final CallbackInfoReturnable<ItemStack> ci)
	{
		ItemStack stack = (ItemStack)(Object)this;
		LivingConsumableEvent event = null;
		switch(getUseAnimation())
		{
			case DRINK:
				event = new LivingConsumableEvent.Drink(living, stack);
				break;
			case EAT:
				event = new LivingConsumableEvent.Eat(living, stack);
				break;
			default:
				break;
		}
		
		if(event != null && MinecraftForge.EVENT_BUS.post(event))
			ci.setReturnValue(stack);
	}
}
