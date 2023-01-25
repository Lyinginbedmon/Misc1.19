package com.example.examplemod.init;

import com.example.examplemod.enchantment.UnobtainableEnchantment;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ExEnchantments
{
	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Reference.ModInfo.MOD_ID);
	
	public static final RegistryObject<Enchantment> CONTRACT_TOOL	= ENCHANTMENTS.register("contract_tool", () -> new UnobtainableEnchantment(Rarity.VERY_RARE, EnchantmentCategory.DIGGER, EquipmentSlot.MAINHAND));
	
	public static void init() { }
}
