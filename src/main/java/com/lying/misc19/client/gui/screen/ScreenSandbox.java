package com.lying.misc19.client.gui.screen;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.client.gui.menu.MenuSandbox;
import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.init.M19Items;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.item.ScrollItem;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.ISpellComponent.Category;
import com.lying.misc19.magic.ISpellComponent.Type;
import com.lying.misc19.magic.variable.VariableSet;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

public class ScreenSandbox extends Screen implements MenuAccess<MenuSandbox>
{
	private final MenuSandbox menu;
	private final Inventory playerInv;
	private Vec2 position = Vec2.ZERO;
	private Vec2 moveStart = null;
	private boolean isMoving = false;
	
	/** The total arrangement */
	ISpellComponent arrangement = null;
	
	/** The last part we were hovering over, if any */
	ISpellComponent hoveredPart = null;
	
	/** The part we have selected to add with left-click */
	ISpellComponent attachPart = null;
	
	ISpellComponent selectedPart = null;
	
	Button rootButton, printButton;
	
	public ScreenSandbox(MenuSandbox menuIn, Inventory inv, Component p_96550_)
	{
		super(p_96550_);
		this.menu = menuIn;
		this.playerInv = inv;
	}
	
	public MenuSandbox getMenu() { return this.menu; }
	
	protected void init()
	{
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
		this.addRenderableWidget(rootButton = new Button(40, 40, 20, 20, Component.literal("ROOT"), (button) -> { setNewPart(SpellComponents.create(SpellComponents.ROOT_CASTER)); }));
		this.addRenderableWidget(new Button(40, 60, 20, 20, Component.literal("OPERATION"), (button) -> { setNewPart(SpellComponents.create(SpellComponents.GLYPH_SET)); }));
		this.addRenderableWidget(new Button(40, 80, 20, 20, Component.literal("VARIABLE1"), (button) -> { setNewPart(SpellComponents.create(SpellComponents.SIGIL_XYZ)); }));
		this.addRenderableWidget(new Button(40, 100, 20, 20, Component.literal("VARIABLE2"), (button) -> { setNewPart(SpellComponents.create(VariableSet.Slot.APEP.glyph())); }));
		this.addRenderableWidget(new Button(40, 120, 20, 20, Component.literal("FUNCTION"), (button) -> { setNewPart(SpellComponents.create(SpellComponents.GLYPH_DEBUG)); }));
		
		this.addRenderableWidget(printButton = new Button(40, 200, 20, 20, Component.literal("Print"), (button) -> 
		{
			ItemStack spell = new ItemStack(M19Items.MAGIC_SCROLL.get());
			if(arrangement != null)
			{
				ScrollItem.setSpell(spell, arrangement);
				this.playerInv.add(spell);
			}
			onClose();
		}));
	}
	
	public void tick()
	{
		this.rootButton.active = arrangement == null;
		this.printButton.active = arrangement != null;
	}
	
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		arrangement = menu.arrangement();
		hoveredPart = getComponentAt(mouseX, mouseY);
		
		float scrollX = position.x, scrollY = position.y;
		if(isMoving)
		{
			Vec2 shift = new Vec2(mouseX - moveStart.x, mouseY - moveStart.y);
			if(shift.length() > 5)
			{
				scrollX += mouseX - moveStart.x;
				scrollY += mouseY - moveStart.y;
			}
		}
		
		if(arrangement == null)
			return;
		
		arrangement.setPositionAndOrganise((width / 2) + scrollX, (height / 2) + scrollY);
		ComponentRenderers.renderGUI(arrangement, matrixStack);
		
		if(hoveredPart != null && attachPart == null)
		{
			List<Component> tooltip = Lists.newArrayList();
			tooltip.add(hoveredPart.translatedName().withStyle(ChatFormatting.BOLD));
			tooltip.add(Component.literal(hoveredPart.category().name()));
			tooltip.add(hoveredPart.description().withStyle(ChatFormatting.ITALIC));
			
			this.renderComponentTooltip(matrixStack, tooltip, mouseX, mouseY);
		}
		else if(attachPart != null)
		{
			attachPart.setPosition(mouseX, mouseY);
			ComponentRenderers.renderGUI(attachPart, matrixStack);
			
			if(hoveredPart != null)
			{
				boolean input = inInputArea(hoveredPart, mouseX, mouseY);
				boolean valid = input ? hoveredPart.isValidInput(attachPart) : hoveredPart.isValidOutput(attachPart);
				if(valid)
					this.renderTooltip(matrixStack, Component.literal(input ? "Input" : "Output"), mouseX, mouseY);
			}
		}
		
		if(this.selectedPart != null)
			this.renderTooltip(matrixStack, selectedPart.translatedName(), (int)selectedPart.position().x, (int)selectedPart.position().y);
	}
	
	@Nullable
	public ISpellComponent getComponentAt(int mouseX, int mouseY)
	{
		Vec2 pos = new Vec2(mouseX, mouseY);
		double minDist = Double.MAX_VALUE;
		ISpellComponent hovered = null;
		
		for(ISpellComponent component : getTotalComponents())
		{
			double dist = Math.sqrt(pos.distanceToSqr(component.position()));
			int scale = ComponentRenderers.get(component.getRegistryName()).spriteScale();
			
			if(dist <= scale && dist < minDist)
			{
				hovered = component;
				minDist = dist;
			}
		}
		
		return hovered;
	}
	
	public List<ISpellComponent> getTotalComponents()
	{
		return arrangement == null ? Lists.newArrayList() : arrangement.getParts();
	}
	
	public boolean tryAddGlyph(ISpellComponent recipient, ISpellComponent input, boolean isInput)
	{
		if(recipient == null && input.category() == Category.ROOT)
		{
			menu.setArrangement(input);
			return true;
		}
		else
		{
			if(isInput && recipient.isValidInput(input))
			{
				recipient.addInput(input);
				return true;
			}
			else if(!isInput && recipient.isValidOutput(input))
			{
				recipient.addOutput(input);
				return true;
			}
			
			menu.setArrangement(arrangement);
		}
		return false;
	}
	
	public boolean mouseClicked(double x, double y, int mouseKey)
	{
		if(attachPart != null)
		{
			if(mouseKey == 0)
			{
				boolean asInput = false;
				ISpellComponent target = null;
				
				if(arrangement != null && hoveredPart != null)
				{
					target = hoveredPart;
					asInput = inInputArea(target, (int)x, (int)y);
				}
				
				if(tryAddGlyph(target, attachPart, asInput))
				{
					attachPart = null;
					return true;
				}
			}
			else if(mouseKey == 1)
			{
				attachPart = null;
				return true;
			}
		}
		else if(!super.mouseClicked(x, y, mouseKey))
		{
			this.selectedPart = hoveredPart;
			
			this.isMoving = true;
			moveStart = new Vec2((int)x, (int)y);
			return true;
		}
		return false;
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
	
	public boolean inInputArea(ISpellComponent target, int x, int y)
	{
		Vec2 up = target.up();
		Vec2 core = target.position();
		Vec2 dir = new Vec2((float)x - core.x, (float)y - core.y).normalized();
		return up.dot(dir) > 0;
	}
	
	public boolean keyPressed(int keyID, int p_97879_, int p_97880_)
	{
		if(selectedPart != null && (keyID == 261 || keyID == 259))
		{
			ISpellComponent parent = selectedPart.parent();
			if(parent == null && selectedPart.type() == Type.ROOT)
				menu.setArrangement(null);
			else
				parent.remove(selectedPart);
			
			selectedPart = null;
			return true;
		}
		else
			return super.keyPressed(keyID, p_97879_, p_97880_);
	}
	
	public void setNewPart(ISpellComponent component)
	{
		this.attachPart = component;
		this.selectedPart = null;
	}
}
