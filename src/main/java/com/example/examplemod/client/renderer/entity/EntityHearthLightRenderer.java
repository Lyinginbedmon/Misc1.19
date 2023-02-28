package com.example.examplemod.client.renderer.entity;

import com.example.examplemod.client.ExModelLayers;
import com.example.examplemod.client.model.ModelHearthLightIndicator;
import com.example.examplemod.client.model.ModelHearthLightLantern;
import com.example.examplemod.entities.EntityHearthLight;
import com.example.examplemod.reference.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EntityHearthLightRenderer extends LivingEntityRenderer<EntityHearthLight, ModelHearthLightIndicator<EntityHearthLight>>
{
	private static final Minecraft mc = Minecraft.getInstance();
	protected static final ResourceLocation TEXTURE = new ResourceLocation(Reference.ModInfo.MOD_PREFIX+"textures/entity/hearth_light.png");
	
	private final LanternHandRenderer lanternRenderer;
	private final IndicatorHandRenderer indicatorRenderer;
	
	public EntityHearthLightRenderer(Context contextIn)
	{
		super(contextIn, new ModelHearthLightIndicator<EntityHearthLight>(contextIn.bakeLayer(ExModelLayers.HEARTH_INDICATOR)), 0F);
		
		this.lanternRenderer = new LanternHandRenderer(contextIn);
		this.indicatorRenderer = new IndicatorHandRenderer(contextIn);
	}
	
	public ResourceLocation getTextureLocation(EntityHearthLight p_114482_) { return TEXTURE; }
	
	protected boolean shouldShowName(EntityHearthLight entity) { return false; }
	
	public boolean shouldRender(EntityHearthLight entity, Frustum p_115469_, double p_115470_, double p_115471_, double p_115472_)
	{
		return super.shouldRender(entity, p_115469_, p_115470_, p_115471_, p_115472_) && (mc.player == null || entity.isVisibleFor(mc.player));
	}
	
	public void render(EntityHearthLight entity, float par2Float, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int bakedLight)
	{
		Vec3 direction = mc.getCameraEntity().position().subtract(entity.position());
		double yaw = Math.atan2(direction.x(), direction.z());
		
		// Main hand, always faces the player and carries the lantern
		matrixStack.pushPose();
			matrixStack.mulPose(Vector3f.YP.rotation((float)yaw));
			matrixStack.translate(-0.35D, 0.35D + getFloatAmount(HumanoidArm.RIGHT, entity.tickCount), 0D);
			// TODO Lantern hand should always face player
			this.lanternRenderer.render(entity, par2Float, partialTicks, matrixStack, bufferSource, bakedLight);
		matrixStack.popPose();
		
		// Off hand, gestures to the player
		matrixStack.pushPose();
			// TODO Indicator hand should orbit body according to current state
			switch(entity.currentIndication())
			{
				case POINTING:
					break;
				default:
					matrixStack.mulPose(Vector3f.YP.rotation((float)yaw));
					matrixStack.translate(0.35D, getFloatAmount(HumanoidArm.LEFT, (int)(entity.tickCount + entity.getUUID().getLeastSignificantBits())), 0D);
					break;
			}
			this.indicatorRenderer.render(entity, par2Float, partialTicks, matrixStack, bufferSource, bakedLight);
		matrixStack.popPose();
	}
	
	private double getFloatAmount(HumanoidArm hand, int tickCount)
	{
		switch(hand)
		{
			case LEFT:
				return Math.sin(tickCount * 0.02D) * 0.01D;
			case RIGHT:
			default:
				return Math.sin(tickCount * 0.02D) * -0.01D;
		}
	}
	
	private class LanternHandRenderer extends LivingEntityRenderer<EntityHearthLight, ModelHearthLightLantern<EntityHearthLight>>
	{
		private static final ModelResourceLocation LANTERN_MODEL = new ModelResourceLocation("soul_lantern", "hanging=false,waterlogged=false");
		private final BlockRenderDispatcher blockRenderer;
		
		public LanternHandRenderer(Context contextIn)
		{
			super(contextIn, new ModelHearthLightLantern<>(contextIn.bakeLayer(ExModelLayers.HEARTH_LANTERN)), 0F);
			this.blockRenderer = contextIn.getBlockRenderDispatcher();
		}
		
		public ResourceLocation getTextureLocation(EntityHearthLight p_114482_) { return TEXTURE; }
		protected boolean shouldShowName(EntityHearthLight entity) { return false; }
		
		@SuppressWarnings("deprecation")
		public void render(EntityHearthLight entity, float par2Float, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int bakedLight)
		{
			ModelManager blockModels = this.blockRenderer.getBlockModelShaper().getModelManager();
			matrixStack.pushPose();
				matrixStack.translate(-0.4D, -0.5D, -0.3D);
				this.blockRenderer.getModelRenderer().renderModel(matrixStack.last(), bufferSource.getBuffer(Sheets.solidBlockSheet()), (BlockState)null, blockModels.getModel(LANTERN_MODEL), 1.0F, 1.0F, 1.0F, bakedLight, OverlayTexture.NO_OVERLAY);
			matrixStack.popPose();
			super.render(entity, par2Float, partialTicks, matrixStack, bufferSource, bakedLight);
		}
	}
	
	private class IndicatorHandRenderer extends LivingEntityRenderer<EntityHearthLight, ModelHearthLightIndicator<EntityHearthLight>>
	{
		public IndicatorHandRenderer(Context contextIn) { super(contextIn, new ModelHearthLightIndicator<>(contextIn.bakeLayer(ExModelLayers.HEARTH_INDICATOR)), 0F); }
		
		public ResourceLocation getTextureLocation(EntityHearthLight p_114482_) { return TEXTURE; }
		protected boolean shouldShowName(EntityHearthLight entity) { return false; }
	}
}
