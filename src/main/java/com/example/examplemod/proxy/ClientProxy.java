package com.example.examplemod.proxy;

import com.example.examplemod.client.KeyBindings;
import com.example.examplemod.client.renderer.TestRenderer;
import com.example.examplemod.init.ExEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
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
		KeyBindings.register(event::register);
	}
	
	public void clientInit()
	{
		EntityRenderers.register(ExEntities.TEST.get(), TestRenderer::new);
	}
	
	public void registerHandlers()
	{
		
	}
	
	public void onLoadComplete(FMLLoadCompleteEvent event)
	{
		
	}
	
	public Player getPlayerEntity(NetworkEvent.Context ctx){ return (ctx.getDirection().getReceptionSide().isClient() ? mc.player : super.getPlayerEntity(ctx)); }
}
