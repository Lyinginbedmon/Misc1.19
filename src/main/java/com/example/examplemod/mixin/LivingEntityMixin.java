package com.example.examplemod.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.personality.ContextQuotient;
import com.example.examplemod.deities.personality.ContextQuotients;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
	private static final Map<MobEffect, RegistryObject<ContextQuotient>> POTION_TO_QUOTIENT = new HashMap<>();
	private Map<MobEffect, MobEffectInstance> effectMap = new HashMap<>();
	
	@Inject(method = "addEatEffect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"))
	public void eatEffectStart(ItemStack stack, Level world, LivingEntity mob, final CallbackInfo ci)
	{
		if(mob.getType() != EntityType.PLAYER || world.isClientSide())
			return;
		
		effectMap.clear();
		for(MobEffect effect : POTION_TO_QUOTIENT.keySet())
		{
			MobEffectInstance status = mob.getEffect(effect);
			effectMap.put(effect, status != null && status.getDuration() <= 0 ? null : status);
		}
	}
	
	@Inject(method = "addEatEffect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"))
	public void eatEffectReturn(ItemStack stack, Level world, LivingEntity mob, final CallbackInfo ci)
	{
		if(mob.getType() != EntityType.PLAYER || world.isClientSide())
			return;
		
		PlayerData data = PlayerData.getCapability((Player)mob);
		for(MobEffect effect : POTION_TO_QUOTIENT.keySet())
		{
			MobEffectInstance instance = mob.getEffect(effect);
			if(instance == null || instance.getDuration() <= 0)
				continue;
			
			// Ignore any status effects that we already had and which have not been improved
			if(effectMap.get(effect) != null && instance.getAmplifier() <= effectMap.get(effect).getAmplifier())
				continue;
			
			double amount = (instance.getDuration() / Reference.Values.TICKS_PER_SECOND) * Math.pow(1 + instance.getAmplifier(), 3);
			data.addQuotient(POTION_TO_QUOTIENT.get(effect).getId(), amount);
		}
	}
	
	static
	{
		POTION_TO_QUOTIENT.put(MobEffects.DAMAGE_BOOST, ContextQuotients.STATUS_STRENGTH);
		POTION_TO_QUOTIENT.put(MobEffects.DAMAGE_RESISTANCE, ContextQuotients.STATUS_RESISTANCE);
		POTION_TO_QUOTIENT.put(MobEffects.POISON, ContextQuotients.STATUS_POISON);
		POTION_TO_QUOTIENT.put(MobEffects.WITHER, ContextQuotients.STATUS_POISON);
	}
}
