package com.example.examplemod.deities.miracle;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.init.ExRegistries;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryObject;

public class Miracles
{
	// Registry objects for different miracles
	public static final RegistryObject<Miracle> SAFE_LANDING = register("safe_landing", () -> new MiracleSafeLanding());
	public static final RegistryObject<Miracle> INDOMITABLE = register("indomitable", () -> new MiracleIndomitable());
	public static final RegistryObject<Miracle> JUGGERNAUT = register("juggernaut", () -> new MiracleJuggernaut());
	// Reaper's Bag - Inventory preserved in safe position on death
	// Last Arrow - Bow/crossbow gains 1 free arrow when loaded whilst empty
	// Redirect - Projectile that hits you misses or strikes nearby hostile mob instead
	// Mining Contract - Pick unable to break until ore vein depleted (or moved away from)
	// Warrior Contract - Sword/axe breaks only on the last hit against the boss
	// Contract of Retribution - A temporary lightning storm starts then strikes a hostile mob near the player (unless they leave the region)
	// Animal Friend - Tame a pet on the first attempt
	// Bountiful Harvest - Harvesting crops treated as Fortune V
	// Bountiful Mine - Breaking ores treated as Fortune V
	// Fertile Soil - Bonemealing crops affects all similar crops within a 5x5x5 area
	// Full Stomach - Eating the last food item in a stack fills your entire hunger bar
	// Holy Sacrament - Eating/drinking a food item heals you
	// Curative Rest - Sleeping heals you
	// Holy Blessing - Praying heals you
	// Strong Brew - A beneficial non-ambient potion effect that would expire instead lasts another 15 seconds
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
			ExampleMod.LOG.info("# Added "+entry.getId());
		});
	}
	
	@Nullable
	public static RegistryObject<Miracle> getRegistryName(Miracle miracleIn)
	{
		for(RegistryObject<Miracle> entry : ExRegistries.MIRACLES.getEntries())
			if(entry.isPresent() && entry.get().getClass().equals(miracleIn.getClass()))
				return entry;
		return null;
	}
}
