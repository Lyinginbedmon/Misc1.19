package com.example.examplemod.api.event;

import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * PlayerEnchantItemEvent is fired whenever the player enchants something.<br>
 * <br>
 * {@link #stack} contains the item after it has been enchanted.
 * This event is fired in {@link EnchantedItemTrigger#trigger(net.minecraft.server.level.ServerPlayer, ItemStack, int)}.<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 * @author Remem
 */
public class PlayerEnchantItemEvent extends PlayerEvent
{
	private final ItemStack stack;
	private final int enchantLevel;
	
	public PlayerEnchantItemEvent(Player player, ItemStack stackIn, int levelIn)
	{
		super(player);
		stack = stackIn;
		enchantLevel = levelIn;
	}
	
	public ItemStack getItem() { return stack.copy(); }
	public int getLevel() { return enchantLevel; }
}
