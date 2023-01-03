package com.example.examplemod.deities.personality;

import javax.annotation.Nullable;

import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

public class Opinion
{
	public static final ResourceKey<Registry<Opinion>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "traits"));
	
	double view = 0;
	ResourceLocation quotientId;
	
	public Opinion(double viewIn, ResourceLocation registryIn)
	{
		this.view = viewIn;
		this.quotientId = registryIn;
	}
	
	@Nullable
	public ResourceLocation getRegistryName()
	{
		for(RegistryObject<Opinion> entry : ExRegistries.TRAITS.getEntries())
			if(entry.isPresent() && entry.get().equals(this))
				return entry.getId();
		return null;
	}
	
	public boolean equals(Opinion varB)
	{
		return this.view == varB.view && this.quotientId.equals(varB.quotientId);
	}
}