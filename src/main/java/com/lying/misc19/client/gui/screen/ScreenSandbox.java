package com.lying.misc19.client.gui.screen;

import com.lying.misc19.client.gui.menu.MenuSandbox;
import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec2;

public class ScreenSandbox extends Screen implements MenuAccess<MenuSandbox>
{
	private final MenuSandbox menu;
	private Vec2 position = Vec2.ZERO, moveStart = null;
	private boolean isMoving = false;
	
	public ScreenSandbox(MenuSandbox menuIn, Inventory inv, Component p_96550_)
	{
		super(p_96550_);
		this.menu = menuIn;
	}
	
	public MenuSandbox getMenu() { return this.menu; }
	
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		
		float scrollX = position.x, scrollY = position.y;
		if(isMoving)
		{
			scrollX += mouseX - moveStart.x;
			scrollY += mouseY - moveStart.y;
		}
		
		ISpellComponent arrangement = menu.arrangement();
		arrangement.setPositionAndOrganise((width / 2) + scrollX, (height / 2) + scrollY);
		ComponentRenderers.renderGUI(arrangement, matrixStack);
	}
	
	public boolean mouseClicked(double x, double y, int mouseKey)
	{
		if(mouseKey == 0)
		{
			isMoving = true;
			moveStart = new Vec2((float)x, (float)y);
		}
		return super.mouseClicked(x, y, mouseKey);
	}
	
	public boolean mouseReleased(double x, double y, int mouseKey)
	{
		if(mouseKey == 0 && isMoving)
		{
			float xOff = (float)x - moveStart.x;
			float yOff = (float)y - moveStart.y;
			Vec2 addMove = new Vec2(xOff, yOff);
			position = position.add(addMove);
			
			isMoving = false;
			moveStart = null;
		}
		return super.mouseReleased(x, y, mouseKey);
	}
}
