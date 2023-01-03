package com.example.examplemod.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

public class ExDataGenerators
{
	public static void onGatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
		generator.addProvider(event.includeServer(), new DeityProvider(generator, existingFileHelper));
		generator.addProvider(event.includeServer(), new MiracleTagProvider(generator, existingFileHelper));
		generator.addProvider(event.includeServer(), new ExItemTags(generator, existingFileHelper));
	}
}
