package com.example.examplemod.deities.miracle;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleStrongBrew extends Miracle
{
	public MiracleStrongBrew()
	{
		super(Power.MAJOR);
	}
	
	@Override
	public float getUtility(Player playerIn, Level worldIn)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPotionExpire);
	}
	
	public void onPotionExpire(MobEffectEvent.Expired event)
	{
		MobEffectInstance old = event.getEffectInstance();
		if(event.getEntity().getType() == EntityType.PLAYER && !old.isAmbient() && !old.getEffect().isInstantenous() && old.getEffect().isBeneficial())
		{
			Player player = (Player)event.getEntity();
			if(!checkMiracle(player, Miracles.STRONG_BREW.get()))
				return;
			
			MobEffectInstance effect = new MobEffectInstance(old.getEffect(), Reference.Values.TICKS_PER_SECOND * 15, old.getAmplifier(), true, old.isVisible());
			PlayerData.getCapability(player).queueEvent((plyr) -> plyr.addEffect(effect));
			reportMiracle(player, Miracles.STRONG_BREW.get());
		}
	}
}
