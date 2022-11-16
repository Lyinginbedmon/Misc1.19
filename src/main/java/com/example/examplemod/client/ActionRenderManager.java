package com.example.examplemod.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.group.action.ActionType;
import com.example.examplemod.entity.ai.group.action.GroupAction;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionFarm;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionFarm.CropState;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionFlank;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionFollow;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionGuardMob;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionGuardPos;
import com.example.examplemod.entity.ai.group.action.GroupAction.ActionQuarry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ActionRenderManager
{
	private static final Minecraft mc = Minecraft.getInstance();
	private static final Map<ResourceLocation, ActionRenderer<?>> ACTION_RENDERERS = new HashMap<>();
	
	public static void init()
	{
		register(ActionType.GUARD_POS, new ActionRenderer<ActionGuardPos>()
		{
			public void render(ActionGuardPos action, PoseStack matrixStack, Camera projectedView)
			{
				outlineCubeAt(action.targetPoint(), projectedView, matrixStack, 0F, 1F, 0F);
				action.formationPoints().forEach((pos) -> drawSquareAt(pos, 0.6D, projectedView, matrixStack, 1F, 1F, 1F));
			}
		});
		register(ActionType.FLANK, new ActionRenderer<ActionFlank>()
		{
			public void render(ActionFlank action, PoseStack matrixStack, Camera projectedView)
			{
				drawTriangleAt(action.getTargetPoint(), 1D, 45F, projectedView, matrixStack, 1F, 0F, 0F);
				action.formationPoints().forEach((pos) -> drawSquareAt(pos, 0.6D, projectedView, matrixStack, 1F, 1F, 1F));
			}
		});
		register(ActionType.GUARD_MOB, new ActionRenderer<ActionGuardMob>()
		{
			public void render(ActionGuardMob action, PoseStack matrixStack, Camera projectedView)
			{
				Vec3 lastPos = action.lastPosition();
				drawSquareAt(lastPos.subtract(projectedView.getPosition()), 1D, matrixStack, 0F, 1F, 0F);
				action.formationPoints().forEach((pos) -> drawSquareAt(lastPos.add(pos.getX(), 0, pos.getZ()).subtract(projectedView.getPosition()), 0.6D, matrixStack, 1F, 1F, 1F));
			}
		});
		register(ActionType.QUARRY, new ActionRenderer<ActionQuarry>()
		{
			public void render(ActionQuarry action, PoseStack matrixStack, Camera projectedView)
			{
				outlineAABB(action.getBounds(), projectedView, matrixStack, 1F, 1F, 1F);
				action.currentLot().forEach((pos) -> outlineCubeAt(pos, projectedView, matrixStack, 1F, 0F, 0F));
			}
		});
		register(ActionType.FOLLOW, new ActionRenderer<ActionFollow>()
		{
			public void render(ActionFollow action, PoseStack matrixStack, Camera projectedView)
			{
				Vec3 camera = projectedView.getPosition();
				drawSquareAt(action.followPos().subtract(camera), 1D, matrixStack, 0F, 1F, 0F);
			}
		});
		register(ActionType.FARM, new ActionRenderer<ActionFarm>()
		{
			public void render(ActionFarm action, PoseStack matrixStack, Camera projectedView)
			{
				outlineAABB(action.getBounds(), projectedView, matrixStack, 1F, 1F, 1F);
				for(CropState state : CropState.values())
					action.getBlocksOfState(state).forEach((pos) ->
					{
						if(state == CropState.PLANT || state == CropState.BONEMEAL)
							drawSquareAt(pos, 1D, projectedView, matrixStack, state.red(), state.green(), state.blue());
						else
							outlineCubeAt(pos, projectedView, matrixStack, state.red(), state.green(), state.blue());
					});
			}
		});
	}
	
	private static void register(ResourceLocation actionType, ActionRenderer<?> renderer)
	{
		ACTION_RENDERERS.put(actionType, renderer);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends GroupAction> ActionRenderer<? super T> getRenderer(ResourceLocation actionType)
	{
		return ACTION_RENDERERS.containsKey(actionType) ? (ActionRenderer<? super T>)ACTION_RENDERERS.get(actionType) : null;
	}
	
	public static void tryRenderAction(GroupAction action, PoseStack matrixStack, Camera projectedView)
	{
		ActionRenderer<? super GroupAction> renderer = getRenderer(action.getRegistryName());
		if(renderer != null)
			renderer.render(action, matrixStack, projectedView);
	}
	
	public static abstract class ActionRenderer<T extends GroupAction>
	{
		public abstract void render(T action, PoseStack matrixStack, Camera projectedView);
		
		/** Typically used to indicate points of interest */
		protected void outlineCubeAt(BlockPos pos, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			Vec3 min = new Vec3(pos.getX(), pos.getY(), pos.getZ());
			outlineAABB(new AABB(min.x, min.y, min.z, min.x + 1D, min.y + 1D, min.z + 1D), projectedView, matrixStack, r, g, b);
		}
		
		/** Typically used to highlight zones of operation */
		protected void outlineAABB(AABB bounds, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			Vec3 camera = projectedView.getPosition();
			Vec3 min = new Vec3(bounds.minX, bounds.minY, bounds.minZ).subtract(camera);
			Vec3 max = new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ).subtract(camera);
			
			drawSquareBetween(new Vec3(min.x, min.y, min.z), new Vec3(max.x, min.y, max.z), matrixStack, r, g, b);
			drawSquareBetween(new Vec3(min.x, max.y, min.z), new Vec3(max.x, max.y, max.z), matrixStack, r, g, b);
			drawSquareBetween(new Vec3(min.x, min.y, min.z), new Vec3(max.x, max.y, min.z), matrixStack, r, g, b);
			drawSquareBetween(new Vec3(min.x, min.y, max.z), new Vec3(max.x, max.y, max.z), matrixStack, r, g, b);
		}
		
		/** Typically used to indicate objects of hostility */
		protected void drawTriangleAt(BlockPos point, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			drawTriangleAt(point, 0.5D, projectedView, matrixStack, r, g, b);
		}
		
		/** Typically used to indicate objects of hostility */
		protected void drawTriangleAt(BlockPos point, double scale, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			drawTriangleAt(new Vec3(point.getX() + 0.5D, point.getY(), point.getZ() + 0.5D), scale, 0F, projectedView, matrixStack, r, g, b);
		}
		
		/** Typically used to indicate objects of hostility */
		protected void drawTriangleAt(Vec3 point, double scale, double degreesRot, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			scale *= 0.5D;
			point = point.subtract(projectedView.getPosition());
			degreesRot = Math.toRadians(degreesRot);
			
			matrixStack.pushPose();
				Vec3 p1 = point.add(new Vec3(scale, 0, 0).yRot((float)degreesRot));
				Vec3 p2 = point.add(new Vec3(-scale, 0D, scale).yRot((float)degreesRot));
				Vec3 p3 = point.add(new Vec3(-scale, 0D, -scale).yRot((float)degreesRot));
				
				VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lineStrip());
				Matrix4f matrix = matrixStack.last().pose();
				Matrix3f normal = matrixStack.last().normal();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p3.x, (float)p3.y, (float)p3.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
			matrixStack.popPose();
		}
		
		/** Typically used to indicate member positions */
		protected void drawSquareAt(BlockPos point, double sideLength, Camera projectedView, PoseStack matrixStack, float r, float g, float b)
		{
			drawSquareAt(new Vec3(point.getX() + 0.5D, point.getY(), point.getZ() + 0.5D).subtract(projectedView.getPosition()), sideLength, matrixStack, r, g, b);
		}
		
		/** Typically used to indicate member positions */
		protected void drawSquareAt(Vec3 point, double sideLength, PoseStack matrixStack, float r, float g, float b)
		{
			sideLength /= 2D;
			Vec3 p1 = point.add(sideLength, 0D, sideLength);
			Vec3 p3 = point.add(-sideLength, 0D, -sideLength);
			drawSquareBetween(p1, p3, matrixStack, r, g, b);
		}
		
		protected void drawSquareBetween(Vec3 min, Vec3 max, PoseStack matrixStack, float r, float g, float b)
		{
			matrixStack.pushPose();
				Vec3 p1 = new Vec3(min.x, min.y, max.z);
				Vec3 p2 = new Vec3(max.x, min.y, max.z);
				Vec3 p3 = new Vec3(max.x, max.y, min.z);
				Vec3 p4 = new Vec3(min.x, max.y, min.z);
				
				VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lineStrip());
				Matrix4f matrix = matrixStack.last().pose();
				Matrix3f normal = matrixStack.last().normal();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p3.x, (float)p3.y, (float)p3.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p4.x, (float)p4.y, (float)p4.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
				builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
			matrixStack.popPose();
		}
	}
}
