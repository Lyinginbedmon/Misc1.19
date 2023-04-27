package com.lying.misc19.client.renderer;

import java.util.function.Consumer;

import com.google.common.base.Function;
import com.lying.misc19.utility.M19Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec2;

public class RenderUtils
{
	/** Draws a coloured line into a GUI screen between the given points */
	public static void drawColorLine(Vec2 posA, Vec2 posB, float width)
	{
		drawColorLine(posA, posB, width, 255, 255, 255, 255);
	}
	
	/** Draws a coloured line into a GUI screen between the given points */
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
	
	/** Draws a coloured line into the world between the given points */
	public static void drawColorLine(PoseStack matrixStack, MultiBufferSource bufferSource, Vec2 posA, Vec2 posB, float width, int r, int g, int b, int a)
	{
		Vec2 offset = M19Utils.rotate(posB.add(posA.negated()).normalized(), 90D);
		Vec2 topRight = posA.add(offset.scale(width / 2));
		Vec2 topLeft = posA.add(offset.scale(width / 2).negated());
		Vec2 botLeft = posB.add(offset.scale(width / 2).negated());
		Vec2 botRight = posB.add(offset.scale(width / 2));
		
		drawBlockColorSquare(matrixStack, bufferSource, topLeft, topRight, botLeft, botRight, r, g, b, a);
	}
	
	public static void drawOutlineColorSquare(Vec2 centre, float width, float height, float thickness, int r, int g, int b, int a)
	{
		float minX = centre.x - width / 2;
		float maxX = centre.x + width / 2;
		float minY = centre.y - height / 2;
		float maxY = centre.y + height / 2;
		
		RenderUtils.drawColorLine(new Vec2(minX, minY), new Vec2(minX, maxY), thickness, r, g, b, a);
		RenderUtils.drawColorLine(new Vec2(maxX, minY), new Vec2(maxX, maxY), thickness, r, g, b, a);
		RenderUtils.drawColorLine(new Vec2(minX, minY), new Vec2(maxX, minY), thickness, r, g, b, a);
		RenderUtils.drawColorLine(new Vec2(minX, maxY), new Vec2(maxX, maxY), thickness, r, g, b, a);
	}
	
	public static void drawBlockColorSquare(Vec2 posA, Vec2 posB, int r, int g, int b, int a)
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

	/** Draws a coloured square into the world */
	public static void drawBlockColorSquare(PoseStack matrixStack, MultiBufferSource bufferSource, Vec2 topLeft, Vec2 topRight, Vec2 botLeft, Vec2 botRight, int r, int g, int b, int a)
	{
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f matrix = matrixStack.last().pose();
		
		matrixStack.pushPose();
			buffer.vertex(matrix, topLeft.x, topLeft.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, topRight.x, topRight.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, botRight.x, botRight.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, botLeft.x, botRight.y, 0F).color(r, g, b, a).endVertex();
		matrixStack.popPose();
		
		matrixStack.pushPose();
			buffer.vertex(matrix, botLeft.x, botRight.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, botRight.x, botRight.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, topRight.x, topRight.y, 0F).color(r, g, b, a).endVertex();
			buffer.vertex(matrix, topLeft.x, topLeft.y, 0F).color(r, g, b, a).endVertex();
		matrixStack.popPose();
	}
	
	public static void drawOutlineCircle(Vec2 posA, float radius, float thickness)
	{
		drawOutlineCircle(posA, radius, thickness, 255, 255, 255, 255);
	}
	
	public static void drawOutlineCircle(Vec2 posA, float radius, float thickness, int r, int g, int b, int a)
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
