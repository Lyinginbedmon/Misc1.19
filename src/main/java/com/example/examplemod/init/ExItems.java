package com.example.examplemod.init;

import com.example.examplemod.ExampleMod;
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
    
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(ExampleMod.EXAMPLE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    
	public static Item register(String nameIn, Item itemIn)
	{
		ITEMS.register("examplemod."+nameIn, () -> itemIn);
		return itemIn;
	}
}
