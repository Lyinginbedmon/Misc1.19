package com.lying.misc19.client.gui.screen;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.lwjgl.glfw.GLFW;

import com.lying.misc19.client.Canvas;
import com.lying.misc19.client.gui.menu.MenuSandbox;
import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.init.M19Items;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.item.ScrollItem;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.ISpellComponent.Category;
import com.lying.misc19.magic.ISpellComponent.Type;
import com.lying.misc19.reference.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

public class ScreenSandbox extends Screen implements MenuAccess<MenuSandbox>
{
	public static final ResourceLocation SAND_BACKGROUND = new ResourceLocation(Reference.ModInfo.MOD_ID, "textures/gui/sandbox_background.png");
	private final MenuSandbox menu;
	private final Inventory playerInv;
	private Vec2 position = Vec2.ZERO;
	private Vec2 lastPosition = Vec2.ZERO;
	private Vec2 moveStart = null;
	private boolean isMoving = false;
	
	private Canvas canvas = null;
	
	/** The last part we were hovering over, if any */
	private ISpellComponent hoveredPart = null;
	
	private GlyphList glyphList;
	/** The part we have selected to add with left-click */
	public ISpellComponent attachPart = null;
	
	private ISpellComponent selectedPart = null;
	
	private Button printButton, copyButton, pasteButton;
	
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
		this.addWidget(this.glyphList = new GlyphList(this.minecraft, this, 150, this.height, 20, this.height - 20, 20));
		
		this.addRenderableWidget(new Button(0, 0, 10, 20, Component.literal("<"), (button) -> 
		{
			this.glyphList.decCategory(menu.arrangement());
		}));
		this.addRenderableWidget(new Button(140, 0, 10, 20, Component.literal(">"), (button) -> 
		{
			this.glyphList.incCategory(menu.arrangement());
		}));
		this.addRenderableWidget(printButton = new Button(0, this.height - 20, 100, 20, Component.literal("Print"), (button) -> 
		{
			ItemStack spell = new ItemStack(M19Items.MAGIC_SCROLL.get());
			if(menu.arrangement() != null)
			{
				ScrollItem.setSpell(spell, menu.arrangement());
				this.playerInv.add(spell);
			}
			onClose();
		}));
		this.addRenderableWidget(copyButton = new Button(105, this.height - 20, 20, 20, Component.literal("Copy"), (button) -> 
		{
			this.minecraft.keyboardHandler.setClipboard(ISpellComponent.saveToNBT(menu.arrangement()).toString());
		}));
		this.addRenderableWidget(pasteButton = new Button(130, this.height - 20, 20, 20, Component.literal("Paste"), (button) -> 
		{
			String clipboard = this.minecraft.keyboardHandler.getClipboard();
			try
			{
				Tag parsed = TagParser.parseTag(clipboard);
				if(parsed.getId() == Tag.TAG_COMPOUND)
				{
					ISpellComponent comp = SpellComponents.readFromNBT((CompoundTag)parsed);
					if(comp.getRegistryName() != SpellComponents.GLYPH_DUMMY)
					{
						if(menu.arrangement() == null && comp.type() == Type.ROOT)
						{
							menu.setArrangement(comp);
							this.position = Vec2.ZERO;
						}
						else
							setNewPart(comp);
					}
				}
			}
			catch(Exception e) { }
		}));
	}
	
	public void tick()
	{
		this.printButton.active = this.copyButton.active = menu.arrangement() != null;
		this.pasteButton.active = !this.minecraft.keyboardHandler.getClipboard().isEmpty();
		
		// Only show core glyphs if the current arrangement is empty
		this.glyphList.checkCategory(menu.arrangement());
	}
	
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
//		this.renderSandBackground(0);
		this.renderBackground(matrixStack);
		
		if(menu.arrangement() != null)
		{
			ISpellComponent arrangement = menu.arrangement();
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
			
			arrangement.setPosition((width / 2) + scrollX, (height / 2) + scrollY);
			
			Vec2 currentPos = new Vec2(scrollX, scrollY);
			if(currentPos != lastPosition)
				updateCanvas(arrangement);
			this.canvas.drawIntoGUI(matrixStack, width, height);
			this.lastPosition = currentPos;
			
			hoveredPart = getComponentAt(mouseX, mouseY);
		}
		
		this.glyphList.setLeftPos(0);
		this.glyphList.render(matrixStack, mouseX, mouseY, partialTicks);
		
		if(hoveredPart != null && attachPart == null)
		{
			List<Component> tooltip = Lists.newArrayList();
			tooltip.add(hoveredPart.translatedName().withStyle(ChatFormatting.BOLD));
			tooltip.add(hoveredPart.category().translate());
			tooltip.add(hoveredPart.description().withStyle(ChatFormatting.ITALIC));
			
			this.renderComponentTooltip(matrixStack, tooltip, mouseX, mouseY);
		}
		else if(attachPart != null)
		{
			attachPart.setPosition(mouseX, mouseY);
			ComponentRenderers.renderGUI(attachPart, matrixStack, width, height);
			
			if(hoveredPart != null)
			{
				boolean input = getAddState(hoveredPart, mouseX, mouseY, attachPart);
				boolean valid = input ? hoveredPart.isValidInput(attachPart) : hoveredPart.isValidOutput(attachPart);
				if(valid)
					this.renderTooltip(matrixStack, Component.translatable("gui."+Reference.ModInfo.MOD_ID+".sandbox.add_"+(input ? "input" : "output")), mouseX, mouseY);
			}
		}
		
		// TODO Render selection as a sprite instead of just showing the tooltip
		if(this.selectedPart != null)
			this.renderTooltip(matrixStack, selectedPart.translatedName(), (int)selectedPart.position().x, (int)selectedPart.position().y);
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	/** Returns true if the part will be added as an input, false for an output */
	private boolean getAddState(ISpellComponent target, int mouseX, int mouseY, ISpellComponent part)
	{
		boolean canBeInput = target.isValidInput(part);
		boolean canBeOutput = target.isValidOutput(part);
		
		if(canBeInput == canBeOutput)
		{
			Vec2 up = target.up();
			Vec2 core = target.position();
			Vec2 dir = new Vec2((float)mouseX - core.x, (float)mouseY - core.y).normalized();
			return up.dot(dir) > 0;
		}
		else
			return canBeInput;
	}
	
	public void renderSandBackground(int offset)
	{
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, SAND_BACKGROUND);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		float f = 32.0F;
		int brightness = 235;
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex(0.0D, (double)this.height, 0.0D).uv(0.0F, (float)this.height / f + (float)offset).color(brightness, brightness, brightness, 255).endVertex();
			bufferbuilder.vertex((double)this.width, (double)this.height, 0.0D).uv((float)this.width / f, (float)this.height / f + (float)offset).color(brightness, brightness, brightness, 255).endVertex();
			bufferbuilder.vertex((double)this.width, 0.0D, 0.0D).uv((float)this.width / f, (float)offset).color(brightness, brightness, brightness, 255).endVertex();
			bufferbuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, (float)offset).color(brightness, brightness, brightness, 255).endVertex();
		tesselator.end();
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, new PoseStack()));
	}
	
	private void updateCanvas(ISpellComponent arrangement)
	{
		this.canvas = ComponentRenderers.populateCanvas(arrangement, null);
	}
	
	public void setNewPart(ResourceLocation component)
	{
		setNewPart(SpellComponents.create(component));
		clearSelected();
	}
	
	public void setNewPart(ISpellComponent component)
	{
		this.attachPart = SpellComponents.readFromNBT(ISpellComponent.saveToNBT(component));
	}
	
	@Nullable
	public ISpellComponent getComponentAt(int mouseX, int mouseY)
	{
		if(this.glyphList.isMouseOver(mouseX, mouseY))
			return null;
		
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
		return menu.arrangement() == null ? Lists.newArrayList() : menu.arrangement().getParts();
	}
	
	public boolean tryAddGlyph(ISpellComponent recipient, ISpellComponent input, boolean asInput)
	{
		if(menu.arrangement() == null)
		{
			if(input.category() == Category.ROOT)
			{
				menu.setArrangement(input);
				menu.arrangement().organise();
				updateCanvas(menu.arrangement());
				this.glyphList.incCategory(menu.arrangement());
				return true;
			}
		}
		else if(recipient != null)
		{
			boolean result = false;
			if(asInput && recipient.isValidInput(input))
			{
				recipient.addInput(input);
				result = true;
			}
			else if(!asInput && recipient.isValidOutput(input))
			{
				recipient.addOutput(input);
				result = true;
			}
			if(result)
			{
				recipient.organise();
				updateCanvas(menu.arrangement());
			}
			return result;
		}
		return false;
	}
	
	public boolean mouseClicked(double x, double y, int mouseKey)
	{
		if(this.glyphList.isMouseOver(x, y))
			return this.glyphList.mouseClicked(x, y, mouseKey);
		
		if(attachPart != null)
		{
			if(mouseKey == 0)
			{
				boolean asInput = false;
				ISpellComponent target = null;
				
				if(menu.arrangement() != null && hoveredPart != null)
				{
					target = hoveredPart;
					asInput = getAddState(target, (int)x, (int)y, attachPart);
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
		else if(mouseKey == 0 && !super.mouseClicked(x, y, mouseKey) && !this.glyphList.isMouseOver(x, y))
		{
			if(hoveredPart == null)
			{
				this.isMoving = true;
				moveStart = new Vec2((int)x, (int)y);
			}
			else
				this.selectedPart = hoveredPart;
			
			return true;
		}
		return false;
	}
	
	public boolean mouseReleased(double x, double y, int mouseKey)
	{
		if(this.glyphList.isMouseOver(x, y))
			return this.glyphList.mouseReleased(x, y, mouseKey);
		
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
	
	public boolean keyPressed(int keyID, int scanCode, int modifiers)
	{
		if(selectedPart != null)
		{
			if(keyID == GLFW.GLFW_KEY_DELETE || keyID == GLFW.GLFW_KEY_BACKSPACE)
			{
				handleDelete(selectedPart);
				clearSelected();
			}
			else if(modifiers == 2)	// Left control held
				if(keyID == GLFW.GLFW_KEY_X)
				{
					// Cut
					setNewPart(selectedPart);
					handleDelete(selectedPart);
					clearSelected();
				}
				else if(keyID == GLFW.GLFW_KEY_C)
				{
					// Copy
					setNewPart(selectedPart);
				}
			return true;
		}
		else
			switch(keyID)
			{
				case GLFW.GLFW_KEY_LEFT:
					this.position = this.position.add(new Vec2(10, 0));
					return true;
				case GLFW.GLFW_KEY_RIGHT:
					this.position = this.position.add(new Vec2(-10, 0));
					return true;
				case GLFW.GLFW_KEY_UP:
					this.position = this.position.add(new Vec2(0, 10));
					return true;
				case GLFW.GLFW_KEY_DOWN:
					this.position = this.position.add(new Vec2(0, -10));
					return true;
				default:
					return super.keyPressed(keyID, scanCode, modifiers);
			}
	}
	
	private void handleDelete(ISpellComponent part)
	{
		if(part == null)
			return;
		
		ISpellComponent parent = part.parent();
		if(parent == null && part.type() == Type.ROOT)
			menu.setArrangement(null);
		else
		{
			parent.remove(part);
			parent.organise();
			updateCanvas(menu.arrangement());
		}
	}
	
	private void clearSelected() { this.selectedPart = null; }
}
