package com.example.examplemod.data;

import org.jetbrains.annotations.Nullable;

import com.example.examplemod.deities.Miracle;
import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.init.MiracleTags;
import com.example.examplemod.reference.Reference;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeRegistryTagsProvider;

public class MiracleTagProvider extends ForgeRegistryTagsProvider<Miracle>
{
	
	public MiracleTagProvider(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(generator, ExRegistries.MIRACLES_REGISTRY.get(), Reference.ModInfo.MOD_ID, existingFileHelper);
	}
	
	protected void addTags()
	{
		getOrCreateRawBuilder(MiracleTags.AIR).build();
		getOrCreateRawBuilder(MiracleTags.ANIMAL).build();
		getOrCreateRawBuilder(MiracleTags.CHAOS).build();
		getOrCreateRawBuilder(MiracleTags.CREATION).build();
		getOrCreateRawBuilder(MiracleTags.DARKNESS).build();
		getOrCreateRawBuilder(MiracleTags.DEATH).build();
		getOrCreateRawBuilder(MiracleTags.DESTRUCTION).build();
		getOrCreateRawBuilder(MiracleTags.EARTH).build();
		getOrCreateRawBuilder(MiracleTags.EVIL).build();
		getOrCreateRawBuilder(MiracleTags.FIRE).build();
		getOrCreateRawBuilder(MiracleTags.GOOD).build();
		getOrCreateRawBuilder(MiracleTags.HEALING).build();
		getOrCreateRawBuilder(MiracleTags.KNOWLEDGE).build();
		getOrCreateRawBuilder(MiracleTags.LAW).build();
		getOrCreateRawBuilder(MiracleTags.LIGHT).build();
		getOrCreateRawBuilder(MiracleTags.LUCK).build();
		getOrCreateRawBuilder(MiracleTags.MAGIC).build();
		getOrCreateRawBuilder(MiracleTags.PLANT).build();
		getOrCreateRawBuilder(MiracleTags.PROTECTION).build();
		getOrCreateRawBuilder(MiracleTags.STRENGTH).build();
		getOrCreateRawBuilder(MiracleTags.TRAVEL).build();
		getOrCreateRawBuilder(MiracleTags.TRICKERY).build();
		getOrCreateRawBuilder(MiracleTags.WAR).build();
		getOrCreateRawBuilder(MiracleTags.WATER).build();
	}
}
