package com.example.examplemod.utility.bus;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.OverlayGodStatus;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

@OnlyIn(Dist.CLIENT)
public class ClientBus
{
	public static Minecraft mc = Minecraft.getInstance();
	
	public static void registerOverlayEvent(RegisterGuiOverlaysEvent event)
	{
		ExampleMod.LOG.info("Registering overlays");
		event.registerAboveAll("god_status", new OverlayGodStatus());
	}
}
