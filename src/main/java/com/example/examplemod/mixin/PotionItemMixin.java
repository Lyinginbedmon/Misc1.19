package com.example.examplemod.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.personality.ContextQuotient;
import com.example.examplemod.deities.personality.ContextQuotients;
import com.example.examplemod.utility.bus.ContextBus;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

@Mixin(PotionItem.class)
public class PotionItemMixin
{
	private Map<MobEffect, MobEffectInstance> effectMap = new HashMap<>();
	
	@Inject(method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"))
	public void finishUsingStart(ItemStack stack, Level world, LivingEntity mob, final CallbackInfoReturnable<ItemStack> ci)
	{
		if(mob.getType() != EntityType.PLAYER || world.isClientSide())
			return;
		
		effectMap.clear();
		for(MobEffect effect : ContextQuotients.getPotionQuotients().keySet())
		{
			MobEffectInstance status = mob.getEffect(effect);
			effectMap.put(effect, status != null && status.getDuration() <= 0 ? null : status);
		}
	}
	
	@Inject(method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"))
	public void finishUsingReturn(ItemStack stack, Level world, LivingEntity mob, final CallbackInfoReturnable<ItemStack> ci)
	{
		if(mob.getType() != EntityType.PLAYER || world.isClientSide())
			return;
		
		Map<MobEffect, RegistryObject<ContextQuotient>> potionQuotients = ContextQuotients.getPotionQuotients();
		PlayerData data = PlayerData.getCapability((Player)mob);
		for(MobEffect effect : potionQuotients.keySet())
			ContextBus.handleStatusEffect(mob.getEffect(effect), effectMap.get(effect), data);
	}
}
