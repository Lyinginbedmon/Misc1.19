package com.example.examplemod.deities.miracle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleIndomitable extends Miracle
{
	private float nextDamage = 0F;
	
	public MiracleIndomitable() { super(Power.MINOR); }
	
	public float getUtility(Player playerIn, Level worldIn)
	{
		return isPlayerImmortal(playerIn) ? 0F : (float)Math.pow(Math.sin((nextDamage / playerIn.getHealth()) * 2.2F), 3);
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
			MiracleIndomitable miracle = (MiracleIndomitable)Miracles.INDOMITABLE.get();
			miracle.nextDamage = event.getAmount();
			
			if(!checkMiracle(player, miracle))
				return;
			
			// Perform miracle
			event.setCanceled(true);
			reportMiracle(player, miracle);
		}
	}
}
