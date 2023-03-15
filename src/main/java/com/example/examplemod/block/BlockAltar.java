package com.example.examplemod.block;

import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.example.examplemod.client.gui.menu.MenuAltar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;

public abstract class BlockAltar extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, MenuProvider
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
	
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		if(!world.isClientSide() && canPrayAt(state, world, player))
			player.openMenu(this);
		
		return InteractionResult.FAIL;
	}
	
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51385_)
	{
		p_51385_.add(FACING, WATERLOGGED);
	}
	
	protected static void addParticlesAndSound(Level world, Vec3 pos, RandomSource rand)
	{
		float f = rand.nextFloat();
		if(f < 0.3F)
		{
			world.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
			if (f < 0.17F)
				world.playLocalSound(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F, false);
		}
		
		world.addParticle(ParticleTypes.SMALL_FLAME, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
	}
	
	/** Whether the given player can pray at this particular altar */
	public boolean canPrayAt(BlockState state, Level world, Player player) { return true; }
	
	public Component getDisplayName() { return Component.translatable("container.examplemod.altar"); }
	
	public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player)
	{
		double range = (double)player.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
		return new MenuAltar(containerId, player.pick(range, 0F, false));
	}
	
	/*
	 * Floral altar
	 * Wooden altar
	 * Redstone altar
	 * Fountain altar
	 * Winged altar
	 * Crystal altar (amethyst)
	 * Mushroom altar
	 * Blazing altar
	 * Ender altar
	 * Tome altar
	 * Overgrown altar
	 * Blood altar
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
	
	/**
	 * A golden hourglass
	 * @author Remem
	 */

	public static class Hourglass extends BlockAltar
	{
		protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 13.0D, 12.0D);
		
		public Hourglass(Properties p_49795_)
		{
			super(p_49795_.sound(SoundType.GLASS));
		}
		
		public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) { return SHAPE; }
	}
	
	/**
	 * A golden tray with a quartz stand
	 * @author Remem
	 */
	
	public static class Golden extends BlockAltar
	{
		protected static final VoxelShape SHAPE_Z = Block.box(1.5D, 0.0D, 3.0D, 14.5D, 3.5D, 13.0D);
		protected static final VoxelShape SHAPE_X = Block.box(3.0D, 0.0D, 1.5D, 13.0D, 3.5D, 14.5D);
		
		public Golden(Properties p_49795_)
		{
			super(p_49795_.sound(SoundType.METAL));
		}
		
		public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_)
		{
			return p_51309_.getValue(FACING).getAxis() == Axis.X ? SHAPE_X : SHAPE_Z;
		}
	}
	
	/**
	 * A skull and adjacent candle
	 * @author Remem
	 */
	public static class Bone extends BlockAltar
	{
		public static final BooleanProperty LIT = BlockStateProperties.LIT;
		private static final Vec3[] candleOffsets = new Vec3[]
				{
					new Vec3(14, 6, 9).scale(1/ 16D),
					new Vec3(7, 6, 14).scale(1 / 16D),
					new Vec3(2, 6, 7).scale(1 / 16D),
					new Vec3(9, 6, 2).scale(1 / 16D)
				};
		public static ToIntFunction<BlockState> LIGHT_EMISSION = (p_152848_) -> {
				return p_152848_.getValue(LIT) ? 0 : 6;
			};
		private static final VoxelShape[] SHAPES = new VoxelShape[]
				{
					Block.box(0D, 0D, 0D, 12, 8.3D, 11.5D),
					Block.box(4.5D, 0D, 0D, 16, 8.3D, 12D),
					Block.box(4D, 0D, 4.5D, 16, 8.3D, 16D),
					Block.box(0D, 0D, 4D, 11.5D, 8.3D, 16D)
				};
		
		public Bone(Properties p_49795_)
		{
			super(p_49795_.sound(SoundType.BONE_BLOCK).lightLevel(LIGHT_EMISSION));
		}
		
		public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand)
		{
			if (state.getValue(LIT))
			{
				Vec3 partPos = candleOffsets[state.getValue(FACING).get2DDataValue()];
				addParticlesAndSound(world, partPos.add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), rand);
			}
		}
		
		public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_)
		{
			return SHAPES[p_51309_.getValue(FACING).get2DDataValue()];
		}
		
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51385_)
		{
			p_51385_.add(FACING, WATERLOGGED, LIT);
		}
		
		public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
		{
			ItemStack heldStack = player.getItemInHand(hand);
			if(canBeLit(state) && (heldStack.getItem() == Items.FLINT_AND_STEEL || heldStack.getItem() == Items.FIRE_CHARGE))
			{
				if(!world.isClientSide)
				{
					setLit(world, state, pos, true);
					world.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);
				}
				player.getItemInHand(hand).hurtAndBreak(1, player, (entity) -> entity.broadcastBreakEvent(hand));
				return InteractionResult.CONSUME;
			}
			return super.use(state, world, pos, player, hand, hitResult);
		}
		
		private static boolean canBeLit(BlockState state)
		{
			return !state.getValue(LIT) && !state.getValue(WATERLOGGED);
		}
		
		public boolean canPrayAt(BlockState state, Level world, Player player) { return state.getValue(LIT); }
		
		public void onProjectileHit(Level world, BlockState state, BlockHitResult hitResult, Projectile projectile)
		{
			if(!world.isClientSide && projectile.isOnFire() && canBeLit(state))
				setLit(world, state, hitResult.getBlockPos(), true);
		}
		
		private static void extinguish(@Nullable Player player, BlockState state, LevelAccessor world, BlockPos pos)
		{
			setLit(world, state, pos, false);
			Vec3 partPos = candleOffsets[state.getValue(FACING).get2DDataValue()];
			world.addParticle(ParticleTypes.SMOKE, (double)pos.getX() + partPos.x(), (double)pos.getY() + partPos.y(), (double)pos.getZ() + partPos.z(), 0.0D, (double)0.1F, 0.0D);
			world.playSound((Player)null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		
		private static void setLit(LevelAccessor world, BlockState state, BlockPos pos, boolean isLit)
		{
			world.setBlock(pos, state.setValue(LIT, Boolean.valueOf(isLit)), 11);
		}
		
		public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluid)
		{
			if(!state.getValue(WATERLOGGED) && fluid.getType() == Fluids.WATER)
			{
				BlockState blockstate = state.setValue(WATERLOGGED, Boolean.valueOf(true));
				if(state.getValue(LIT))
					extinguish((Player)null, blockstate, world, pos);
				else
					world.setBlock(pos, blockstate, 3);
				
				world.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(world));
				return true;
			}
			return false;
		}
	}
}
