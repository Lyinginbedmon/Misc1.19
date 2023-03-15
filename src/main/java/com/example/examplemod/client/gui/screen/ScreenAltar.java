package com.example.examplemod.client.gui.screen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.client.gui.menu.MenuAltar;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketStartPraying;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ScreenAltar extends Screen implements MenuAccess<MenuAltar>
{
	private final MenuAltar altarMenu;
	private Button chooseGodButton, startPrayingButton;
	
	public ScreenAltar()
	{
		this(null, null, Component.empty());
	}
	
	public ScreenAltar(MenuAltar altarIn, Inventory inv, Component displayName)
	{
		super(displayName);
		this.altarMenu = altarIn;
	}
	
	public MenuAltar getMenu()
	{
		return this.altarMenu;
	}
	
	public void onClose()
	{
		this.minecraft.player.closeContainer();
		super.onClose();
	}
	
	protected void init()
	{
         this.addRenderableWidget(this.chooseGodButton = new Button(this.width / 2 - 100, 196, 98, 20, Component.translatable("container.examplemod.altar.choose_god"), (button) -> {
            ExampleMod.LOG.info("Open god selection menu");
         }));
         this.addRenderableWidget(this.startPrayingButton = new Button(this.width / 2 + 2, 196, 98, 20, Component.translatable("container.examplemod.altar.pray"), (button) -> {
            ExampleMod.LOG.info("Begin praying");
            PacketHandler.sendToServer(new PacketStartPraying(this.minecraft.player.getUUID()));
         }));
	}
	
	public void tick()
	{
		PlayerData data = PlayerData.getCapability(this.minecraft.player);
		boolean isPraying = data.isPraying();
		this.chooseGodButton.active = !isPraying;
		this.startPrayingButton.active = !isPraying && (data.canPray() || this.minecraft.player.isCreative());
	}
	
	public boolean isPauseScreen() { return false; }
	
	protected void closeScreen()
	{
		this.minecraft.player.closeContainer();
        PacketHandler.sendToServer(new PacketStartPraying(this.minecraft.player.getUUID(), false));
	}
}
