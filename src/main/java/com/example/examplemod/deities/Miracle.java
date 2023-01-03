package com.example.examplemod.deities;

import java.util.Optional;

import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.reference.Reference;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class Miracle
{
	public static final ResourceKey<Registry<Miracle>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "miracles"));
	
	public abstract float getUtility(Player playerIn, Level worldIn);
	
	public abstract void perform(Player playerIn, Level worldIn);
	
	public final boolean is(TagKey<Miracle> keyIn)
	{
		Optional<Holder<Miracle>> holder = ExRegistries.MIRACLES_REGISTRY.get().getHolder(this);
		return holder.isPresent() && holder.get().is(keyIn);
	}
}
