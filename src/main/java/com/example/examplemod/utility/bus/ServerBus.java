package com.example.examplemod.utility.bus;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.api.event.MiracleEvent;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.miracle.Miracle;
import com.example.examplemod.reference.Reference;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
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
	public static void onLivingTick(final LivingEvent.LivingTickEvent event)
	{
		if(event.getEntity().getType() == EntityType.PLAYER)
		{
			Player player = (Player)event.getEntity();
			PlayerData data = PlayerData.getCapability(player);
			data.tick();
			
			Deity god = data.getDeity();
			if(god == null)
				return;
			
			List<Miracle> miraclesAvailable = god.miracles();
			Miracle chosenMiracle = null;
			Map<Miracle, Float> utilityCache = new HashMap<>();
			if(miraclesAvailable.isEmpty())
				return;
			else if(miraclesAvailable.size() > 1)
			{
				Comparator<Miracle> sort = new Comparator<Miracle>()
					{
						public int compare(Miracle o1, Miracle o2)
						{
							float util1 = utilityCache.getOrDefault(o1, Mth.clamp(o1.getUtility(player, player.getLevel()), 0F, 1F));
							float util2 = utilityCache.getOrDefault(02, Mth.clamp(o2.getUtility(player, player.getLevel()), 0F, 1F));
							
							utilityCache.put(o1, util1);
							utilityCache.put(o2, util2);
							return util1 > util2 ? -1 : util1 < util2 ? 1 : 0;
						}
					};
				miraclesAvailable.sort(sort);
				
				float highestUtility = utilityCache.get(miraclesAvailable.get(0));
				List<Miracle> bestMiracles = Lists.newArrayList();
				for(Miracle miracle : miraclesAvailable)
					if(utilityCache.get(miracle) >= highestUtility)
						bestMiracles.add(miracle);
					else
						break;
				
				chosenMiracle = bestMiracles.get(player.getRandom().nextInt(bestMiracles.size()));
			}
			else
				chosenMiracle = miraclesAvailable.get(0);
			
			float utility = utilityCache.getOrDefault(chosenMiracle, Mth.clamp(chosenMiracle.getUtility(player, player.getLevel()), 0F, 1F));
			if(chosenMiracle != null && utility > 0F)
			{
				MiracleEvent miracleEvent = new MiracleEvent(player, god, chosenMiracle, utility);
				if(!MinecraftForge.EVENT_BUS.post(miracleEvent))
					chosenMiracle.perform(player, player.getLevel());
			}
		}
	}
}
