package com.example.examplemod.utility.bus;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.DeityRegistry;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientBus
{
	public static Minecraft mc = Minecraft.getInstance();
	public static Deity currentGod = null;
	
	public static void registerOverlayEvent(RegisterGuiOverlaysEvent event)
	{
//		event.registerAboveAll("mob_commands", new OverlayMobCommand());
	}
	
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event)
	{
		if(currentGod == null)
			currentGod = DeityRegistry.getInstance().getDeity("erinus");
		if(currentGod == null || mc.player == null)
			return;
		
		ExampleMod.LOG.info("Current deity: "+currentGod.displayName().getString());
		ExampleMod.LOG.info("# Opinion: "+currentGod.opinionOf(mc.player));
	}
}
