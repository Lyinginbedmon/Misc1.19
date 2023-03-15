package com.example.examplemod.init;

import com.example.examplemod.reference.Reference;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ExItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.ModInfo.MOD_ID);

    public static final RegistryObject<Item> REAPER_BAG_ITEM = ITEMS.register("reaper_bag", () -> new BlockItem(ExBlocks.REAPER_BAG.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> STONE_ALTAR_ITEM = ITEMS.register("stone_altar", () -> new BlockItem(ExBlocks.STONE_ALTAR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> HOURGLASS_ALTAR_ITEM = ITEMS.register("hourglass_altar", () -> new BlockItem(ExBlocks.HOURGLASS_ALTAR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> GOLDEN_ALTAR_ITEM = ITEMS.register("golden_altar", () -> new BlockItem(ExBlocks.GOLDEN_ALTAR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<Item> BONE_ALTAR_ITEM = ITEMS.register("bone_altar", () -> new BlockItem(ExBlocks.BONE_ALTAR.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    
	public static Item register(String nameIn, Item itemIn)
	{
		ITEMS.register(Reference.ModInfo.MOD_ID+"."+nameIn, () -> itemIn);
		return itemIn;
	}
}
