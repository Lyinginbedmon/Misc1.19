package com.example.examplemod.utility.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.OverlayGodStatus;
import com.example.examplemod.entities.EntityHearthLight;
import com.example.examplemod.utility.ExUtils;
import com.example.examplemod.utility.pathfinding.HearthLightPathfinder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientBus
{
	public static Minecraft mc = Minecraft.getInstance();
	
	public static void registerOverlayEvent(RegisterGuiOverlaysEvent event)
	{
		ExampleMod.LOG.info("Registering overlays");
//		event.registerAboveAll("god_status", new OverlayGodStatus());
	}
	
	@SubscribeEvent
	public static void renderHearthLightPaths(RenderLevelStageEvent event)
	{
		if(event.getStage() != Stage.AFTER_CUTOUT_BLOCKS || mc.options.renderDebug) return;
		Player player = mc.player;
		
		PoseStack matrixStack = event.getPoseStack();
		Vec3 offset = event.getCamera().getPosition();
		for(EntityHearthLight entity : player.getLevel().getEntitiesOfClass(EntityHearthLight.class, player.getBoundingBox().inflate(32D), (light) -> light.isVisibleFor(player))) 
		{
			HearthLightPathfinder pathfinder = entity.getPathfinder();
			if(pathfinder == null)
				continue;
			
			// Visualise completed path as a weighted plot
			if(pathfinder.searchCompleted() && pathfinder.hasPath())
			{
				List<BlockPos> path = entity.getPathfinder().getPath();
				
				List<Vec3> points = pathToWeightedPoints(path);
				for(int i=1; i<points.size(); i++)
				{
					float col1 = (float)(i - 1) / (float)path.size();
					float col2 = (float)i / (float)path.size();
					
					matrixStack.pushPose();
						matrixStack.translate(0D, 0.5D, 0D);
						Vec3 p1 = points.get(i - 1).subtract(offset);
						Vec3 p2 = points.get(i).subtract(offset);
						
						VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
						Matrix4f matrix = matrixStack.last().pose();
						Matrix3f normal = matrixStack.last().normal();
						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(col1, col1, col1, 1F).normal(normal, 1, 0, 0).endVertex();
						builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(col2, col2, col2, 1F).normal(normal, 1, 0, 0).endVertex();
					matrixStack.popPose();
				}
				
				for(int i=0; i<path.size() - 1; i++)
				{
					BlockPos node1 = path.get(i);
					BlockPos node2 = path.get(i + 1);
					
					float col1 = (float)i / (float)path.size();
					float col2 = (float)(i+1) / (float)path.size();
					
					matrixStack.pushPose();
						Vec3 p1 = new Vec3(node1.getX() + 0.5D, node1.getY(), node1.getZ() + 0.5D).subtract(offset);
						Vec3 p2 = new Vec3(node2.getX() + 0.5D, node2.getY(), node2.getZ() + 0.5D).subtract(offset);
						
						VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
						Matrix4f matrix = matrixStack.last().pose();
						Matrix3f normal = matrixStack.last().normal();
						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(0F, col1, col1, 1F).normal(normal, 1, 0, 0).endVertex();
						builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(0F, col2, col2, 1F).normal(normal, 1, 0, 0).endVertex();
					matrixStack.popPose();
				}
			}
			// Visualise pathing in progress
//			else
//			{
//				AbstractPathingSearch search = pathfinder.currentSearch();
//				for(BlockPos pos : search.getEvaluatingList())
//				{
//					Vec3 min = new Vec3(pos.getX(), pos.getY(), pos.getZ()).subtract(offset);
//					Vec3 max = min.add(1, 0, 1);
//					
//					float r = 0.24F, g = 0.7F, b = 0.53F;
//					matrixStack.pushPose();
//						Vec3 p1 = new Vec3(min.x, min.y, max.z);
//						Vec3 p2 = new Vec3(max.x, min.y, max.z);
//						Vec3 p3 = new Vec3(max.x, max.y, min.z);
//						Vec3 p4 = new Vec3(min.x, max.y, min.z);
//						
//						VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lineStrip());
//						Matrix4f matrix = matrixStack.last().pose();
//						Matrix3f normal = matrixStack.last().normal();
//						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
//						builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
//						builder.vertex(matrix, (float)p3.x, (float)p3.y, (float)p3.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
//						builder.vertex(matrix, (float)p4.x, (float)p4.y, (float)p4.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
//						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 1F).normal(normal, 1, 0, 0).endVertex();
//					matrixStack.popPose();
//				}
//				
//				List<BlockPos> latestPath = search.getLatestPath();
//				for(int i=0; i<latestPath.size() - 1; i++)
//				{
//					BlockPos node1 = latestPath.get(i);
//					BlockPos node2 = latestPath.get(i + 1);
//					
//					matrixStack.pushPose();
//						Vec3 p1 = ExUtils.posToVec(node1).subtract(offset);
//						Vec3 p2 = ExUtils.posToVec(node2).subtract(offset);
//						
//						VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
//						Matrix4f matrix = matrixStack.last().pose();
//						Matrix3f normal = matrixStack.last().normal();
//						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(0F, 0F, 0F, 1F).normal(normal, 1, 0, 0).endVertex();
//						builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(1F, 1F, 1F, 1F).normal(normal, 1, 0, 0).endVertex();
//					matrixStack.popPose();
//				}
//			}
		};
	}
	
	public static List<Vec3> pathToWeightedPoints(List<BlockPos> path)
	{
		List<Vec3> points = Lists.newArrayList();
		
		for(BlockPos pos : path)
			points.add(getWeightedPosition(ExUtils.posToVec(pos).add(0D, 0.5D, 0D), path));
		
		return points;
	}
	
	private static Vec3 getWeightedPosition(Vec3 node, List<BlockPos> nodes)
	{
		// Lowest distance to node
		double minDist = Double.MAX_VALUE;
		
		// Map of nodes to their distance to target position
		Map<BlockPos, Double> distances = new HashMap<>();
		for(int i=0; i<nodes.size(); i++)
		{
			double dist = distAlongPath(node, i, nodes);
			distances.put(nodes.get(i), dist);
			minDist = Math.min(minDist, dist);
		}
		
		// Weighted position
		double weightSum = 0D;
		Vec3 weightedPos = Vec3.ZERO;
		for(BlockPos pos : nodes)
		{
			Vec3 position = ExUtils.posToVec(pos);
			double weight = minDist / distances.get(pos);
			weightedPos = weightedPos.add(position.scale(weight));
			weightSum += weight;
		}
		
		return weightedPos.scale(1 / weightSum);
	}
	
	private static double distAlongPath(Vec3 vec, int index, List<BlockPos> path)
	{
		int closestIndex = 0;
		double minDist = Double.MAX_VALUE;
		for(int i=0; i<path.size(); i++)
		{
			BlockPos node = path.get(i);
			Vec3 nodeVec = ExUtils.posToVec(node);
			double dist = nodeVec.distanceTo(vec);
			if(dist < minDist)
			{
				closestIndex = i;
				minDist = dist;
			}
		}
		
		double totalDistance = 0D;
		BlockPos prev = null;
		for(int i=Math.min(closestIndex, index); i<Math.max(closestIndex, index); i++)
		{
			BlockPos pos = path.get(i);
			if(prev != null)
				totalDistance += Math.sqrt(pos.distSqr(path.get(i + 1)));
			prev = pos;
		}
		return totalDistance + minDist;
	}
}
