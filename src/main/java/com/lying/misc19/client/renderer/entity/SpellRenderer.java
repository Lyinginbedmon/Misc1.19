package com.lying.misc19.client.renderer.entity;

import com.lying.misc19.entities.SpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class SpellRenderer extends EntityRenderer<SpellEntity>
{
	public SpellRenderer(Context contextIn)
	{
		super(contextIn);
	}
	
	public ResourceLocation getTextureLocation(SpellEntity entity)
	{
		return null;
	}
	
	public void render(SpellEntity spellEntity, float p_115037_, float p_115038_, PoseStack p_115039_, MultiBufferSource p_115040_, int p_115041_)
	{
		// TODO Render contained spell
	}
}
