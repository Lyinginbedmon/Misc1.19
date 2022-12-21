package com.example.examplemod.init;

import java.util.function.Supplier;

import com.example.examplemod.deities.Miracle;
import com.example.examplemod.deities.Miracles;
import com.example.examplemod.reference.Reference;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

public class ExRegisters
{
	public static final DeferredRegister<Miracle> CONDITIONS					= DeferredRegister.create(Miracle.REGISTRY_KEY, Reference.ModInfo.MOD_ID);
	public static final Supplier<IForgeRegistry<Miracle>> CONDITIONS_REGISTRY	= CONDITIONS.makeRegistry(RegistryBuilder::new);
	
	public static void registerCustom(IEventBus modEventBus)
	{
		CONDITIONS.register(modEventBus);
		
		Miracles.init();
	}
}
