package com.example.examplemod.deities.personality;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface ContextQuotient
{
	public static final ResourceKey<Registry<ContextQuotient>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "quotients"));
	
	public double get(Player playerIn);
}