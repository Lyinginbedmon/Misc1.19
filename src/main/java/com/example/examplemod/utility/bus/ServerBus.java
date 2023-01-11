package com.example.examplemod.utility.bus;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBus
{
	@SubscribeEvent
	public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event)
	{
		if(event.getObject().getType() == EntityType.PLAYER)
			event.addCapability(PlayerData.IDENTIFIER, new PlayerData((Player)event.getObject()));
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerLoggedInEvent event)
	{
		PlayerData data = PlayerData.getCapability(event.getEntity());
		if(data != null)
			data.markDirty();
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.Clone event)
	{
		Player playerNew = event.getEntity();
		Player playerOld = event.getOriginal();
		playerOld.reviveCaps();
		
		PlayerData dataNew = PlayerData.getCapability(playerNew);
		PlayerData dataOld = PlayerData.getCapability(playerOld);
		if(dataNew != null && dataOld != null)
			dataNew.deserializeNBT(dataOld.serializeNBT());
		
		playerOld.invalidateCaps();
	}
	
	@SubscribeEvent
	public static void onPlayerTick(LivingTickEvent event)
	{
		if(event.getEntity().getType() == EntityType.PLAYER)
			PlayerData.getCapability((Player)event.getEntity()).tick();
	}
}
