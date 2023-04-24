package com.lying.misc19.client.renderer;

import com.lying.misc19.client.renderer.entity.*;
import com.lying.misc19.init.M19Entities;
import com.lying.misc19.reference.Reference;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RenderRegistry
{
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(M19Entities.SPELL.get(), SpellRenderer::new);
	}
}
