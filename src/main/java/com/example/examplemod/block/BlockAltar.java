package com.example.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockAltar extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock
{
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	public BlockAltar(Properties p_49795_)
	{
		super(p_49795_);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
	}
	
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState p_152844_)
	{
		return p_152844_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152844_);
	}
	
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		FluidState state = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, Boolean.valueOf(state.getType() == Fluids.WATER));
	}
	
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51385_)
	{
		p_51385_.add(FACING, WATERLOGGED);
	}
	
	/*
	 * Floral altar
	 * Wooden altar
	 * Redstone altar
	 * Bone altar with skull
	 * Golden altar
	 * Fountain altar
	 * Winged altar
	 * Crystal altar (amethyst)
	 * Mushroom altar
	 * Blazing altar
	 * Ender altar
	 * Tome altar
	 * Overgrown altar
	 * Blood altar
	 * Hourglass altar
	 */
	
	/**
	 * A simple stone altar with a candle
	 * @author Remem
	 */
	public static class Stone extends BlockAltar
	{
		protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 5.0D, 14.0D);
		
		public Stone(Properties p_49795_)
		{
			super(p_49795_.sound(SoundType.STONE));
		}
		
		public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) { return SHAPE; }
	}
}
