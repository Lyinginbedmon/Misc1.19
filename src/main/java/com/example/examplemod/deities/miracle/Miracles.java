package com.example.examplemod.deities.miracle;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.miracle.MiracleBountiful.*;
import com.example.examplemod.init.ExRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryObject;

public class Miracles
{
	// Registry objects for different miracles
	public static final RegistryObject<Miracle> SAFE_LANDING = register("safe_landing", () -> new MiracleSafeLanding());
	public static final RegistryObject<Miracle> INDOMITABLE = register("indomitable", () -> new MiracleIndomitable());
	public static final RegistryObject<Miracle> JUGGERNAUT = register("juggernaut", () -> new MiracleJuggernaut());
	public static final RegistryObject<Miracle> REAPERS_BAG = register("reapers_bag", () -> new MiracleReapersBag());
	public static final RegistryObject<Miracle> LAST_ARROW = register("last_arrow", () -> new MiracleLastArrow());
	// Redirect - Projectile that hits you misses or strikes nearby hostile mob instead
	// Mining Contract - Pick unable to break until ore vein depleted (or moved away from)
	public static final RegistryObject<Miracle> CONTRACT_MINE = register("mining_contract", () -> new MiracleContractMine());
	// Warrior Contract - Sword/axe breaks only on the last hit against the boss
	public static final RegistryObject<Miracle> LIGHTNING = register("lightning", () -> new MiracleLightning());
	public static final RegistryObject<Miracle> ANIMAL_FRIEND = register("animal_friend", () -> new MiracleAnimalFriend());
	public static final RegistryObject<Miracle> BOUNTIFUL_HARVEST = register("bountiful_harvest", () -> new MiracleBountifulHarvest());
	public static final RegistryObject<Miracle> BOUNTIFUL_MINE = register("bountiful_mine", () -> new MiracleBountifulMine());
	// Fertile Soil - Bonemealing crops affects all similar crops within a 5x5x5 area
	// Full Stomach - Eating the last food item in a stack fills your entire hunger bar
	// Holy Sacrament - Eating/drinking a food item heals you
	// Curative Rest - Sleeping heals you
	// Holy Blessing - Praying heals you
	public static final RegistryObject<Miracle> STRONG_BREW = register("strong_brew", () -> new MiracleStrongBrew());
	// By God's Light - Damages hostile undead in an area around you
	// Deathguard - Spawns a group of temporary friendly zombies and skeletons to protect you
	// Momma Bear - Spawns a group of temporary friendly regional animals to protect you
	// Adder's Nest - Spawns a group of temporary friendly snakes to protect you
	// Hearth Light - Spawns a will o' wisp that paths towards your spawn point
	
	private static RegistryObject<Miracle> register(String nameIn, Supplier<Miracle> miracleIn)
	{
		return ExRegistries.MIRACLES.register(nameIn, miracleIn);
	}
	
	public static void init() { }
	
	public static void registerMiracleListeners()
	{
		ExampleMod.LOG.info("# Adding miracle listeners #");
		ExRegistries.MIRACLES.getEntries().forEach((entry) -> 
		{
			entry.get().addListeners(MinecraftForge.EVENT_BUS);
			ExampleMod.LOG.info("# * Added "+entry.getId());
		});
		ExampleMod.LOG.info("# "+ExRegistries.MIRACLES.getEntries().size()+" total miracles #");
	}
	
	@Nullable
	public static RegistryObject<Miracle> getRegistryName(Miracle miracleIn)
	{
		for(RegistryObject<Miracle> entry : ExRegistries.MIRACLES.getEntries())
			if(entry.isPresent() && entry.get().getClass().equals(miracleIn.getClass()))
				return entry;
		return null;
	}
	
	@Nullable
	public static RegistryObject<Miracle> getByName(ResourceLocation nameIn)
	{
		for(RegistryObject<Miracle> entry : ExRegistries.MIRACLES.getEntries())
			if(entry.isPresent() && entry.getId().equals(nameIn))
				return entry;
		return null;
	}
	
	public static List<String> getMiracleNames()
	{
		List<String> names = Lists.newArrayList();
		for(RegistryObject<Miracle> entry : ExRegistries.MIRACLES.getEntries())
			names.add(entry.getId().toString());
		return names;
	}
}
