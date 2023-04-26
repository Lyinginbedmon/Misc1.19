package com.lying.misc19.init;

import com.lying.misc19.blocks.Sandbox;
import com.lying.misc19.reference.Reference;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class M19Blocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Reference.ModInfo.MOD_ID);
    
    public static final RegistryObject<Block> SANDBOX = BLOCKS.register("sandbox", () -> new Sandbox(BlockBehaviour.Properties.of(Material.WOOD).noOcclusion()));
    
    public static void init() { }
}
