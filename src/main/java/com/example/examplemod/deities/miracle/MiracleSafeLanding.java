package com.example.examplemod.deities.miracle;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleSafeLanding extends Miracle
{
	public MiracleSafeLanding() { super(Power.MINOR); }
	
	public float getUtility(Player playerIn, Level worldIn)
	{
		if(isPlayerImmortal(playerIn) || playerIn.getEffect(MobEffects.SLOW_FALLING) != null)
			return 0F;
		
		return (float)Math.pow(Math.max(0F, playerIn.fallDistance - 3F) / playerIn.getHealth(), 7);
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPlayerFall);
	}
	
	public void onPlayerFall(LivingDamageEvent event)
	{
		if(!event.isCanceled() && event.getSource() == DamageSource.FALL && event.getEntity().getType() == EntityType.PLAYER)
		{
			Player player = (Player)event.getEntity();
			if(!checkMiracle(player, Miracles.SAFE_LANDING.get()))
				return;
			
			// Perform miracle
			event.setAmount(Math.min(event.getAmount(), player.getHealth() - 1));
			reportMiracle(player, Miracles.SAFE_LANDING.get());
		}
	}
}
