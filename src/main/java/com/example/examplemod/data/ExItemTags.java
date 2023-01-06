package com.example.examplemod.data;

import java.util.List;

import javax.annotation.Nullable;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExItemTags extends ItemTagsProvider
{
    public static final TagKey<Item> LEATHER_ARMOUR = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "leather_armour"));
    public static final TagKey<Item> METAL_ARMOUR = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "metal_armour"));
    public static final TagKey<Item> VEGETABLE = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "vegetable"));
    public static final TagKey<Item> MEAT = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "meat"));
    public static final TagKey<Item> TABOO = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "taboo_food"));
    
	public static final List<TagKey<Item>> DIET_TAGS = List.of(MEAT, ItemTags.FISHES, VEGETABLE);
    
	public ExItemTags(DataGenerator dataGenerator, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(dataGenerator, new ExBlockTags(dataGenerator, existingFileHelper), Reference.ModInfo.MOD_ID, existingFileHelper);
	}
	
	public String getName() { return "ExampleMod item tags"; }
	
	protected void addTags()
	{
		tag(LEATHER_ARMOUR).add(
				Items.LEATHER_BOOTS,
				Items.LEATHER_CHESTPLATE,
				Items.LEATHER_HELMET,
				Items.LEATHER_LEGGINGS,
				Items.TURTLE_HELMET);
		tag(METAL_ARMOUR).add(
				Items.CHAINMAIL_BOOTS,
				Items.CHAINMAIL_CHESTPLATE,
				Items.CHAINMAIL_HELMET,
				Items.CHAINMAIL_LEGGINGS,
				Items.IRON_BOOTS,
				Items.IRON_CHESTPLATE,
				Items.IRON_HELMET,
				Items.IRON_LEGGINGS,
				Items.GOLDEN_BOOTS,
				Items.GOLDEN_CHESTPLATE,
				Items.GOLDEN_HELMET,
				Items.GOLDEN_LEGGINGS);
		tag(MEAT).add(
				Items.PORKCHOP,
				Items.MUTTON,
				Items.BEEF,
				Items.CHICKEN,
				Items.RABBIT,
				Items.COOKED_PORKCHOP,
				Items.COOKED_MUTTON,
				Items.COOKED_BEEF,
				Items.COOKED_CHICKEN,
				Items.RABBIT,
				Items.RABBIT_STEW,
				Items.ROTTEN_FLESH);
		tag(VEGETABLE).add(
				Items.POTATO,
				Items.POISONOUS_POTATO,
				Items.BAKED_POTATO,
				Items.CARROT,
				Items.GOLDEN_CARROT,
				Items.BEETROOT,
				Items.BEETROOT_SOUP,
				Items.MUSHROOM_STEW,
				Items.APPLE,
				Items.GOLDEN_APPLE,
				Items.ENCHANTED_GOLDEN_APPLE,
				Items.PUMPKIN_PIE,
				Items.MELON_SLICE,
				Items.GLISTERING_MELON_SLICE,
				Items.CHORUS_FRUIT,
				Items.GLOW_BERRIES,
				Items.SWEET_BERRIES,
				Items.DRIED_KELP);
		tag(TABOO).add(Items.ROTTEN_FLESH, Items.SPIDER_EYE);
	}
}
