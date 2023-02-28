package com.example.examplemod.client.renderer;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.ExModelLayers;
import com.example.examplemod.client.model.*;
import com.example.examplemod.client.renderer.entity.EntityHearthLightRenderer;
import com.example.examplemod.init.ExEntities;
import com.example.examplemod.reference.Reference;

import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRenderRegistry
{
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
//		if(ConfigVO.GENERAL.verboseLogs())
			ExampleMod.LOG.info("Registering renderers");
		
		event.registerEntityRenderer(ExEntities.HEARTH_LIGHT.get(), EntityHearthLightRenderer::new);
		event.registerEntityRenderer(ExEntities.GUARD_ZOMBIE.get(), ZombieRenderer::new);
		event.registerEntityRenderer(ExEntities.GUARD_SKELETON.get(), SkeletonRenderer::new);
	}
	
	@SubscribeEvent
	public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event)
	{
//		if(ConfigVO.GENERAL.verboseLogs())
			ExampleMod.LOG.info("Registering model layers");
		
		event.registerLayerDefinition(ExModelLayers.HEARTH_LANTERN, () -> ModelHearthLightLantern.createBodyLayer());
		event.registerLayerDefinition(ExModelLayers.HEARTH_INDICATOR, () -> ModelHearthLightIndicator.createBodyLayer());
	}
}
