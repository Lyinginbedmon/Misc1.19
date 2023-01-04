package com.example.examplemod.data;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ExEntityTags extends EntityTypeTagsProvider
{
	public static final TagKey<EntityType<?>> ANIMAL = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "animal"));
    public static final TagKey<EntityType<?>> FISH = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "fish"));
	public static final TagKey<EntityType<?>> PEOPLE = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "people"));
    public static final TagKey<EntityType<?>> SEA_CREATURE = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "sea_creature"));
	
	public ExEntityTags(DataGenerator p_126517_, ExistingFileHelper helperIn)
	{
		super(p_126517_, Reference.ModInfo.MOD_ID, helperIn);
	}
	
	public String getName() { return "ExampleMod entity tags"; }
	
	protected void addTags()
	{
		tag(FISH)
			.add(EntityType.COD)
			.add(EntityType.PUFFERFISH)
			.add(EntityType.SALMON)
			.add(EntityType.TROPICAL_FISH);
		tag(SEA_CREATURE)
			.add(EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN)
			.add(EntityType.SQUID, EntityType.GLOW_SQUID)
			.add(EntityType.DOLPHIN)
			.add(EntityType.TURTLE)
			.addTag(FISH);
		tag(ANIMAL)
			.add(EntityType.AXOLOTL)
			.add(EntityType.BAT)
			.add(EntityType.BEE)
			.add(EntityType.CAT, EntityType.OCELOT)
			.add(EntityType.CHICKEN)
			.add(EntityType.COW, EntityType.MOOSHROOM)
			.add(EntityType.DOLPHIN)
			.add(EntityType.DONKEY, EntityType.HORSE, EntityType.MULE)
			.add(EntityType.FOX)
			.add(EntityType.FROG, EntityType.TADPOLE)
			.add(EntityType.GOAT)
			.add(EntityType.HOGLIN, EntityType.STRIDER)
			.add(EntityType.LLAMA, EntityType.TRADER_LLAMA)
			.add(EntityType.PANDA)
			.add(EntityType.PARROT)
			.add(EntityType.PIG)
			.add(EntityType.POLAR_BEAR)
			.add(EntityType.RABBIT)
			.add(EntityType.RAVAGER)
			.add(EntityType.SHEEP)
			.add(EntityType.SQUID, EntityType.GLOW_SQUID)
			.add(EntityType.TURTLE)
			.add(EntityType.WOLF)
			.addTag(FISH);
		tag(PEOPLE)
			.add(EntityType.EVOKER, EntityType.ILLUSIONER)
			.add(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE)
			.add(EntityType.PILLAGER, EntityType.VINDICATOR)
			.add(EntityType.PLAYER)
			.add(EntityType.VILLAGER)
			.add(EntityType.WITCH);
	}
}
