package com.example.examplemod.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.network.NetworkEvent;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy
{
	private static final Minecraft mc = Minecraft.getInstance();
	
	public static void registerKeyMappings(RegisterKeyMappingsEvent event)
	{
		
	}
	
	public void clientInit()
	{
		
	}
	
	public void registerHandlers()
	{
		
	}
	
	public void onLoadComplete(FMLLoadCompleteEvent event)
	{
		
	}
	
	public Player getPlayerEntity(NetworkEvent.Context ctx){ return (ctx.getDirection().getReceptionSide().isClient() ? mc.player : super.getPlayerEntity(ctx)); }
}
