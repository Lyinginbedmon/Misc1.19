package com.example.examplemod.client;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.client.ActionRenderManager.ActionRenderer;
import com.example.examplemod.entity.ai.group.GroupAction;
import com.example.examplemod.entity.ai.group.GroupPlayer;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.utility.GroupSaveData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;

@OnlyIn(Dist.CLIENT)
public class GroupRenderer
{
	private static final Minecraft mc = Minecraft.getInstance();
	private static RandomSource random = RandomSource.create();
	
	public static void renderGroups(RenderLevelStageEvent event)
	{
		if(event.getStage() != Stage.AFTER_CUTOUT_BLOCKS || !mc.options.renderDebug) return;
		GroupSaveData manager = GroupSaveData.clientStorageCopy;
		Player player = mc.player;
		
		List<IMobGroup> groupsToRender = Lists.newArrayList();
		player.getLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32D)).forEach((entity) -> 
		{
			IMobGroup group = manager.getGroup(entity);
			if(group != null && !groupsToRender.contains(group))
				groupsToRender.add(group);
		});
		
		if(groupsToRender.isEmpty())
			return;
		
		random.setSeed(114880526);
		for(IMobGroup group : groupsToRender)
			renderGroup(group, event.getPoseStack(), event.getCamera(), group instanceof GroupPlayer && ((GroupPlayer)group).isOwner(player) ? -1 : random.nextInt());
	}
	
	private static void renderGroup(IMobGroup group, PoseStack matrixStack, Camera projectedView, int colour)
	{
		Vec3 offset = projectedView.getPosition();
		
		// Central position of the group
		Vec3 groupPos = group.position().subtract(offset);
		
		// Colour values
		int r = colour >> 16;
		int g = colour >> 8;
		int b = colour & 255 >> 0;
		
		// Identify members by drawing lines from the group position to them
		for(LivingEntity member : group.members())
		{
			Vec3 memberPos = member.position().subtract(offset);
			matrixStack.pushPose();
				VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
				Matrix4f matrix = matrixStack.last().pose();
				Matrix3f normal = matrixStack.last().normal();
				builder.vertex(matrix, (float)groupPos.x, (float)groupPos.y, (float)groupPos.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)memberPos.x, (float)memberPos.y, (float)memberPos.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
			matrixStack.popPose();
		}
		
		if(group.hasAction())
		{
			GroupAction action = group.getAction();
			
			ResourceLocation registryName = action.getRegistryName();
			matrixStack.pushPose();
				matrixStack.translate(groupPos.x, groupPos.y, groupPos.z);
				matrixStack.translate(0D, 0.5D, 0D);
				matrixStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
				matrixStack.scale(-0.025F, -0.025F, 0.025F);
				String nameString = registryName.toString();
				float width = mc.font.width(nameString) * -0.5F;
				int opacity = (int)(mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
				BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
				mc.font.drawInBatch(nameString, width, 0F, -1, false, matrixStack.last().pose(), buffer, false, opacity, 15728880);
				buffer.endBatch();
			matrixStack.popPose();
			
			ActionRenderer renderer = ActionRenderManager.getRenderer(registryName);
			if(renderer != null)
				renderer.render(action, matrixStack, projectedView);
		}
	}
}
