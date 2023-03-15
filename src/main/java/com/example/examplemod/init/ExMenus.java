package com.example.examplemod.init;

import com.example.examplemod.client.gui.menu.MenuAltar;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ExMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Reference.ModInfo.MOD_ID);
    
    public static final RegistryObject<MenuType<MenuAltar>> ALTAR_MENU = MENUS.register("altar", () -> new MenuType<MenuAltar>(MenuAltar::new));
    
	public static MenuType<?> register(String nameIn, MenuType<?> menuIn)
	{
		MENUS.register(Reference.ModInfo.MOD_ID+"."+nameIn, () -> menuIn);
		return menuIn;
	}
}
