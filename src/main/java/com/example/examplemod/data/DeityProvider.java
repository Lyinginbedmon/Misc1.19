package com.example.examplemod.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.DeityRegistry;
import com.example.examplemod.reference.Reference;
import com.google.common.collect.Sets;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;

public class DeityProvider implements DataProvider
{
	private final DataGenerator.PathProvider pathProvider;
	protected ExistingFileHelper fileHelper;
	
	public DeityProvider(DataGenerator generatorIn, ExistingFileHelper fileHelperIn)
	{
		this.pathProvider = generatorIn.createPathProvider(DataGenerator.Target.DATA_PACK, "deities");
		this.fileHelper = fileHelperIn;
	}
	
	public String getName(){ return "Example deities"; }
	
	public void run(CachedOutput cache) throws IOException
	{
		Set<String> set = Sets.newHashSet();
		Consumer<Deity> consumer = (god) ->
		{
			if(!set.add(god.simpleName()))
				throw new IllegalStateException("Duplicate deity "+god.simpleName());
			else
			{
				Path path = this.pathProvider.json(new ResourceLocation(Reference.ModInfo.MOD_ID, god.simpleName()));
				try
				{
					DataProvider.saveStable(cache, god.toJson(), path);
				}
				catch(IOException e)
				{
					ExampleMod.LOG.error("Couldn't save deity {}", path, e);
				}
			}
		};
		
		DeityRegistry.getDefaultDeities().forEach(consumer);
	}
}
