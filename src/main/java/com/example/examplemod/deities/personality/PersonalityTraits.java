package com.example.examplemod.deities.personality;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.example.examplemod.init.ExRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

public class PersonalityTraits
{
	public static final RegistryObject<Opinion> PACIFIST = register("pacifist", () -> new Opinion(5, -12, ContextQuotients.DAMAGE_GIVEN.getId()));
	public static final RegistryObject<Opinion> WARMONGER = register("warmonger", () -> new Opinion(-12, 5, ContextQuotients.DAMAGE_GIVEN.getId()));
	public static final RegistryObject<Opinion> REVOLUTIONARY = register("revolutionary", () -> new Opinion(-5, 12, ContextQuotients.DAMAGE_BOSS.getId()));
	public static final RegistryObject<Opinion> BRUTE = register("brute", () -> new Opinion(0, 13, ContextQuotients.MELEE.getId()));
	public static final RegistryObject<Opinion> ARCHER = register("archery", () -> new Opinion(0, 13, ContextQuotients.ARCHERY.getId()));
	public static final RegistryObject<Opinion> KINGSLAYER = register("kingslayer", () -> new Opinion(-5, 12, ContextQuotients.KILL_BOSS.getId()));
	public static final RegistryObject<Opinion> MASOCHIST = register("masochist", () -> new Opinion(0, 20, ContextQuotients.DAMAGE_TAKEN.getId()));
	
	public static final RegistryObject<Opinion> NUDIST = register("nudist", () -> new Opinion(-6, 20, ContextQuotients.NUDITY.getId()));
	public static final RegistryObject<Opinion> GARBED = register("garbed", () -> new Opinion(20, -6, ContextQuotients.NUDITY.getId()));
	public static final RegistryObject<Opinion> RUSTIC = register("rustic", () -> new Opinion(-6, 20, ContextQuotients.WEAR_LEATHER.getId()));
	public static final RegistryObject<Opinion> STEELSKIN = register("steelskin", () -> new Opinion(-6, 20, ContextQuotients.WEAR_METAL.getId()));
	public static final RegistryObject<Opinion> ENCHANTED = register("enchanted", () -> new Opinion(-6, 20, ContextQuotients.EQUIP_MAGIC.getId()));
	public static final RegistryObject<Opinion> SUBLUNARY = register("sublunary", () -> new Opinion(20, -6, ContextQuotients.EQUIP_MAGIC.getId()));
	// Potion Addict
	// Teetotal
	// Tank
	// Gymrat
	// Badblood
	
	public static final RegistryObject<Opinion> SANGUINE = register("sanguine", () -> new Opinion(12));
	public static final RegistryObject<Opinion> OPTIMIST = register("optimist", () -> new Opinion(6));
	public static final RegistryObject<Opinion> PESSIMIST = register("pessimist", () -> new Opinion(-6));
	public static final RegistryObject<Opinion> DEPRESSIVE = register("depressive", () -> new Opinion(-12));
	
	public static final RegistryObject<Opinion> HERMIT = register("hermit", () -> new Opinion(12, -8, ContextQuotients.SOCIAL.getId()));
	public static final RegistryObject<Opinion> SOCIALITE = register("socialite", () -> new Opinion(-8, 12, ContextQuotients.SOCIAL.getId()));
	public static final RegistryObject<Opinion> ZOOLATER = register("zoolater", () -> new Opinion(-10, 4, ContextQuotients.ANIMALS.getId()));
	public static final RegistryObject<Opinion> ZOOPHOBE = register("zoophobe", () -> new Opinion(4, -10, ContextQuotients.ANIMALS.getId()));
	
	public static final RegistryObject<Opinion> DRUID = register("druid", () -> new Opinion(-8, 4, ContextQuotients.NATURE.getId()));
	public static final RegistryObject<Opinion> AGRIPHOBIC = register("agriphobic", () -> new Opinion(4, -8, ContextQuotients.NATURE.getId()));
	public static final RegistryObject<Opinion> AGORAPHOBIC = register("agoraphobic", () -> new Opinion(4, -3, ContextQuotients.AREA.getId()));
	public static final RegistryObject<Opinion> CLAUSTROPHOBIC = register("claustrophobic", () -> new Opinion(-3, 4, ContextQuotients.AREA.getId()));
	public static final RegistryObject<Opinion> MOLE = register("mole", () -> new Opinion(-8, 4, ContextQuotients.UNDERGROUND.getId()));
	public static final RegistryObject<Opinion> CLOUDWATCHER = register("cloudwatcher", () -> new Opinion(4, -8, ContextQuotients.UNDERGROUND.getId()));
	public static final RegistryObject<Opinion> SEAFARER = register("seafarer", () -> new Opinion(-8, 4, ContextQuotients.WATER.getId()));
	public static final RegistryObject<Opinion> LANDLOVER = register("landlover", () -> new Opinion(4, -8, ContextQuotients.WATER.getId()));
	public static final RegistryObject<Opinion> PYROMANIAC = register("pyromaniac", () -> new Opinion(-8, 4, ContextQuotients.FIRE.getId()));
	public static final RegistryObject<Opinion> MYCOLOGIST = register("mycologist", () -> new Opinion(-8, 4, ContextQuotients.FUNGUS.getId()));
	
	public static final RegistryObject<Opinion> VEGETARIAN = register("vegetarian", () -> new Opinion(-3, 6, ContextQuotients.EAT_VEG.getId()));
	public static final RegistryObject<Opinion> CARNIVORE = register("carnivore", () -> new Opinion(-3, 6, ContextQuotients.EAT_MEAT.getId()));
	public static final RegistryObject<Opinion> PESCETARIAN = register("pescetarian", () -> new Opinion(-3, 6, ContextQuotients.EAT_FISH.getId()));
	
	public static final RegistryObject<Opinion> SHADOW = register("shadow", () -> new Opinion(10, -5, ContextQuotients.LIGHT.getId()));
	public static final RegistryObject<Opinion> BRIGHT = register("bright", () -> new Opinion(-5, 10, ContextQuotients.LIGHT.getId()));
	
	public static final RegistryObject<Opinion> WANDERER = register("wanderer", () -> new Opinion(0, 16, ContextQuotients.TRAVEL.getId()));
	public static final RegistryObject<Opinion> HOMEBODY = register("homebody", () -> new Opinion(8, -12, ContextQuotients.TRAVEL.getId()));
	
	public static final RegistryObject<Opinion> INVENTOR = register("inventor", () -> new Opinion(-4, 10, ContextQuotients.CRAFTING.getId()));
	public static final RegistryObject<Opinion> BLACKSMITH = register("blacksmith", () -> new Opinion(-4, 10, ContextQuotients.SMELTING.getId()));
	// Alchemist
	public static final RegistryObject<Opinion> SCHOLAR = register("scholar", () -> new Opinion(0, 8, ContextQuotients.LEVEL.getId()));
	
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
