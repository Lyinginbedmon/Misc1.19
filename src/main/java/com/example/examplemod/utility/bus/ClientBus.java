package com.example.examplemod.utility.bus;

import com.example.examplemod.client.OverlayMobCommand;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.utility.GroupSaveData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientBus
{
	private static final Minecraft mc = Minecraft.getInstance();
	
	public static void registerOverlayEvent(RegisterGuiOverlaysEvent event)
	{
		event.registerAboveAll("mob_commands", new OverlayMobCommand());
	}
	
	@SubscribeEvent
	public static void renderGroupEvent(RenderLivingEvent event)
	{
		LivingEntity entity = event.getEntity();
		PoseStack matrixStack = event.getPoseStack();
		MultiBufferSource bufferSource = event.getMultiBufferSource();
		
		GroupSaveData manager = GroupSaveData.clientStorageCopy;
		IMobGroup group = manager.getGroup(entity);
		if(group == null)
			return;
		
		Vec3 cam = mc.getCameraEntity().getEyePosition(event.getPartialTick());
		Vec3 offset = new Vec3(0D, 1D, 0D);
		Vec3 posA = entity.position().add(offset).subtract(cam);
		Vec3 posB = group.position().add(offset).subtract(cam);
		
		float minX = (float)posA.x;
		float minY = (float)posA.y;
		float minZ = (float)posA.z;
		
		float maxX = (float)posB.x;
		float maxY = (float)posB.y;
		float maxZ = (float)posB.z;
		
		matrixStack.pushPose();
			VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
			Matrix4f matrix = matrixStack.last().pose();
			Matrix3f normal = matrixStack.last().normal();
			builder.vertex(matrix, maxX, maxY, maxZ).color(1F, 1F, 1F, 1F).normal(normal, 1, 0, 0).endVertex();
			builder.vertex(matrix, minX, minY, minZ).color(1F, 1F, 1F, 1F).normal(normal, 1, 0, 0).endVertex();
			builder.vertex(matrix, maxX, maxY, maxZ).color(1F, 1F, 1F, 1F).normal(normal, 0, 1, 0).endVertex();
			builder.vertex(matrix, minX, minY, minZ).color(1F, 1F, 1F, 1F).normal(normal, 0, 1, 0).endVertex();
			builder.vertex(matrix, maxX, maxY, maxZ).color(1F, 1F, 1F, 1F).normal(normal, 0, 0, 1).endVertex();
			builder.vertex(matrix, minX, minY, minZ).color(1F, 1F, 1F, 1F).normal(normal, 0, 0, 1).endVertex();
		matrixStack.popPose();
	}
}
