package com.example.examplemod.utility.bus;

import com.example.examplemod.entity.ai.group.GroupPlayer;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBus
{
	@SubscribeEvent
	public static void onServerTick(TickEvent.LevelTickEvent event)
	{
		if(!event.level.isClientSide && event.level.dimension() == Level.OVERWORLD)
		{
			MinecraftServer server = event.level.getServer();
			
			GroupSaveData manager = GroupSaveData.get(server);
			manager.tick(server);
			if(manager.needsSync())
				manager.syncToClients(server);
		}
	}
	
	@SubscribeEvent
	public static void onWorldUnload(LevelEvent.Unload event)
	{
		GroupSaveData.clientStorageCopy.clear();
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerLoggedInEvent event)
	{
		GroupSaveData manager = GroupSaveData.get(event.getEntity().getServer());
		manager.syncToClient((ServerPlayer)event.getEntity());
		// Ensure all players have a personal group as standard
		if(!manager.hasGroup(event.getEntity()))
			manager.register(new GroupPlayer(event.getEntity()));
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.isCanceled()) return;
		GroupSaveData manager = GroupSaveData.get(event.getEntity().getServer());
		IMobGroup group = manager.getGroup(event.getEntity());
		if(group != null)
			group.remove(event.getEntity());
	}
}
