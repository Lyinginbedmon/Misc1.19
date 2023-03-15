package com.example.examplemod.utility.pathfinding;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public abstract class AbstractNodeState<T>
{
	protected final BlockPos start;
	protected final BlockPos dest;
	
	protected BlockPos pos;
	
	public AbstractNodeState(BlockPos initialPos, BlockPos destination)
	{
		start = initialPos;
		dest = destination;
		
		pos = initialPos;
	}
	
	public abstract T clone();
	
	public abstract String toString();
	
	/** Distance squared from this node to the original */
	public double distToStart() { return pos.distSqr(start); }
	/** Distance squared from this node to the destination */
	public double distToEnd() { return pos.distSqr(dest); }
	
	public double distToPath() { return start.distSqr(dest); }
	
	public BlockPos position() { return pos; }
	public abstract int movesTaken();
	
	public AbstractNodeState<T> move(Vec3i moveIn) { this.pos = this.pos.offset(moveIn); return this; }
	
	public abstract List<BlockPos> path();
	
	public boolean equals(AbstractNodeState<T> stateIn) { return stateIn.position().distSqr(position()) == 0D; }
}
