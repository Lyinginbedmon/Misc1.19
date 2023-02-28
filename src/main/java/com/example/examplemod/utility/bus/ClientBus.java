package com.example.examplemod.utility.bus;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entities.EntityHearthLight;
import com.example.examplemod.utility.HearthLightPathfinder;
import com.example.examplemod.utility.PathingSearch;
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
//		float r = 1F, g = 1F, b = 1F;
		for(EntityHearthLight entity : player.getLevel().getEntitiesOfClass(EntityHearthLight.class, player.getBoundingBox().inflate(32D), (light) -> light.isVisibleFor(player))) 
		{
			HearthLightPathfinder pathfinder = entity.getPathfinder();
			if(pathfinder == null)
				continue;
			
			if(pathfinder.searchCompleted() && entity.hasPathToShow())
			{
				List<BlockPos> path = entity.getPathfinder().getPath();
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
			else
			{
				PathingSearch search = pathfinder.currentSearch();
				for(Pair<BlockPos, BlockPos> evaluate : search.nodesToEvaluate)
				{
					BlockPos pos = evaluate.getKey();
					Vec3 min = new Vec3(pos.getX(), pos.getY(), pos.getZ()).subtract(offset);
					Vec3 max = min.add(1, 0, 1);
					
					float r = 0.24F, g = 0.7F, b = 0.53F;
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
				
				for(Map.Entry<BlockPos, BlockPos> node : search.nodeGraph.entrySet())
				{
					BlockPos node1 = node.getKey();
					BlockPos node2 = node.getValue();
					
					matrixStack.pushPose();
						Vec3 p1 = new Vec3(node1.getX() + 0.5D, node1.getY(), node1.getZ() + 0.5D).subtract(offset);
						Vec3 p2 = new Vec3(node2.getX() + 0.5D, node2.getY(), node2.getZ() + 0.5D).subtract(offset);
						
						VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
						Matrix4f matrix = matrixStack.last().pose();
						Matrix3f normal = matrixStack.last().normal();
						builder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(0F, 0F, 0F, 1F).normal(normal, 1, 0, 0).endVertex();
						builder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(1F, 1F, 1F, 1F).normal(normal, 1, 0, 0).endVertex();
					matrixStack.popPose();
				}
			}
		};
	}
}
