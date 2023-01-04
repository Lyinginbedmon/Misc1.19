package com.example.examplemod.deities.miracle;

import java.util.function.Supplier;

import com.example.examplemod.init.ExRegistries;

import net.minecraftforge.registries.RegistryObject;

public class Miracles
{
	// Registry objects for different miracles
	public static final RegistryObject<Miracle> SAFE_LANDING = register("safe_landing", () -> new MiracleSafeLanding());
	
	private static RegistryObject<Miracle> register(String nameIn, Supplier<Miracle> miracleIn)
	{
		return ExRegistries.MIRACLES.register(nameIn, miracleIn);
	}
	
	public static void init() { }
}
