package com.example.examplemod.data;

import org.jetbrains.annotations.Nullable;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ExBlockTags extends BlockTagsProvider
{
    public static final TagKey<Block> NATURE = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "nature"));
    public static final TagKey<Block> FIRE = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "fire"));
    public static final TagKey<Block> MUSHROOM = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(Reference.ModInfo.MOD_ID, "mushroom"));
	
	public ExBlockTags(DataGenerator p_126511_, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(p_126511_, Reference.ModInfo.MOD_ID, existingFileHelper);
	}
	
	public String getName() { return "ExampleMod block tags"; }
	
	protected void addTags()
	{
		tag(NATURE)
			.add(Blocks.CACTUS)
			.add(Blocks.LILY_PAD)
			.add(Blocks.VINE)
			.add(Blocks.GLOW_LICHEN)
			.add(Blocks.MOSS_CARPET)
			.add(Blocks.SWEET_BERRY_BUSH)
			.add(Blocks.SUGAR_CANE)
			.add(Blocks.HANGING_ROOTS)
			.add(Blocks.BAMBOO, Blocks.BAMBOO_SAPLING)
			.add(Blocks.BIG_DRIPLEAF, Blocks.SMALL_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM)
			.add(Blocks.AZALEA, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA, Blocks.FLOWERING_AZALEA_LEAVES)
			.add(Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM)
			.add(Blocks.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK)
			.addTag(BlockTags.SAPLINGS)
			.addTag(BlockTags.LOGS)
			.addTag(BlockTags.LEAVES)
			.addTag(BlockTags.FLOWERS)
			.addTag(BlockTags.FLOWER_POTS)
			.addTag(BlockTags.BEEHIVES)
			.addTag(BlockTags.CAVE_VINES)
			.addTag(BlockTags.DIRT)
			.addTag(BlockTags.CROPS)
			.addTag(BlockTags.CORAL_PLANTS)
			.addTag(BlockTags.CORALS)
			.addTag(BlockTags.WALL_CORALS)
			.addTag(BlockTags.NYLIUM);
		tag(FIRE)
			.add(Blocks.TORCH, Blocks.REDSTONE_TORCH, Blocks.SOUL_TORCH, Blocks.WALL_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.SOUL_WALL_TORCH)
			.add(Blocks.LANTERN, Blocks.SOUL_LANTERN)
			.add(Blocks.JACK_O_LANTERN)
			.add(Blocks.MAGMA_BLOCK)
			.add(Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE)
			.addTag(BlockTags.CAMPFIRES)
			.addTag(BlockTags.FIRE);
		tag(MUSHROOM)
			.add(Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK)
			.add(Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_RED_MUSHROOM)
			.add(Blocks.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM)
			.add(Blocks.MYCELIUM)
			.add(Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN)
			.addTag(BlockTags.NYLIUM)
			.addTag(BlockTags.WART_BLOCKS);
	}
}
