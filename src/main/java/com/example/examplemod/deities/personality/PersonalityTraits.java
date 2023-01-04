package com.example.examplemod.deities.personality;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.example.examplemod.init.ExRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

public class PersonalityTraits
{
	public static final RegistryObject<Opinion> PACIFIST = register("pacifist", () -> new Opinion(-1, ContextQuotients.VIOLENCE.getId()));
	public static final RegistryObject<Opinion> BRUTE = register("brute", () -> new Opinion(1, ContextQuotients.VIOLENCE.getId()));
	public static final RegistryObject<Opinion> MASOCHIST = register("masochist", () -> new Opinion(1, ContextQuotients.DAMAGE_TAKEN.getId()));
	
	public static final RegistryObject<Opinion> NUDIST = register("nudist", () -> new Opinion(1, ContextQuotients.NUDITY.getId()));
	public static final RegistryObject<Opinion> GARBED = register("garbed", () -> new Opinion(-1, ContextQuotients.NUDITY.getId()));
	
	public static final RegistryObject<Opinion> SANGUINE = register("sanguine", () -> new Opinion(1, ContextQuotients.STATIC.getId()));
	public static final RegistryObject<Opinion> OPTIMIST = register("optimist", () -> new Opinion(0.5D, ContextQuotients.STATIC.getId()));
	public static final RegistryObject<Opinion> PESSIMIST = register("pessimist", () -> new Opinion(-0.5D, ContextQuotients.STATIC.getId()));
	public static final RegistryObject<Opinion> DEPRESSIVE = register("depressive", () -> new Opinion(-1, ContextQuotients.STATIC.getId()));
	
	public static final RegistryObject<Opinion> HERMIT = register("hermit", () -> new Opinion(-1, ContextQuotients.SOCIAL.getId()));
	public static final RegistryObject<Opinion> SOCIALITE = register("socialite", () -> new Opinion(1, ContextQuotients.SOCIAL.getId()));
	public static final RegistryObject<Opinion> ZOOLATER = register("zoolater", () -> new Opinion(1, ContextQuotients.ANIMALS.getId()));
	public static final RegistryObject<Opinion> ZOOPHOBE = register("zoophobe", () -> new Opinion(-1, ContextQuotients.ANIMALS.getId()));
	
	public static final RegistryObject<Opinion> DRUID = register("druid", () -> new Opinion(1, ContextQuotients.NATURE.getId()));
	public static final RegistryObject<Opinion> AGRIPHOBIC = register("agriphobic", () -> new Opinion(-1, ContextQuotients.NATURE.getId()));
	public static final RegistryObject<Opinion> AGORAPHOBIC = register("agoraphobic", () -> new Opinion(1, ContextQuotients.UNDERGROUND.getId()));
	public static final RegistryObject<Opinion> CLAUSTROPHOBIC = register("claustrophobic", () -> new Opinion(-1, ContextQuotients.UNDERGROUND.getId()));
	public static final RegistryObject<Opinion> SEAFARER = register("seafarer", () -> new Opinion(1, ContextQuotients.WATER.getId()));
	public static final RegistryObject<Opinion> LANDLOVER = register("landlover", () -> new Opinion(-1, ContextQuotients.WATER.getId()));
	
	public static final RegistryObject<Opinion> VEGETARIAN = register("vegetarian", () -> new Opinion(1, ContextQuotients.EAT_VEG.getId()));
	public static final RegistryObject<Opinion> CARNIVORE = register("carnivore", () -> new Opinion(1, ContextQuotients.EAT_MEAT.getId()));
	
	public static final RegistryObject<Opinion> SHADOW = register("shadow", () -> new Opinion(-1, ContextQuotients.LIGHT.getId()));
	public static final RegistryObject<Opinion> BRIGHT = register("bright", () -> new Opinion(1, ContextQuotients.LIGHT.getId()));
	
	public static final RegistryObject<Opinion> WANDERER = register("wanderer", () -> new Opinion(1, ContextQuotients.TRAVEL.getId()));
	public static final RegistryObject<Opinion> HOMEBODY = register("homebody", () -> new Opinion(-1, ContextQuotients.TRAVEL.getId()));
	
	public static final RegistryObject<Opinion> INVENTOR = register("inventor", () -> new Opinion(1, ContextQuotients.CRAFTING.getId()));
	
	private static RegistryObject<Opinion> register(String nameIn, Supplier<Opinion> opinionIn)
	{
		return ExRegistries.TRAITS.register(nameIn, opinionIn);
	}
	
	public static void init() { }
	
	@Nullable
	public static RegistryObject<Opinion> byRegistryName(ResourceLocation name)
	{
		for(RegistryObject<Opinion> trait : ExRegistries.TRAITS.getEntries())
			if(trait.getId().equals(name))
				return trait;
		return null;
	}
}
