package com.example.examplemod.deities.personality;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

public class PersonalityContext
{
	private Map<ResourceLocation, Double> quotients = new HashMap<>();
	private Player player;
	
	public PersonalityContext(Player playerIn)
	{
		this.player = playerIn;
	}
	
	public double getQuotient(ResourceLocation nameIn)
	{
		if(!quotients.containsKey(nameIn))
		{
			RegistryObject<ContextQuotient> entry = ContextQuotients.getByName(nameIn);
			if(entry != null)
				this.quotients.put(nameIn, Mth.clamp(entry.get().get(this.player), 0D, 1D));
		}
		
		return this.quotients.getOrDefault(nameIn, 0D);
	}
}
