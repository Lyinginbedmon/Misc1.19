package com.example.examplemod.init;

import com.example.examplemod.block.BlockAltar;
import com.example.examplemod.block.BlockReaperBag;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ExBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Reference.ModInfo.MOD_ID);
    
    public static final RegistryObject<Block> REAPER_BAG = BLOCKS.register("reaper_bag", () -> new BlockReaperBag(BlockBehaviour.Properties.of(Material.AIR).strength(-1.0F, 3600000.8F).noLootTable().noOcclusion()));
    public static final RegistryObject<Block> STONE_ALTAR = BLOCKS.register("stone_altar", () -> new BlockAltar.Stone(BlockBehaviour.Properties.of(Material.STONE).strength(2F).noOcclusion()));
    public static final RegistryObject<Block> HOURGLASS_ALTAR = BLOCKS.register("hourglass_altar", () -> new BlockAltar.Hourglass(BlockBehaviour.Properties.of(Material.METAL).strength(2F).noOcclusion()));
    public static final RegistryObject<Block> GOLDEN_ALTAR = BLOCKS.register("golden_altar", () -> new BlockAltar.Golden(BlockBehaviour.Properties.of(Material.METAL).strength(2F).noOcclusion()));
    public static final RegistryObject<Block> BONE_ALTAR = BLOCKS.register("bone_altar", () -> new BlockAltar.Bone(BlockBehaviour.Properties.of(Material.STONE).strength(2F).noOcclusion()));
    
    public static void init() { }
}
