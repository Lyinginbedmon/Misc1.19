package com.lying.misc19.utility.bus;

import com.lying.misc19.capabilities.LivingData;
import com.lying.misc19.reference.Reference;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBus
{
	@SubscribeEvent
	public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event)
	{
		if(event.getObject() instanceof LivingEntity)
			event.addCapability(LivingData.IDENTIFIER, new LivingData((LivingEntity)event.getObject()));
	}
	
	@SubscribeEvent
	public static void onLivingTick(LivingTickEvent event)
	{
		if(event.getEntity().isAlive())
		{
			LivingData data = LivingData.getCapability(event.getEntity());
			if(data == null)
				return;
			
			data.tick();
		}
	}
}
