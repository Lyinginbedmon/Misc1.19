package com.example.examplemod.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * PlayerEnchantEvent is fired whenever the player enchants something.<br>
 * This event is fired in {@link Player#onEnchantmentPerformed(ItemStack, int)}.<br>
 * <br>
 * {@link #stack} contains the item prior to applying enchantments.<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 * @author Remem
 */
public class PlayerEnchantEvent extends PlayerEvent
{
	private final ItemStack stack;
	private final int enchantLevel;
	
	public PlayerEnchantEvent(Player player, ItemStack stackIn, int levelIn)
	{
		super(player);
		stack = stackIn;
		enchantLevel = levelIn;
	}
	
	public ItemStack getItem() { return stack.copy(); }
	public int getLevel() { return enchantLevel; }
}
