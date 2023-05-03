package com.lying.misc19.client.gui.menu;

import com.lying.misc19.init.M19Menus;
import com.lying.misc19.magic.ISpellComponent;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MenuSandbox extends AbstractContainerMenu
{
	private ISpellComponent arrangement = null;
	
	public MenuSandbox(int containerId, Inventory inv)
	{
		super(M19Menus.SANDBOX_MENU.get(), containerId);
	}
	
	public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
	
	public boolean stillValid(Player player) { return true; }
	
	public ISpellComponent arrangement() { return this.arrangement; }
	
	public void setArrangement(ISpellComponent spellIn) { this.arrangement = spellIn; }
}
