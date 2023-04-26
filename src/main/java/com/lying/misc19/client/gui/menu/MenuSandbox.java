package com.lying.misc19.client.gui.menu;

import com.lying.misc19.init.M19Menus;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VariableSet.Slot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MenuSandbox extends AbstractContainerMenu
{
	private ISpellComponent arrangement;
	
	public MenuSandbox(int containerId, Inventory inv)
	{
		super(M19Menus.SANDBOX_MENU.get(), containerId);
		
		setArrangement(SpellComponents.create(SpellComponents.ROOT_DUMMY).addOutputs(
				SpellComponents.create(SpellComponents.GLYPH_SET).addInputs(SpellComponents.create(SpellComponents.GLYPH_FALSE)).addOutputs(SpellComponents.create(Slot.BAST.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_SET).addInputs(SpellComponents.create(SpellComponents.GLYPH_TRUE)).addOutputs(SpellComponents.create(Slot.THOTH.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_SET).addInputs(SpellComponents.create(SpellComponents.GLYPH_FALSE)).addOutputs(SpellComponents.create(Slot.SUTEKH.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_XOR).addInputs(SpellComponents.create(Slot.BAST.glyph()), SpellComponents.create(Slot.THOTH.glyph())).addOutputs(SpellComponents.create(Slot.ANUBIS.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_AND).addInputs(SpellComponents.create(Slot.ANUBIS.glyph()), SpellComponents.create(Slot.SUTEKH.glyph())).addOutputs(SpellComponents.create(Slot.HORUS.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_AND).addInputs(SpellComponents.create(Slot.BAST.glyph()), SpellComponents.create(Slot.THOTH.glyph())).addOutputs(SpellComponents.create(Slot.ISIS.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_OR).addInputs(SpellComponents.create(Slot.HORUS.glyph()), SpellComponents.create(Slot.ISIS.glyph())).addOutputs(SpellComponents.create(Slot.RA.glyph())),
				SpellComponents.create(SpellComponents.GLYPH_XOR).addInputs(SpellComponents.create(Slot.ANUBIS.glyph()), SpellComponents.create(Slot.SUTEKH.glyph())).addOutputs(SpellComponents.create(Slot.OSIRIS.glyph()))));
	}
	
	public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
	
	public boolean stillValid(Player player) { return true; }
	
	public ISpellComponent arrangement() { return this.arrangement; }
	
	public void setArrangement(ISpellComponent spellIn) { this.arrangement = spellIn; }
}
