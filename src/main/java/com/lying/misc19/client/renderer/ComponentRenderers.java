package com.lying.misc19.client.renderer;

import java.util.HashMap;
import java.util.Map;

import com.lying.misc19.client.Canvas;
import com.lying.misc19.client.renderer.magic.CircleRenderer;
import com.lying.misc19.client.renderer.magic.ComponentRenderer;
import com.lying.misc19.client.renderer.magic.RootRenderer;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public class ComponentRenderers
{
	private static Map<ResourceLocation, ComponentRenderer> REGISTRY = new HashMap<>();
	
	static
	{
		register(SpellComponents.ROOT_CASTER, new RootRenderer());
		register(SpellComponents.ROOT_DUMMY, new RootRenderer());
		register(SpellComponents.ROOT_POSITION, new RootRenderer());
		register(SpellComponents.ROOT_TARGET, new RootRenderer());
		register(SpellComponents.CIRCLE_BASIC, new CircleRenderer());
		register(SpellComponents.CIRCLE_STEP, new CircleRenderer());
	}
	
	public static void register(ResourceLocation name, ComponentRenderer renderer)
	{
		REGISTRY.put(name, renderer);
	}
	
	public static ComponentRenderer get(ResourceLocation name) { return REGISTRY.getOrDefault(name, new ComponentRenderer()); }
	
	public static void renderGUI(ISpellComponent component, PoseStack matrixStack)
	{
		populateCanvas(component, matrixStack).drawIntoGUI(matrixStack);
//		renderer.drawGUIGlyph(component, matrixStack);
	}
	
	public static void renderGUIGlyph(ISpellComponent component, PoseStack matrixStack)
	{
		ResourceLocation registryName = component.getRegistryName();
		REGISTRY.getOrDefault(registryName, new ComponentRenderer()).drawGUIGlyph(component, matrixStack);
	}
	
	public static void renderWorld(ISpellComponent component, PoseStack matrixStack, MultiBufferSource bufferSource)
	{
		populateCanvas(component, matrixStack).drawIntoWorld(matrixStack, bufferSource);
	}
	
	private static Canvas populateCanvas(ISpellComponent component, PoseStack matrixStack)
	{
		Canvas canvas = new Canvas();
		
		ResourceLocation registryName = component.getRegistryName();
		ComponentRenderer renderer = REGISTRY.getOrDefault(registryName, new ComponentRenderer());
		
		renderer.addToCanvasRecursive(component, canvas);
		return canvas;
	}
}
