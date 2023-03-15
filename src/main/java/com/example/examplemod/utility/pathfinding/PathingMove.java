package com.example.examplemod.utility.pathfinding;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.utility.ExUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PathingMove
{
	protected static final double PLACE_COST = 2D;
	public static final EntityDimensions CROUCHING_PLAYER = EntityDimensions.fixed(0.6F, 1.5F);
	private static final List<PathingMove> MOVES = Lists.newArrayList();
	
	public static List<PathingMove> values() { return MOVES; }
	
	private final String name;
	
	protected PathingMove(String nameIn)
	{
		this.name = nameIn;
	}
	
	public String getName() { return this.name; }
	
	/** Returns true if this move can be taken from the given state */
	public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
	{
		return standardChecks(stateIn, stateIn.position(), stateIn.applyMove(this, world).position(), world);
	}
	
	/**
	 * Scans for collission or dangerous blocks in the path of the given move
	 */
	protected static boolean standardChecks(ComplexNodeState stateIn, BlockPos start, BlockPos end, Level world)
	{
		Vec3 startVec = ExUtils.posToVec(start);
		Vec3 endVec = ExUtils.posToVec(end);
		Vec3 dir = endVec.subtract(startVec);
		
		for(double i=0D; i<1D; i+=0.2D)
		{
			AABB bounds = CROUCHING_PLAYER.makeBoundingBox(startVec.add(dir.scale(i)));
			
			// First and foremost, ensure the move doesn't pass through solid terrain at any point
			if(!stateIn.noCollision(world, bounds))
				return false;
			
			BlockPos[] points = new BlockPos[]
					{
						new BlockPos(bounds.minX, bounds.minY, bounds.minZ),
						new BlockPos(bounds.maxX, bounds.minY, bounds.minZ),
						new BlockPos(bounds.minX, bounds.maxY, bounds.minZ),
						new BlockPos(bounds.maxX, bounds.maxY, bounds.minZ),
						new BlockPos(bounds.minX, bounds.minY, bounds.maxZ),
						new BlockPos(bounds.maxX, bounds.minY, bounds.maxZ),
						new BlockPos(bounds.minX, bounds.maxY, bounds.maxZ),
						new BlockPos(bounds.maxX, bounds.maxY, bounds.maxZ)
					};
			
			// Ensure the move does not cause damage by walking into lava or over cactus
			for(BlockPos point : points)
				if(world.getFluidState(point).is(FluidTags.LAVA) || world.getBlockState(point.below()).getBlock() == Blocks.CACTUS)
					return false;
		}
		return true;
	}
	
	public double length() { return 1D; }
	public double cost() { return 1D; }
	
	/** Returns the result of modifying the given state with this move */
	public ComplexNodeState apply(ComplexNodeState stateIn, @Nullable Level world)
	{
		return stateIn;
	}
	
	public void addToPath(List<BlockPos> path)
	{
		;
	}
	
	private static void addMove(PathingMove moveIn)
	{
		MOVES.add(moveIn);
		ExampleMod.LOG.info("# * Registered move: "+moveIn.getName());
	}
	
	/**
	 * Standard moves travel linearly, must start on solid ground, and cannot be used whilst falling
	 */
	private static class StandardMove extends PathingMove
	{
		protected final Direction[] directions;
		private final boolean placeAtEnd;
		
		public StandardMove(@Nonnull Direction dir1, @Nullable Direction dir2)
		{
			this(dir1, dir2, false);
		}
		
		public StandardMove(@Nonnull Direction dir1, @Nullable Direction dir2, boolean place)
		{
			super((place ? "place_" : "") + dir1.getSerializedName()+(dir2 == null ? "" : "_"+dir2.getSerializedName()));
			directions = dir2 == null ? new Direction[]{dir1} : new Direction[]{dir1, dir2};
			placeAtEnd = place;
		}
		
		public double cost() { return super.cost() + (placeAtEnd ? PLACE_COST : 0D); }
		
		public double length()
		{
			Vec3i offset = Vec3i.ZERO;
			for(Direction dir : directions)
				offset = offset.offset(dir.getNormal());
			return offset.distSqr(Vec3i.ZERO);
		}
		
		public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
		{
			Vec3 pos = ExUtils.posToVec(stateIn.position()).add(0D, -0.1D, 0D);
			ComplexNodeState stateEnd = stateIn.applyMove(this, world);
			return 
					!stateIn.falling() &&
					(!stateIn.noCollision(world, CROUCHING_PLAYER.makeBoundingBox(pos)) || world.getFluidState(stateIn.position()).is(FluidTags.WATER)) &&
					(placeAtEnd == false || stateIn.canPlaceBlockAt(world, stateEnd.position().below())) &&
					super.checkValid(stateIn, world);
		}
		
		public ComplexNodeState apply(ComplexNodeState stateIn, @Nullable Level world)
		{
			for(Direction dir : directions)
				stateIn.move(dir.getNormal());
			if(placeAtEnd)
				stateIn.placeBlock(stateIn.position().below());
			return stateIn;
		}
		
		public void addToPath(List<BlockPos> path)
		{
			BlockPos last = path.get(path.size() - 1);
			for(Direction dir : directions)
				path.add(last = last.offset(dir.getNormal()));
		}
	}
	
	/**
	 * Falling moves don't require solid ground at their destination like regular moves but cannot be taken if accrued damage is too high<br>
	 * They set the nodeState to be FALLING, preventing other non-falling moves from validating<br>
	 * They unset this value when they detect a landing point, eg solid ground, slime blocks, or water<br>
	 * Each falling move taken increments the nodeState's fall distance accordingly<br>
	 */
	private static class FallingMove extends PathingMove
	{
		private final Direction[] directions;
		
		public FallingMove(@Nullable Direction dir1, @Nullable Direction dir2)
		{
			super(dir1 == null && dir2 == null ? "fall_down" : "fall_"+dir1.getSerializedName()+(dir2 == null ? "" : "_"+dir2.getSerializedName()));
			directions = dir1 == null ? new Direction[] {Direction.DOWN} : dir2 == null ? new Direction[]{Direction.DOWN, dir1} : new Direction[]{Direction.DOWN, dir1, dir2};
		}
		
		public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
		{
			return stateIn.accruedFallDamage() < 20 && super.checkValid(stateIn, world);
		}
		
		public ComplexNodeState apply(ComplexNodeState stateIn, @Nullable Level world)
		{
			for(Direction dir : directions)
				stateIn.move(dir.getNormal());
			
			stateIn.fall(1);
			if(world != null)
			{
				if(!world.getFluidState(stateIn.position()).isEmpty() && world.getFluidState(stateIn.position()).is(FluidTags.WATER))
					stateIn.land(false);
				else if(!stateIn.noCollision(world, CROUCHING_PLAYER.makeBoundingBox(ExUtils.posToVec(stateIn.position()).add(0D, -0.1D, 0D))))
					stateIn.land(world.getBlockState(stateIn.position().below()).getBlock() == Blocks.SLIME_BLOCK);
			}
			return stateIn;
		}
		
		public void addToPath(List<BlockPos> path)
		{
			BlockPos last = path.get(path.size() - 1);
			for(Direction dir : directions)
				path.add(last = last.offset(dir.getNormal()));
		}
	}
	
	/**
	 * Step moves validate much like diagonal standard moves but skip most checks at the middle position
	 */
	private static class StepMove extends StandardMove
	{
		public StepMove(@Nonnull Direction step1, @Nonnull Direction step2)
		{
			super(step1, step2, false);
		}
		
		public double length()
		{
			double len = 0D;
			for(int i=0; i<directions.length; i++)
				len += directions[i].getNormal().distSqr(Vec3i.ZERO);
			return len;
		}
		
		public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
		{
			if(stateIn.falling())
				return false;
			
			BlockPos start = stateIn.position();
			BlockPos move1 = start.offset(directions[0].getNormal());
			BlockPos move2 = move1.offset(directions[1].getNormal());
			
			// Step 1
			Vec3 pos0 = ExUtils.posToVec(start).add(0D, -0.1D, 0D);
			if((stateIn.noCollision(world, CROUCHING_PLAYER.makeBoundingBox(pos0)) && !world.getFluidState(start).is(FluidTags.WATER)) || !standardChecks(stateIn, start, move1, world))
				return false;
			
			// Step 2
			Vec3 pos2 = ExUtils.posToVec(move2).add(0D, -0.1D, 0D);
			if((stateIn.noCollision(world, CROUCHING_PLAYER.makeBoundingBox(pos2)) && !world.getFluidState(move2).is(FluidTags.WATER)) || !standardChecks(stateIn, move1, move2, world))
				return false;
			
			return true;
		}
	}
	
	private static class ComplexMove extends PathingMove
	{
		private final Direction step;
		private final Pair<Direction, Direction> move;
		private final Direction[] moveArray;
		private final boolean stepFirst;
		
		public ComplexMove(String nameIn, Direction stepIn, Pair<Direction, Direction> moveIn)
		{
			this(nameIn, stepIn, moveIn, true);
		}
		
		public ComplexMove(String nameIn, Direction stepIn, Pair<Direction, Direction> moveIn, boolean stepFirstIn)
		{
			super(nameIn);
			step = stepIn;
			move = moveIn;
			stepFirst = stepFirstIn;
			moveArray = new Direction[]{step, move.getLeft(), move.getRight()};
		}
		
		public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
		{
			if(stateIn.falling())
				return false;
			
			BlockPos start = stateIn.position();
			BlockPos end;
			if(stepFirst)
			{
				BlockPos plusStep = start.offset(step.getNormal());
				BlockPos plusMove = plusStep.offset(move.getLeft().getNormal().offset(move.getRight().getNormal()));
				if(!standardChecks(stateIn, start, plusStep, world) || !standardChecks(stateIn, plusStep, plusMove, world))
					return false;
				
				end = plusMove;
			}
			else
			{
				BlockPos plusMove = start.offset(move.getLeft().getNormal().offset(move.getRight().getNormal()));
				BlockPos plusStep = plusMove.offset(step.getNormal());
				if(!standardChecks(stateIn, start, plusMove, world) || !standardChecks(stateIn, plusMove, plusStep, world))
					return false;
				
				end = plusStep;
			}
			
			Vec3 pos = ExUtils.posToVec(end).add(0D, -0.1D, 0D);
			return stateIn.noCollision(world, CROUCHING_PLAYER.makeBoundingBox(pos));
		}
		
		public ComplexNodeState apply(ComplexNodeState stateIn, @Nullable Level world)
		{
			for(Direction dir : moveArray)
				stateIn.move(dir.getNormal());
			
			return stateIn;
		}
		
		public void addToPath(List<BlockPos> path)
		{
			BlockPos last = path.get(path.size() - 1);
			
			if(stepFirst)
				path.add(last = last.offset(step.getNormal()));
			
			path.add(last = last.offset(move.getLeft().getNormal().offset(move.getRight().getNormal())));
			
			if(!stepFirst)
				path.add(last.offset(step.getNormal()));
		}
	}
	
	static
	{
		ExampleMod.LOG.info("# Initialising Hearth Light pathfinder moveset");
		
		// Falling straight down
		addMove(new FallingMove(null, null));
		
		List<Pair<Direction, Direction>> directions = Lists.newArrayList();
		for(Direction dir : Direction.Plane.HORIZONTAL)
			directions.add(Pair.of(dir, (Direction)null));
		directions.add(Pair.of(Direction.NORTH, Direction.WEST));
		directions.add(Pair.of(Direction.NORTH, Direction.EAST));
		directions.add(Pair.of(Direction.SOUTH, Direction.WEST));
		directions.add(Pair.of(Direction.SOUTH, Direction.EAST));
		
		for(Pair<Direction, Direction> direction : directions)
		{
			Direction main = direction.getKey();
			Direction off = direction.getValue();
			if(off == null)
			{
				addMove(new StepMove(Direction.UP, main));
				addMove(new StepMove(main, Direction.DOWN));
			}
			else
			{
				addMove(new ComplexMove(String.join("_", Direction.UP.getSerializedName(), main.getSerializedName(), off.getSerializedName()), Direction.UP, Pair.of(main, off)));
				addMove(new ComplexMove(String.join("_", main.getSerializedName(), off.getSerializedName(), Direction.DOWN.getSerializedName()), Direction.DOWN, Pair.of(main, off), false));
			}
			
			addMove(new StandardMove(main, off));
			addMove(new StandardMove(main, off, true));
			addMove(new FallingMove(main, off));
		}
		
		addMove(new PathingMove("climb_up")
		{
			public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
			{
				BlockPos pos = stateIn.position();
				return super.checkValid(stateIn, world) && (isClimbable(world.getBlockState(pos))|| world.getFluidState(pos).is(FluidTags.WATER));
			}
			
			private static boolean isClimbable(BlockState state)
			{
				Block block = state.getBlock();
				return 
						block == Blocks.LADDER ||
						block == Blocks.VINE ||
						block instanceof TrapDoorBlock;
			}
			
			public ComplexNodeState apply(ComplexNodeState stateIn, Level world)
			{
				stateIn.move(Direction.UP.getNormal());
				return stateIn;
			}
		});
		
		// Pillaring upwards
		/**
		 * Pillaring moves don't require solid ground at their destination like regular moves<br>
		 * Instead, they validate only at the end step and set a placed block in the nodeState<br>
		 * Placed blocks count as solid for future collision checks made from the nodeState<br>
		 */
		addMove(new PathingMove("pillar_up")
			{
				public double cost() { return PLACE_COST; }
				
				public boolean checkValid(ComplexNodeState stateIn, @Nonnull Level world)
				{
					return stateIn.canPlaceBlockAt(world, stateIn.position()) && super.checkValid(stateIn, world);
				}
				
				public ComplexNodeState apply(ComplexNodeState stateIn, Level world)
				{
					stateIn.placeBlock(stateIn.position());
					stateIn.move(Direction.UP.getNormal());
					return stateIn;
				}
			});
		
		ExampleMod.LOG.info("# "+MOVES.size()+" possible moves");
	}
}
