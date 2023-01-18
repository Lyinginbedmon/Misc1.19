package com.example.examplemod.deities.miracle;

import com.example.examplemod.reference.Reference;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleJuggernaut extends Miracle
{
	private float nextDamage = 0F;
	
	public MiracleJuggernaut() { super(Power.MAJOR); }
	
	public float getUtility(Player playerIn, Level worldIn)
	{
		return isPlayerImmortal(playerIn) ? 0F : (float)Math.pow(nextDamage / playerIn.getHealth(), 10);
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPlayerHurt);
	}
	
	public void onPlayerHurt(LivingDamageEvent event)
	{
		if(!event.isCanceled() && event.getSource().getEntity() != null && event.getEntity().getType() == EntityType.PLAYER)
		{
			Player player = (Player)event.getEntity();
			MiracleJuggernaut miracle = (MiracleJuggernaut)Miracles.JUGGERNAUT.get();
			miracle.nextDamage = event.getAmount();
			
			if(!checkMiracle(player, miracle))
				return;
			
			// Perform miracle
			event.setCanceled(true);
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Reference.Values.TICKS_PER_SECOND * 15, 5, true, false));
			
			reportMiracle(player, miracle);
		}
	}
}
