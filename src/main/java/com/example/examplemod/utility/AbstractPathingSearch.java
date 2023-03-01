package com.example.examplemod.utility;

import java.util.List;
import java.util.function.BiPredicate;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractPathingSearch
{
	protected final BlockPos position;
	protected final BlockPos destination;
	
	protected boolean success = false;
	
	protected AbstractPathingSearch(BlockPos pos, BlockPos dest)
	{
		this.position = pos;
		this.destination = dest;
	}
	
	public abstract List<BlockPos> getLatestPath();
	
	public abstract List<BlockPos> getEvaluatingList();
	
	public abstract boolean finished();
	
	/** True if this search evaluated the destination node */
	public final boolean successful() { return this.success; }
	
	public void evaluateNextNode(Level world) { }
	
	public List<BlockPos> createPath() { return Lists.newArrayList(); }
	
	public abstract int nodesSearched();
	
	public void reset()
	{
		success = false;
	}
	
	protected enum Moves
	{
		FALL(0, -1, 0),
		PILLAR(0, 1, 0),
		NORTH(Direction.NORTH),
		SOUTH(Direction.SOUTH),
		EAST(Direction.EAST),
		WEST(Direction.WEST),
		NORTHEAST(Direction.NORTH, Direction.EAST),
		NORTHWEST(Direction.NORTH, Direction.WEST),
		SOUTHEAST(Direction.SOUTH, Direction.EAST),
		SOUTHWEST(Direction.SOUTH, Direction.WEST)
		;
		
		private static final EntityDimensions CROUCHING_PLAYER = EntityDimensions.fixed(0.6F, 1.5F);
		
		private final Vec3i offset;
		private final BiPredicate<BlockPos,Level> isValidAt;
		
		private Moves(Direction... dir) { this(makeCompound(dir)); }
		private Moves(Vec3i vec) { this(vec.getX(), vec.getY(), vec.getZ(), solidGround(vec.getX(), vec.getY(), vec.getZ())); }
		private Moves(int x, int y, int z)
		{
			this(x, y, z, basicPredicate(x, y, z));
		}
		
		private Moves(int x, int y, int z, BiPredicate<BlockPos,Level> validAt)
		{
			this.offset = new Vec3i(x, y, z);
			this.isValidAt = validAt;
		}
		
		private static Vec3i makeCompound(Direction... directions)
		{
			Vec3i vec = Vec3i.ZERO;
			for(int i=0; i<directions.length; i++)
				vec = vec.offset(directions[i].getNormal());
			return vec;
		}
		
		public boolean checkValid(BlockPos pos, Level world) { return this.isValidAt.test(pos, world); }
		
		public Vec3i offset() { return this.offset; }
		
		private static BiPredicate<BlockPos, Level> solidGround(int x, int y, int z)
		{
			return basicPredicate(x,y,z).and((pos,world) -> Block.canSupportCenter(world, pos.offset(x,y,z).below(), Direction.UP));
		}
		
		private static BiPredicate<BlockPos, Level> basicPredicate(int x, int y, int z)
		{
			return (pos,world) -> 
			{
				Vec3 position = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				Vec3 offset = position.add(x, y, z);
				Vec3 mid = position.add(new Vec3(x, y, z).scale(0.5D));
				return
						world.noCollision(CROUCHING_PLAYER.makeBoundingBox(position)) &&
						world.noCollision(CROUCHING_PLAYER.makeBoundingBox(offset)) &&
						world.noCollision(CROUCHING_PLAYER.makeBoundingBox(mid));
			};
		}
	}
}
