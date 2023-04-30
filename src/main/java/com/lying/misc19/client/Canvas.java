package com.lying.misc19.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import com.lying.misc19.client.renderer.RenderUtils;
import com.lying.misc19.utility.M19Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

/** Cohesive layered drawing object */
public class Canvas
{
	public static final int SPRITES = 0;
	public static final int GLYPHS = 1;
	public static final int EXCLUSIONS = 2;
	public static final int DECORATIONS = 3;
	
	private Map<Integer, List<ICanvasObject>> elements = new HashMap<>();
	
	public void addElement(ICanvasObject object, int zLevel)
	{
		List<ICanvasObject> objects = elements.getOrDefault(Math.max(0, zLevel), Lists.newArrayList());
		objects.add(object);
		elements.put(zLevel, objects);
	}
	
	public void drawIntoGUI(PoseStack matrixStack)
	{
		draw(matrixStack, (element, matrix, exclusions) -> element.drawGui(matrixStack, exclusions));
	}
	
	public void drawIntoWorld(PoseStack matrixStack, MultiBufferSource bufferSource)
	{
		matrixStack.pushPose();
			float scale = 0.005F;
			matrixStack.scale(scale, -scale, scale);
			draw(matrixStack, (element, matrix, exclusions) -> element.drawWorld(matrixStack, bufferSource, exclusions));
		matrixStack.popPose();
	}
	
	private void draw(PoseStack matrixStack, TriConsumer<ICanvasObject, PoseStack, List<Exclusion>> func)
	{
		List<Integer> levels = Lists.newArrayList();
		levels.addAll(elements.keySet());
		levels.sort(Collections.reverseOrder());
		
		for(int i : levels)
			elements.get(i).forEach((element) -> func.accept(element,matrixStack, getExclusionsAbove(i)));
	}
	
	public List<Exclusion> getExclusionsAbove(int level)
	{
		List<Exclusion> exclusions = Lists.newArrayList();
		
		for(Entry<Integer, List<ICanvasObject>> entry : elements.entrySet())
			if(entry.getKey() < level)
				entry.getValue().forEach((object) -> {
					if(object.getClass() == Exclusion.class)
						exclusions.add((Exclusion)object); } );
		
		return exclusions;
	}
	
	public interface ICanvasObject
	{
		public void drawGui(PoseStack matrixStack, List<Exclusion> exclusions);
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Exclusion> exclusions);
	}
	
	public static class Circle implements ICanvasObject
	{
		private final Vec2 position;
		private final float radius, thickness;
		private final int r, g, b, a;
		
		public Circle(Vec2 pos, float radiusIn, float thicknessIn)
		{
			this(pos, radiusIn, thicknessIn, 255, 255, 255, 255);
		}
		
		public Circle(Vec2 pos, float radiusIn, float thicknessIn, int red, int green, int blue, int alpha)
		{
			this.position = pos;
			this.radius = radiusIn;
			this.thickness = thicknessIn;
			this.r = red;
			this.g = green;
			this.b = blue;
			this.a = alpha;
		}
		
		public void drawGui(PoseStack matrixStack, List<Exclusion> exclusions)
		{
			RenderUtils.drawOutlineCircle(position, radius, thickness, r, g, b, a, exclusions);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Exclusion> exclusions)
		{
			RenderUtils.drawOutlineCircle(matrixStack, bufferSource, position, radius, thickness, r, g, b, a);
		}
	}
	
	public static class Line implements ICanvasObject
	{
		private final Vec2 start, end;
		private final float thickness;
		private final int r, g, b, a;
		
		public Line(Vec2 posA, Vec2 posB, float thickness)
		{
			this(posA, posB, thickness, 255, 255, 255, 255);
		}
		
		public Line(Vec2 posA, Vec2 posB, float thickness, int red, int green, int blue, int alpha)
		{
			this.start = posA;
			this.end = posB;
			this.thickness = thickness;
			this.r = red;
			this.g = green;
			this.b = blue;
			this.a = alpha;
		}
		
		public void drawGui(PoseStack matrixStack, List<Exclusion> exclusions)
		{
			RenderUtils.drawColorLine(start, end, thickness, r, g, b, a, exclusions);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Exclusion> exclusions)
		{
			RenderUtils.drawColorLine(matrixStack, bufferSource, start, end, thickness, r, g, b, a);
		}
	}
	
	public static class Sprite implements ICanvasObject
	{
		private final ResourceLocation textureLocation;
		private final int width, height;
		private final Vec2 position;
		
		public Sprite(ResourceLocation texture, Vec2 position, int width, int height)
		{
			this.textureLocation = texture;
			this.position = position;
			this.width = width;
			this.height = height;
		}
		
		public void drawGui(PoseStack matrixStack, List<Exclusion> exclusions)
		{
		    RenderSystem.setShader(GameRenderer::getPositionTexShader);
		    RenderSystem.setShaderTexture(0, textureLocation);
		    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			RenderUtils.draw(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX, (buffer) -> 
			{
				Vec2 topLeft = new Vec2(position.x - width / 2, position.y - height / 2);
				Vec2 topRight = new Vec2(position.x + width / 2, position.y - height / 2);
				Vec2 botRight = new Vec2(position.x + width / 2, position.y + height / 2);
				Vec2 botLeft = new Vec2(position.x - width / 2, position.y + height / 2);
				
				buffer.vertex(topLeft.x, topLeft.y, 0).uv(0F, 0F).endVertex();
				buffer.vertex(botLeft.x, botLeft.y, 0).uv(0F, 1F).endVertex();
				buffer.vertex(botRight.x, botRight.y, 0).uv(1F, 1F).endVertex();
				buffer.vertex(topRight.x, topRight.y, 0).uv(1F, 0F).endVertex();
			});
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Exclusion> exclusions)
		{
			Vec2 topLeft = new Vec2(position.x - width / 2, position.y - height / 2);
			Vec2 topRight = new Vec2(position.x + width / 2, position.y - height / 2);
			Vec2 botRight = new Vec2(position.x + width / 2, position.y + height / 2);
			Vec2 botLeft = new Vec2(position.x - width / 2, position.y + height / 2);
			VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(textureLocation));
			Matrix4f matrix = matrixStack.last().pose();
			
			matrixStack.pushPose();
				buffer.vertex(matrix, topLeft.x, topLeft.y, 0F).color(255, 255, 255, 255).uv(1F, 0F).uv2(255).endVertex();
				buffer.vertex(matrix, topRight.x, topRight.y, 0F).color(255, 255, 255, 255).uv(0F, 0F).uv2(255).endVertex();
				buffer.vertex(matrix, botRight.x, botRight.y, 0F).color(255, 255, 255, 255).uv(0F, 1F).uv2(255).endVertex();
				buffer.vertex(matrix, botLeft.x, botLeft.y, 0F).color(255, 255, 255, 255).uv(1F, 1F).uv2(255).endVertex();
			matrixStack.popPose();
			
			matrixStack.pushPose();
				buffer.vertex(matrix, botLeft.x, botLeft.y, 0F).color(255, 255, 255, 255).uv(0F, 1F).uv2(255).endVertex();
				buffer.vertex(matrix, botRight.x, botRight.y, 0F).color(255, 255, 255, 255).uv(1F, 1F).uv2(255).endVertex();
				buffer.vertex(matrix, topRight.x, topRight.y, 0F).color(255, 255, 255, 255).uv(1F, 0F).uv2(255).endVertex();
				buffer.vertex(matrix, topLeft.x, topLeft.y, 0F).color(255, 255, 255, 255).uv(0F, 0F).uv2(255).endVertex();
			matrixStack.popPose();
		}
	}
	
	/** Defines a quad where canvas objects below should not be drawn */
	public static class Exclusion implements ICanvasObject
	{
		private final Vec2 xy, Xy, XY, xY;
		
		private final double xy2XY, Xy2xY;
		private final List<Pair<Vec2, Vec2>> bounds;
		
		public Exclusion(Vec2 xyIn, Vec2 XyIn, Vec2 XYin, Vec2 xYIn)
		{
			this.xy = xyIn;
			this.Xy = XyIn;
			this.XY = XYin;
			this.xY = xYIn;
			
			this.xy2XY = xy.distanceToSqr(XY);
			this.Xy2xY = Xy.distanceToSqr(xY);
			
			this.bounds = List.of(Pair.of(xy, Xy), Pair.of(Xy, XY), Pair.of(XY, xY), Pair.of(xY, xy));
		}
		
		public void drawGui(PoseStack matrixStack, List<Exclusion> exclusions) { }
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Exclusion> exclusions) { }
		
		/** Returns true if the given point is contained inbetween the points of this exclusion */
		public boolean isInside(Vec2 point)
		{
			// Any point inside the quad will be closer to all opposing diagonal corners than they are to eachother
			double distxy = point.distanceToSqr(xy);
			double distXY = point.distanceToSqr(XY);
			
			double distXy = point.distanceToSqr(Xy);
			double distxY = point.distanceToSqr(xY);
			
			return distxy < xy2XY && distXY < xy2XY || distXy < Xy2xY && distxY < Xy2xY;
		}
		
		/** Returns the point at which the given line intersects the bounds of this exclusion */
		@Nullable
		public Vec2 intercept(Vec2 posA, Vec2 posB)
		{
			if(isInside(posA) == isInside(posB))
				return null;
			
			Vec2 target = isInside(posA) ? posA : posB;
			Vec2 closest = null;
			double minDist = Double.MAX_VALUE;
			for(Pair<Vec2, Vec2> bound : bounds)
			{
				
				Vec2 interceptBetween = null;
				
				if(interceptBetween.distanceToSqr(target) < minDist)
				{
					minDist = interceptBetween.distanceToSqr(target);
					closest = interceptBetween;
				}
			}
			
			return closest;
		}
	}
}
