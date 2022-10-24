package com.example.examplemod.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.group.ActionType;
import com.example.examplemod.entity.ai.group.GroupAction;
import com.example.examplemod.entity.ai.group.GroupAction.ActionGuardMob;
import com.example.examplemod.entity.ai.group.GroupAction.ActionGuardPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class ActionRenderManager
{
	private static final Minecraft mc = Minecraft.getInstance();
	private static final Map<ResourceLocation, ActionRenderer> ACTION_RENDERERS = new HashMap<>();
	
	public static void init()
	{
		register(ActionType.GUARD_POS, new ActionRenderer()
		{
			public void render(GroupAction action, PoseStack matrixStack, Camera projectedView)
			{
				ActionGuardPos guardPos = (ActionGuardPos)action;
				Vec3 camera = projectedView.getPosition();
				
				BlockPos target = guardPos.targetPoint();
				drawSquareAt(new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D).subtract(camera), 1D, matrixStack, 0F, 1F, 0F);
				
				guardPos.formationPoints().forEach((pos) -> drawSquareAt(new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D).subtract(camera), 0.6D, matrixStack, 1F, 1F, 1F));
			}
		});
		register(ActionType.GUARD_MOB, new ActionRenderer()
		{
			public void render(GroupAction action, PoseStack matrixStack, Camera projectedView)
			{
				ActionGuardMob guardPos = (ActionGuardMob)action;
				Vec3 camera = projectedView.getPosition();
				
				BlockPos lastPos = guardPos.lastPosition();
				drawSquareAt(new Vec3(lastPos.getX() + 0.5D, lastPos.getY(), lastPos.getZ() + 0.5D).subtract(camera), 1D, matrixStack, 0F, 1F, 0F);
				guardPos.formationPoints().forEach((pos) -> drawSquareAt(new Vec3(lastPos.getX() + pos.getX() + 0.5D, lastPos.getY(), lastPos.getZ() + pos.getZ() + 0.5D).subtract(camera), 0.6D, matrixStack, 1F, 1F, 1F));
			}
		});
	}
	
	private static void register(ResourceLocation actionType, ActionRenderer renderer)
	{
		ACTION_RENDERERS.put(actionType, renderer);
	}
	
	@Nullable
	public static ActionRenderer getRenderer(ResourceLocation actionType)
	{
		return ACTION_RENDERERS.containsKey(actionType) ? ACTION_RENDERERS.get(actionType) : null;
	}
	
	public static abstract class ActionRenderer
	{
		public abstract void render(GroupAction action, PoseStack matrixStack, Camera projectedView);
		
		protected void drawSquareAt(BlockPos point, double sideLength, PoseStack matrixStack, float r, float g, float b)
		{
			drawSquareAt(new Vec3(point.getX() + 0.5D, point.getY(), point.getZ() + 0.5D), sideLength, matrixStack, r, g, b);
		}
		
		protected void drawSquareAt(Vec3 point, double sideLength, PoseStack matrixStack, float r, float g, float b)
		{
			matrixStack.pushPose();
				sideLength /= 2D;
				Vec3 p1 = point.add(sideLength, 0D, sideLength);
				Vec3 p2 = point.add(sideLength, 0D, -sideLength);
				Vec3 p3 = point.add(-sideLength, 0D, -sideLength);
				Vec3 p4 = point.add(-sideLength, 0D, sideLength);
				
				VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lineStrip());
				Matrix4f matrix = matrixStack.last().pose();
				Matrix3f normal = matrixStack.last().normal();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p2.x, (float)p1.y, (float)p2.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p3.x, (float)p1.y, (float)p3.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p4.x, (float)p1.y, (float)p4.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
			matrixStack.popPose();
		}
	}
}
