package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.reference.Reference;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class TestRenderer extends MobRenderer<TestEntity, HumanoidModel<TestEntity>>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TestRenderer(EntityRendererProvider.Context context)
	{
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE))));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(TestEntity p_114482_)
	{
		return new ResourceLocation(Reference.ModInfo.MOD_ID, "entities/test.png");
	}
}
