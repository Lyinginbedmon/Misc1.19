package com.lying.misc19.client.renderer.magic;

import java.util.List;

import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.client.renderer.RenderUtils;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.reference.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public class ComponentRenderer
{
	public void drawGlyph(ISpellComponent component, PoseStack matrixStack)
	{
		drawGlyph(component.getRegistryName(), component.position(), matrixStack);
	    drawChildGlyphs(component, matrixStack);
	}
	
	protected static void drawChildGlyphs(ISpellComponent component, PoseStack matrixStack)
	{
		component.inputs().forEach((input) -> ComponentRenderers.renderGUIGlyph(input, matrixStack));
		component.outputs().forEach((output) -> ComponentRenderers.renderGUIGlyph(output, matrixStack));
	}
	
	public void drawPattern(ISpellComponent component, PoseStack matrixStack)
	{
		RenderUtils.drawCircle(component.position(), 10, 1.25F);
	    drawChildPatterns(component, matrixStack);
	}
	
	protected static void drawChildPatterns(ISpellComponent component, PoseStack matrixStack)
	{
		component.inputs().forEach((input) -> ComponentRenderers.renderGUIPattern(input, matrixStack));
		component.outputs().forEach((output) -> ComponentRenderers.renderGUIPattern(output, matrixStack));
	}
	
	// FIXME Render glyph texture properly and centred on component position
	protected static void drawGlyph(ResourceLocation registryName, Vec2 position, PoseStack matrixStack)
	{
		matrixStack.pushPose();
			RenderSystem.enableBlend();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
		    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		    RenderSystem.setShaderTexture(0, new ResourceLocation(Reference.ModInfo.MOD_ID, "textures/magic/"+registryName.getPath()+".png"));
		    blit(matrixStack, (int)(position.x - 8), (int)(position.y - 8), 0, 0, 16, 16);
		    RenderSystem.disableBlend();
	    matrixStack.popPose();
	}
	
	public static void blit(PoseStack p_93201_, int p_93202_, int p_93203_, int p_93204_, int p_93205_, int p_93206_, TextureAtlasSprite p_93207_)
	{
		innerBlit(p_93201_.last().pose(), p_93202_, p_93202_ + p_93205_, p_93203_, p_93203_ + p_93206_, p_93204_, p_93207_.getU0(), p_93207_.getU1(), p_93207_.getV0(), p_93207_.getV1());
	}
	
	public static void blit(PoseStack p_93229_, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_)
	{
		blit(p_93229_, p_93230_, p_93231_, 0, (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
	}
	
	public static void blit(PoseStack p_93144_, int p_93145_, int p_93146_, int p_93147_, float p_93148_, float p_93149_, int p_93150_, int p_93151_, int p_93152_, int p_93153_)
	{
		innerBlit(p_93144_, p_93145_, p_93145_ + p_93150_, p_93146_, p_93146_ + p_93151_, p_93147_, p_93150_, p_93151_, p_93148_, p_93149_, p_93152_, p_93153_);
	}
	
	public static void blit(PoseStack p_93161_, int p_93162_, int p_93163_, int p_93164_, int p_93165_, float p_93166_, float p_93167_, int p_93168_, int p_93169_, int p_93170_, int p_93171_)
	{
		innerBlit(p_93161_, p_93162_, p_93162_ + p_93164_, p_93163_, p_93163_ + p_93165_, 0, p_93168_, p_93169_, p_93166_, p_93167_, p_93170_, p_93171_);
	}
	
	public static void blit(PoseStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_)
	{
		blit(p_93134_, p_93135_, p_93136_, p_93139_, p_93140_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
	}
	
	private static void innerBlit(PoseStack p_93188_, int p_93189_, int p_93190_, int p_93191_, int p_93192_, int p_93193_, int p_93194_, int p_93195_, float p_93196_, float p_93197_, int p_93198_, int p_93199_)
	{
		innerBlit(p_93188_.last().pose(), p_93189_, p_93190_, p_93191_, p_93192_, p_93193_, (p_93196_ + 0.0F) / (float)p_93198_, (p_93196_ + (float)p_93194_) / (float)p_93198_, (p_93197_ + 0.0F) / (float)p_93199_, (p_93197_ + (float)p_93195_) / (float)p_93199_);
	}
	
	private static void innerBlit(Matrix4f p_93113_, int p_93114_, int p_93115_, int p_93116_, int p_93117_, int p_93118_, float p_93119_, float p_93120_, float p_93121_, float p_93122_)
	{
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(p_93113_, (float)p_93114_, (float)p_93117_, (float)p_93118_).uv(p_93119_, p_93122_).endVertex();
		bufferbuilder.vertex(p_93113_, (float)p_93115_, (float)p_93117_, (float)p_93118_).uv(p_93120_, p_93122_).endVertex();
		bufferbuilder.vertex(p_93113_, (float)p_93115_, (float)p_93116_, (float)p_93118_).uv(p_93120_, p_93121_).endVertex();
		bufferbuilder.vertex(p_93113_, (float)p_93114_, (float)p_93116_, (float)p_93118_).uv(p_93119_, p_93121_).endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
	}
	
	public final void renderInputs(List<ISpellComponent> inputs, PoseStack matrixStack)
	{
		inputs.forEach((input) -> ComponentRenderers.renderGUIPattern(input, matrixStack));
	}
	
	public final void renderOutputs(List<ISpellComponent> outputs, PoseStack matrixStack)
	{
		outputs.forEach((output) -> ComponentRenderers.renderGUIPattern(output, matrixStack));
	}
}
