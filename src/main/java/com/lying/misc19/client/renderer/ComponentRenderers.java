package com.lying.misc19.client.renderer;

import java.util.HashMap;
import java.util.Map;

import com.lying.misc19.client.renderer.magic.ComponentRenderer;
import com.lying.misc19.client.renderer.magic.RootRenderer;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

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
	}
	
	public static void register(ResourceLocation name, ComponentRenderer renderer)
	{
		REGISTRY.put(name, renderer);
	}
	
	public static void renderComponent(ISpellComponent component, PoseStack matrixStack)
	{
		REGISTRY.getOrDefault(component.getRegistryName(), new ComponentRenderer()).render(component, matrixStack);
	}
}
