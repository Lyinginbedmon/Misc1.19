package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

@Mixin(BrewingStandBlockEntity.class)
public interface AccessorBrewingStandBlockEntity
{
	@Accessor("items")
	NonNullList<ItemStack> getItems();
	
	@Accessor("brewTime")
	int getBrewTime();
}
