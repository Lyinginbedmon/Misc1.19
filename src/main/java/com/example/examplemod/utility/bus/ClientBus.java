package com.example.examplemod.utility.bus;

import com.example.examplemod.client.OverlayMobCommand;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

@OnlyIn(Dist.CLIENT)
public class ClientBus
{
	public static void registerOverlayEvent(RegisterGuiOverlaysEvent event)
	{
		event.registerAboveAll("mob_commands", new OverlayMobCommand());
	}
}
