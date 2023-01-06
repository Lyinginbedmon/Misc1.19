package com.example.examplemod.deities.personality;

import javax.annotation.Nullable;

import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.registries.RegistryObject;

public class Opinion
{
	public static final ResourceKey<Registry<Opinion>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "traits"));
	
	private final double a, b;
	private final ResourceLocation quotientId;
	
	public Opinion(double atZero, double atOne, ResourceLocation registryIn)
	{
		this.a = atZero;
		this.b = atOne;
		this.quotientId = registryIn;
	}
	public Opinion(double staticVal)
	{
		this(staticVal, staticVal, ContextQuotients.STATIC.getId());
	}
	
	@Nullable
	public ResourceLocation getRegistryName()
	{
		for(RegistryObject<Opinion> entry : ExRegistries.TRAITS.getEntries())
			if(entry.isPresent() && entry.get().equals(this))
				return entry.getId();
		return null;
	}
	
	public ResourceLocation quotient() { return this.quotientId; }
	
	public Tuple<Double, Double> range(){ return new Tuple<Double,Double>(this.a, this.b); }
	
	public boolean equals(Opinion varB)
	{
		return this.a == varB.a && this.b == varB.b && this.quotientId.equals(varB.quotientId);
	}
	
	public double value(PersonalityContext contextIn)
	{
		if(this.a == this.b)
			return this.a;
		
		double range = b - a;
		return a + (contextIn.getQuotient(quotientId) * range);
	}
}