package com.example.examplemod.deities.miracle;

import com.example.examplemod.reference.Reference;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MiracleSafeLanding extends Miracle
{
	public float getUtility(Player playerIn, Level worldIn)
	{
		if(playerIn.getAbilities().invulnerable || playerIn.getEffect(MobEffects.SLOW_FALLING) != null)
			return 0F;
		
		float fallDamage = Math.max(0F, playerIn.fallDistance - 3F);
		float health = playerIn.getHealth();
		return (float)Math.pow(fallDamage / health, 7);
	}
	
	public void perform(Player playerIn, Level worldIn)
	{
		playerIn.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Reference.Values.TICKS_PER_SECOND * 5, 0, false, false));
	}
}
