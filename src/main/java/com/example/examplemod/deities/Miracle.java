package com.example.examplemod.deities;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class Miracle
{
	public static final ResourceKey<Registry<Miracle>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "miracles"));
	
	public abstract float getUtility(Player playerIn, Level worldIn);
	
	public abstract void perform(Player playerIn, Level worldIn);
}
