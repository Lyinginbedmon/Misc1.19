package com.lying.misc19.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.client.renderer.RenderUtils;
import com.lying.misc19.utility.M19Utils;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

/** Cohesive layered drawing object */
public class Canvas
{
	private Map<Integer, List<ICanvasObject>> elements = new HashMap<>();
	
	public void addElement(ICanvasObject object, int zLevel)
	{
		List<ICanvasObject> objects = elements.getOrDefault(zLevel, Lists.newArrayList());
		objects.add(object);
		elements.put(zLevel, objects);
	}
	
	public void drawIntoGUI(PoseStack matrixStack)
	{
		draw(matrixStack, (element, matrix) -> element.drawGui(matrixStack));
	}
	
	public void drawIntoWorld(PoseStack matrixStack, MultiBufferSource bufferSource)
	{
		matrixStack.pushPose();
			float scale = 0.005F;
			matrixStack.scale(scale, -scale, scale);
			draw(matrixStack, (element, matrix) -> element.drawWorld(matrixStack, bufferSource));
		matrixStack.popPose();
	}
	
	private void draw(PoseStack matrixStack, BiConsumer<ICanvasObject,PoseStack> func)
	{
		List<Integer> levels = Lists.newArrayList();
		levels.addAll(elements.keySet());
		levels.sort(Collections.reverseOrder());
		
		for(int i : levels)
			elements.get(i).forEach((element) -> func.accept(element,matrixStack));
	}
	
	public interface ICanvasObject
	{
		public void drawGui(PoseStack matrixStack);
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource);
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
		
		public void drawGui(PoseStack matrixStack)
		{
			RenderUtils.drawOutlineCircle(position, radius, thickness, r, g, b, a);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource)
		{
			// Existing line-based approach to drawing circles
			Vec2 offset = new Vec2(radius, 0);
			for(int i=0; i<360; i++)
			{
				Vec2 pos = position.add(offset);
				Vec2 pos2 = position.add(offset = M19Utils.rotate(offset, 1D));
				RenderUtils.drawColorLine(matrixStack, bufferSource, pos, pos2, thickness, 255, 0, 0, a);
			}
			
			// FIXME Correctly order points for square drawing
			Vec2 offsetOut = new Vec2(radius + thickness / 2, 0);
			Vec2 offsetIn = new Vec2(radius - thickness / 2, 0);
			int resolution = 64;
			float turn = 360F / resolution;
			for(int i=0; i<360; i++)
			{
				Vec2 topLeft = position.add(offsetOut);
				Vec2 topRight = position.add(offsetIn);
				Vec2 botRight = position.add(offsetIn = M19Utils.rotate(offsetIn, turn));
				Vec2 botLeft = position.add(offsetOut = M19Utils.rotate(offsetOut, turn));
				
				RenderUtils.drawBlockColorSquare(matrixStack, bufferSource, topLeft, topRight, botRight, botLeft, 0, 255, 0, a);
			}
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
		
		public void drawGui(PoseStack matrixStack)
		{
			RenderUtils.drawColorLine(start, end, thickness, r, g, b, a);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource)
		{
			RenderUtils.drawColorLine(matrixStack, bufferSource, start, end, thickness, r, g, b, a);
		}
	}
	
	public static class FilledSquare implements ICanvasObject
	{
		private final Vec2 centre;
		private final float width, height;
		private final int r, g, b, a;
		
		public FilledSquare(Vec2 centre, float width, float height)
		{
			this(centre, width, height, 255, 255, 255, 255);
		}
		
		public FilledSquare(Vec2 centre, float width, float height, int red, int green, int blue, int alpha)
		{
			this.centre = centre;
			this.width = width;
			this.height = height;
			this.r = red;
			this.g = green;
			this.b = blue;
			this.a = alpha;
		}
		
		public void drawGui(PoseStack matrixStack)
		{
			Vec2 min = centre.add(new Vec2(width, height).scale(-0.5F));
			Vec2 max = centre.add(new Vec2(width, height).scale(0.5F));
			
			RenderUtils.drawBlockColorSquare(min, max, r, g, b, a);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource)
		{
			float minX = centre.x - width / 2;
			float maxX = centre.x + width / 2;
			float minY = centre.y - height / 2;
			float maxY = centre.y + height / 2;
			Vec2 topLeft = new Vec2(minX, minY);
			Vec2 topRight = new Vec2(maxX, minY);
			Vec2 botLeft = new Vec2(minX, maxY);
			Vec2 botRight = new Vec2(maxX, maxY);
			
			RenderUtils.drawBlockColorSquare(matrixStack, bufferSource, topLeft, topRight, botLeft, botRight, r, g, b, a);
		}
	}
}
