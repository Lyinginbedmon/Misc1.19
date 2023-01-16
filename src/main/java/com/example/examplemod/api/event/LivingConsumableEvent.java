package com.example.examplemod.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class LivingConsumableEvent extends LivingEvent
{
	private final ItemStack item;
	
	public LivingConsumableEvent(LivingEntity player, ItemStack stackIn)
	{
		super(player);
		this.item = stackIn;
	}
	
	public ItemStack getItem() { return this.item.copy(); }
	
	/**
	 * LivingDrinkEvent is fired when an entity finishes drinking an item.<br>
	 * This event is fired in {@link ItemStack#finishUsingItem(net.minecraft.world.level.Level, LivingEntity)}.<br>
	 * <br>
	 * This event is {@link Cancelable}.<br>
	 * If this event is cancelled, the potion is not drank or consumed.<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
	 * @author Remem
	 */
	public static class Drink extends LivingConsumableEvent
	{
		public Drink(LivingEntity player, ItemStack stackIn)
		{
			super(player, stackIn);
		}
	}
	
	/**
	 * LivingEatEvent is fired when an entity finishes eating a food item.<br>
	 * This event is fired in {@link ItemStack#finishUsingItem(net.minecraft.world.level.Level, LivingEntity)}.<br>
	 * <br>
	 * This event is {@link Cancelable}.<br>
	 * If this event is cancelled, the food is not eaten or consumed.<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
	 * @author Remem
	 */
	public static class Eat extends LivingConsumableEvent
	{
		public Eat(LivingEntity player, ItemStack stackIn)
		{
			super(player, stackIn);
		}
	}
}
