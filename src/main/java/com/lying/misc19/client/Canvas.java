package com.lying.misc19.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.client.renderer.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;

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
		draw(matrixStack, (element, matrix) -> element.drawGUI(matrixStack));
	}
	
	public void drawIntoWorld(PoseStack matrixStack)
	{
		draw(matrixStack, (element, matrix) -> element.drawWorld(matrixStack));
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
		public void drawGUI(PoseStack matrixStack);
		
		public void drawWorld(PoseStack matrixStack);
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
		
		public void drawGUI(PoseStack matrixStack)
		{
			RenderUtils.drawCircle(position, radius, thickness, r, g, b, a);
		}
		
		public void drawWorld(PoseStack matrixStack) { }
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
		
		public void drawGUI(PoseStack matrixStack)
		{
			RenderUtils.drawColorLine(start, end, thickness, r, g, b, a);
		}
		
		public void drawWorld(PoseStack matrixStack) { }
	}
}
