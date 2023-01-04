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
	
	public ExBlockTags(DataGenerator p_126511_, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(p_126511_, Reference.ModInfo.MOD_ID, existingFileHelper);
	}
	
	public String getName() { return "ExampleMod block tags"; }
	
	protected void addTags()
	{
		tag(NATURE)
			.add(Blocks.LILY_PAD)
			.add(Blocks.VINE)
			.addTag(BlockTags.LOGS)
			.addTag(BlockTags.LEAVES)
			.addTag(BlockTags.FLOWERS)
			.addTag(BlockTags.BEEHIVES)
			.addTag(BlockTags.CAVE_VINES)
			.addTag(BlockTags.DIRT)
			.addTag(BlockTags.CROPS);
	}
}
