package com.example.examplemod.block;

import com.example.examplemod.init.ExItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockReaperBag extends Block implements SimpleWaterloggedBlock
{
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	public BlockReaperBag(Properties p_49795_)
	{
		super(p_49795_);
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}
	
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builderIn)
	{
		builderIn.add(WATERLOGGED);
	}
	
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState p_152844_)
	{
		return p_152844_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152844_);
	}
	
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		FluidState state = context.getLevel().getFluidState(context.getClickedPos());
		return super.getStateForPlacement(context).setValue(WATERLOGGED, Boolean.valueOf(state.getType() == Fluids.WATER));
	}
	
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
	{
		return context.isHoldingItem(ExItems.REAPER_BAG_ITEM.get()) ? Shapes.block() : Shapes.empty();
	}
	
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state1, Direction direction, BlockState state2, LevelAccessor level, BlockPos pos, BlockPos neighbour)
	{
		if(state1.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state1, direction, state2, level, pos, neighbour);
	}
	
	public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
	
	public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) { return 1F; }
	
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
	
	public boolean isRandomlyTicking(BlockState state) { return true; }
	
	@SuppressWarnings("deprecation")
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		if(level.isAreaLoaded(pos, 1) && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)).isEmpty())
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
	}
}
