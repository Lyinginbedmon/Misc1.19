package com.lying.misc19.client.renderer;

import java.util.function.Consumer;

import com.google.common.base.Function;
import com.lying.misc19.utility.M19Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec2;

public class RenderUtils
{
	public static void drawColorLine(Vec2 posA, Vec2 posB, float width)
	{
		drawColorLine(posA, posB, width, 255, 255, 255, 255);
	}
	
	public static void drawColorLine(Vec2 posA, Vec2 posB, float width, int r, int g, int b, int a)
	{
	    RenderSystem.setShader(GameRenderer::getPositionColorShader);
	    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		draw(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, (buffer) -> 
		{
			Vec2 offset = M19Utils.rotate(posB.add(posA.negated()).normalized(), 90D);
			
			Vec2 topRight = posA.add(offset.scale(width / 2));
			Vec2 topLeft = posA.add(offset.scale(width / 2).negated());
			Vec2 botLeft = posB.add(offset.scale(width / 2).negated());
			Vec2 botRight = posB.add(offset.scale(width / 2));
			
			buffer.vertex(topRight.x, topRight.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(botRight.x, botRight.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(botLeft.x, botLeft.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(topLeft.x, topLeft.y, 0).color(r, g, b, a).endVertex();
		});
	}
	
	public static void drawColorSquare(Vec2 posA, Vec2 posB, int r, int g, int b, int a)
	{
	    RenderSystem.setShader(GameRenderer::getPositionColorShader);
	    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		draw(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, (buffer) -> 
		{
			buffer.vertex(posA.x, posB.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(posB.x, posB.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(posB.x, posA.y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(posA.x, posA.y, 0).color(r, g, b, a).endVertex();
		});
	}
	
	public static void drawCircle(Vec2 posA, float radius, float thickness)
	{
		drawCircle(posA, radius, thickness, 255, 255, 255, 255);
	}
	
	public static void drawCircle(Vec2 posA, float radius, float thickness, int r, int g, int b, int a)
	{
		Vec2 offset = new Vec2(radius, 0);
		for(int i=0; i<360; i++)
			drawColorLine(posA.add(offset), posA.add(offset = M19Utils.rotate(offset, 1D)), thickness, r, g, b, a);
	}
	
	public static void draw(VertexFormat.Mode drawMode, VertexFormat format, Consumer<BufferBuilder> func)
	{
		draw(drawMode, format, bufferBuilder ->
		{
			func.accept(bufferBuilder);
			return null;
		});
	}
	
	private static <R> R draw(VertexFormat.Mode drawMode, VertexFormat format, Function<BufferBuilder, R> func)
	{
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(drawMode, format);
		R result = func.apply(buffer);
		tesselator.end();
		return result;
	}
}
