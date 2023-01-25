package com.example.examplemod.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class UnobtainableEnchantment extends Enchantment
{
	public UnobtainableEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... slots)
	{
		super(rarity, category, slots);
	}
	
	public boolean canApplyAtEnchantingTable(ItemStack stack) { return false; }
	public boolean isTreasureOnly() { return true; }
	public boolean isCurse() { return true; }
	public boolean isDiscoverable() { return false; }
	public boolean isAllowedOnBooks() { return false; }
}
