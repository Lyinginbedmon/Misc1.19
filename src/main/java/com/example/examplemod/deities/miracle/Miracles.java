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
	public static final RegistryObject<Miracle> JUGGERNAUT = register("juggernaut", () -> new MiracleIndomitable());
	// Reaper's Bag - Inventory preserved in safe position on death
	// Last Arrow - Bow/crossbow gains 1 free arrow when loaded whilst empty
	// Redirect - Projectile that hits you misses or strikes nearby hostile mob instead
	// Mining Contract - Pick unable to break until ore vein depleted (or moved away from)
	// Warrior Contract - Sword/axe breaks only on the last hit against the boss
	
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
