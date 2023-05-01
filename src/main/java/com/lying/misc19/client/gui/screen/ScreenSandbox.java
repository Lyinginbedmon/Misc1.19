package com.lying.misc19.client.gui.screen;

import com.lying.misc19.client.gui.menu.MenuSandbox;
import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ScreenSandbox extends Screen implements MenuAccess<MenuSandbox>
{
	private final MenuSandbox menu;
	
	public ScreenSandbox(MenuSandbox menuIn, Inventory inv, Component p_96550_)
	{
		super(p_96550_);
		this.menu = menuIn;
	}
	
	public MenuSandbox getMenu() { return this.menu; }
	
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
//		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		
		ISpellComponent arrangement = menu.arrangement();
		arrangement.setPositionAndOrganise(width / 2, height / 2);
		ComponentRenderers.renderGUI(arrangement, matrixStack);
	}
}
