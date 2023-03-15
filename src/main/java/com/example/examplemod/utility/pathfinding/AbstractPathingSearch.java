package com.example.examplemod.utility.pathfinding;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public abstract class AbstractPathingSearch
{
	protected final BlockPos position;
	protected final BlockPos destination;
	
	private boolean success = false;
	
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
	
	public void reset() { success = false; }
	
	protected void setSuccess(boolean successIn) { this.success = successIn; }
}
