package com.lying.misc19.client.renderer.entity;

import com.lying.misc19.init.M19Items;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PendulumLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel & HeadedModel> extends ItemInHandLayer<T, M>
{
	private static final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
	private static final ItemStack WEIGHT = new ItemStack(M19Items.PENDULUM_WEIGHT.get());
	
	private static final double LENGTH = 1.2D;
	private static final Vec3 handle = new Vec3(0D, -0.05D, 0.25D);
	
	public PendulumLayer(RenderLayerParent<T, M> renderer)
	{
		super(renderer, null);
	}
	
	protected void renderArmWithItem(LivingEntity player, ItemStack stack, ItemTransforms.TransformType transform, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int p_174531_)
	{
		if(stack.getItem() != M19Items.PENDULUM.get() || stack.isEmpty() || player.getUseItem() != stack)
			return;
		
		poseStack.pushPose();
			getParentModel().translateToHand(arm, poseStack);
			poseStack.mulPose(Vector3f.XP.rotationDegrees(-90F));
			poseStack.mulPose(Vector3f.YP.rotationDegrees(180F));
			boolean isLeft = arm == HumanoidArm.LEFT;
			poseStack.translate((double)((float)(isLeft ? -1 : 1) / 16F) - handle.x, 0.125D - handle.y, -0.625D - handle.z);
			poseStack.pushPose();
				poseStack.mulPose(Vector3f.ZP.rotationDegrees((float)player.getTicksUsingItem() * 32F));
				
				renderLine(poseStack, bufferSource);
				
				poseStack.translate(0D, -LENGTH, 0D);
				
				// XXX Coloured trail behind weight?
				
				poseStack.scale(0.3F, 0.3F, 0.3F);
				BakedModel bakedModel = itemRenderer.getModel(WEIGHT, player.getLevel(), player, player.getId());
				itemRenderer.render(WEIGHT, ItemTransforms.TransformType.FIXED, false, poseStack, bufferSource, p_174531_, OverlayTexture.NO_OVERLAY, bakedModel);
			poseStack.popPose();
		poseStack.popPose();
	}
	
	private static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource)
	{
		VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
		Matrix4f matrix = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		
		float col = 48F / 255F;
		
		float len = -(float)(LENGTH - 0.05D);
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, 1, 0, 0).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, 1, 0, 0).endVertex();
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, -1, 0, 0).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, -1, 0, 0).endVertex();
		
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, 0, 1, 0).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, 0, 1, 0).endVertex();
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, 0, -1, 0).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, 0, -1, 0).endVertex();
		
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, 0, 0, 1).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, 0, 0, 1).endVertex();
		builder.vertex(matrix, 0F, 0F, 0F).color(col, col, col, 1F).normal(normal, 0, 0, -1).endVertex();
		builder.vertex(matrix, 0F, len, 0F).color(col, col, col, 1F).normal(normal, 0, 0, -1).endVertex();
	}
}
