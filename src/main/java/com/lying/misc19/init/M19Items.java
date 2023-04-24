package com.lying.misc19.init;

import com.lying.misc19.item.ScrollItem;
import com.lying.misc19.reference.Reference;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class M19Items
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.ModInfo.MOD_ID);
    
    public static final RegistryObject<Item> MAGIC_SCROLL = ITEMS.register("magic_scroll", () -> new ScrollItem(new Item.Properties()));
    
	public static Item register(String nameIn, Item itemIn)
	{
		ITEMS.register(Reference.ModInfo.MOD_ID+"."+nameIn, () -> itemIn);
		return itemIn;
	}
}
