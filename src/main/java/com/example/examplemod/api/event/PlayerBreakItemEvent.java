package com.example.examplemod.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class PlayerBreakItemEvent extends PlayerEvent
{
	private final ItemStack item;
	
	public PlayerBreakItemEvent(Player player, ItemStack itemIn)
	{
		super(player);
		this.item = itemIn;
	}
	
	public ItemStack getItem() { return this.item; }
}
