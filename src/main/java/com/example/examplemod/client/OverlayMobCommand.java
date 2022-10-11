package com.example.examplemod.client;

import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.example.examplemod.utility.MobCommanding.NotifyData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class OverlayMobCommand implements IGuiOverlay
{
	private static final ResourceLocation COMMAND_TEXTURES = new ResourceLocation(Reference.ModInfo.MOD_PREFIX+"textures/gui/companion_commands.png");
	private static final Minecraft mc = Minecraft.getInstance();
	private static final Font font = mc.font;
	public static final Style STYLE_ISSUE = Style.EMPTY.withItalic(true);
    private static final int ICON_SIZE = 16;
    
	public void render(ForgeGui gui, PoseStack matrixStack, float partialTicks, int width, int height)
	{
		if(MobCommanding.isMarking())
			drawOptionSet(MobCommanding.currentAction(), matrixStack, width, height);
		
		if(MobCommanding.hasRecipients())
			drawScreenList(matrixStack, width, height);
	}
	
	public void drawOptionSet(Mark current, PoseStack matrixStack, int width, int height)
	{
		if(current == null)
			return;
		
		int xLev = width / 2;
		int yLev = (height + font.lineHeight * 2) / 2;
		
		MutableComponent textCur = Component.literal("{ ").append(current.translate(MobCommanding.currentTarget()).setStyle(STYLE_ISSUE)).append(Component.literal(" }"));
        font.draw(matrixStack, textCur.getString(), xLev - (font.width(textCur.getString()) / 2), yLev, -1);
        
        Mark[] options = MobCommanding.currentOptions();
        int optionsSize = options.length;
        int index = 0;
        while(optionsSize > 0 && index < options.length)
        {
        	int row = Math.min(optionsSize, 8);
            int barX = xLev - (row * ICON_SIZE + (row - 1)) / 2;
        	for(int i=0; i<row; i++)
        	{
	        	Mark option = MobCommanding.currentOptions()[index++];
	        	drawCommandIcon(matrixStack, option, barX + i * ICON_SIZE + i, yLev + 10, option == current);
	        	
	        	optionsSize--;
	        	if(index > options.length)
	        		break;
        	}
        	yLev += ICON_SIZE + 1;
        }
	}
	
	public void drawScreenList(PoseStack matrixStack, int width, int height)
	{
		int xLev = 5;
		int yLev = 0;
		font.draw(matrixStack, Component.literal("Next order recipients:"), xLev, 0, -1);
		for(NotifyData data : MobCommanding.getOrderRecipients())
		{
			font.draw(matrixStack, data.translate(), xLev + 10, font.lineHeight + yLev + 1, -1);
			yLev += font.lineHeight + 1;
		};
	}
	
	private void drawCommandIcon(PoseStack matrixStack, Mark type, int x, int y, boolean highlight)
	{
		int index = type.iconIndex();
		matrixStack.pushPose();
			float colour = highlight ? 1F : 0.5F;
			RenderSystem.setShaderColor(colour, colour, colour, 1F);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderTexture(0, COMMAND_TEXTURES);
			
			int col = index % 16;
			int row = Math.floorDiv(index, 16);
			
			blit(matrixStack, x, y, col * ICON_SIZE, row * ICON_SIZE, ICON_SIZE, ICON_SIZE);
		matrixStack.popPose();
		
	}
	
	public void blit(PoseStack p_93229_, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_)
	{
		blit(p_93229_, p_93230_, p_93231_, -90, (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
	}
	
	public static void blit(PoseStack p_93144_, int p_93145_, int p_93146_, int p_93147_, float p_93148_, float p_93149_, int p_93150_, int p_93151_, int p_93152_, int p_93153_)
	{
		innerBlit(p_93144_, p_93145_, p_93145_ + p_93150_, p_93146_, p_93146_ + p_93151_, p_93147_, p_93150_, p_93151_, p_93148_, p_93149_, p_93152_, p_93153_);
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
}
