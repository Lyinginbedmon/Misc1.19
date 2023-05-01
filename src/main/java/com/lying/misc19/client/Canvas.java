package com.lying.misc19.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.client.renderer.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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
	
	private void draw(PoseStack matrixStack, TriConsumer<ICanvasObject, PoseStack, List<Quad>> func)
	{
		RenderUtils.testColor = true;
		
		List<Integer> levels = Lists.newArrayList();
		levels.addAll(elements.keySet());
		levels.sort(Collections.reverseOrder());
		
		for(int i : levels)
			elements.get(i).forEach((element) -> func.accept(element,matrixStack, getExclusionsBelow(i)));
	}
	
	public List<Quad> getExclusionsBelow(int level)
	{
		List<Quad> exclusions = Lists.newArrayList();
		for(Entry<Integer, List<ICanvasObject>> entry : elements.entrySet())
			if(entry.getKey() < level)
				entry.getValue().forEach((object) -> {
					if(object.isExclusion())
						exclusions.addAll(((ICanvasExclusion)object).getQuads()); } );
		return exclusions;
	}
	
	public interface ICanvasObject
	{
		public void drawGui(PoseStack matrixStack, List<Quad> exclusions);
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Quad> exclusions);
		
		public default boolean isExclusion() { return false; }
	}
	
	public interface ICanvasExclusion extends ICanvasObject
	{
		public default boolean isExclusion() { return true; }
		
		public List<Quad> getQuads();
		
		public default void drawGui(PoseStack matrixStack, List<Quad> exclusions) { }
		public default void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Quad> exclusions) { }
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
		
		public void drawGui(PoseStack matrixStack, List<Quad> exclusions)
		{
			RenderUtils.drawOutlineCircle(position, radius, thickness, r, g, b, a, exclusions);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Quad> exclusions)
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
		
		public void drawGui(PoseStack matrixStack, List<Quad> exclusions)
		{
			RenderUtils.drawColorLine(start, end, thickness, r, g, b, a, exclusions);
		}
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Quad> exclusions)
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
		
		public void drawGui(PoseStack matrixStack, List<Quad> exclusions)
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
		
		public void drawWorld(PoseStack matrixStack, MultiBufferSource bufferSource, List<Quad> exclusions)
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
	public static class ExclusionQuad implements ICanvasExclusion
	{
		private final Quad quad;
		
		public ExclusionQuad(Vec2 xyIn, Vec2 XyIn, Vec2 XYin, Vec2 xYIn)
		{
			this.quad = new Quad(xyIn, XyIn, XYin, xYIn);
		}
		
		public List<Quad> getQuads(){ return List.of(this.quad); }
		
		public void drawGui(PoseStack matrixStack, List<Quad> exclusions)
		{
			Line lineAB = new Line(quad.a(), quad.b(), 1, 255, 255, 255, 255);
			Line lineBC = new Line(quad.b(), quad.c(), 1, 255, 255, 255, 255);
			Line lineCD = new Line(quad.c(), quad.d(), 1, 255, 255, 255, 255);
			Line lineDA = new Line(quad.d(), quad.a(), 1, 255, 255, 255, 255);
			
			lineAB.drawGui(matrixStack, Lists.newArrayList());
			lineBC.drawGui(matrixStack, Lists.newArrayList());
			lineCD.drawGui(matrixStack, Lists.newArrayList());
			lineDA.drawGui(matrixStack, Lists.newArrayList());
		}
	}
}
