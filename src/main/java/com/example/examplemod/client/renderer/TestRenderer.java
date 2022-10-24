package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.entity.ai.tree.TreeNode;
import com.example.examplemod.entity.ai.tree.TreeNode.NodeMap;
import com.example.examplemod.reference.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestRenderer extends MobRenderer<TestEntity, HumanoidModel<TestEntity>>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TestRenderer(EntityRendererProvider.Context context)
	{
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR))));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}
	
	public ResourceLocation getTextureLocation(TestEntity p_114482_)
	{
		return new ResourceLocation(Reference.ModInfo.MOD_ID, "textures/entity/test.png");
	}
	
	@SuppressWarnings("resource")
	public void render(TestEntity entity, float p_114830_, float p_114831_, PoseStack poseStack, MultiBufferSource renderBuffer, int packedLight)
	{
		super.render(entity, p_114830_, p_114831_, poseStack, renderBuffer, packedLight);
		
		if(Minecraft.getInstance().player.isCreative())// && entity == this.entityRenderDispatcher.crosshairPickEntity)
		{
			boolean showFull = Minecraft.getInstance().player.isDiscrete();
			NodeMap map = entity.getTree().mapTree();
			poseStack.pushPose();
				poseStack.translate(0D, entity.getBbHeight() + 0.5D, 0D);
				poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
				float scale = 1F / 80F;
				poseStack.scale(-scale, -scale, scale);
				poseStack.translate(getMapWidth(map, getFont(), showFull) * -0.5D, -getMapHeight(map, getFont(), showFull), 0D);
				renderNodeMap(map, poseStack, poseStack.last().pose(), renderBuffer, packedLight, getFont(), 0F);
			poseStack.popPose();
		}
	}
	
	private double getMapHeight(NodeMap nodeMap, Font font, boolean showFull)
	{
		double height = 0D;
		
		if(nodeMap.parent.shouldLogChildren())
		{
			int totalChildren = nodeMap.getChildren().size();
			for(int i=0; i<totalChildren; i++)
			{
				NodeMap child = nodeMap.getChild(i);
				if(!showFull && !child.parent.wasActive())
					continue;
				
				height += getMapHeight(child, font, showFull);
				if(i < totalChildren - 1)
					height += (double)font.lineHeight + 1D;
			}
		}
		return height;
	}
	
	private int getMapWidth(NodeMap nodeMap, Font font, boolean showFull)
	{
		if(!showFull && !nodeMap.parent.wasActive())
			return 0;
		
		int width = font.width("> "+nodeMap.parent.getDisplayName());
		int max = 0;
		if(nodeMap.parent.shouldLogChildren())
			for(NodeMap child : nodeMap.getChildren())
				max = Math.max(max, getMapWidth(child, font, showFull));
		
		return width + max;
	}
	
	@SuppressWarnings("resource")
	private void renderNodeMap(NodeMap nodeMap, PoseStack poseStack, Matrix4f matrix4f, MultiBufferSource renderBuffer, int packedLight, Font font, float xOffset)
	{
		boolean showFull = Minecraft.getInstance().player.isDiscrete();
        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int)(f1 * 255.0F) << 24;
        
		TreeNode node = nodeMap.parent;
		String displayText = "> " + node.getDisplayName();
		if(node.wasActive() || showFull)
			font.drawInBatch(Component.literal(displayText), xOffset, 0, node.wasActive() ? node.previousResult().hudColor() : ChatFormatting.GRAY.getColor(), false, matrix4f, renderBuffer, false, j, packedLight);
		if(!node.shouldLogChildren())
			return;
		
		float childOffset = font.width(displayText) + 2F;
		int totalChildren = nodeMap.getChildren().size();
		for(int i=0; i<totalChildren; i++)
		{
			NodeMap child = nodeMap.getChild(i);
			if(!showFull && !child.parent.wasActive())
				continue;
			
			renderNodeMap(nodeMap.getChild(i), poseStack, matrix4f, renderBuffer, packedLight, font, xOffset + childOffset);
			if(i < totalChildren - 1)
				poseStack.translate(0D, (double)font.lineHeight + 1D, 0D);
		}
	}
}
