package com.example.examplemod.utility.pathfinding;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ComplexNodeState extends AbstractNodeState<ComplexNodeState>
{
	private int fallDistance = 0;
	private int totalFallDamage = 0;
	private List<BlockPos> placedBlocks = Lists.newArrayList();
	
	public List<PathingMove> moveHistory = Lists.newArrayList();
	
	public ComplexNodeState(BlockPos initialPos, BlockPos destination)
	{
		super(initialPos, destination);
	}
	
	public ComplexNodeState clone()
	{
		ComplexNodeState state = initialState();
		state.pos = this.pos;
		state.fallDistance = this.fallDistance;
		state.totalFallDamage = this.totalFallDamage;
		state.moveHistory.addAll(this.moveHistory);
		return state;
	}
	
	public String toString() { return "State:{["+pos.toShortString()+"], damage "+accruedFallDamage()+"}"; }
	
	/** Returns the total number of moves taken to reach this state */
	public int movesTaken()
	{
		return moveHistory.size();
	}
	
	/** Returns the linearity of the path taken by this node, where 1 = no consecutively repeated moves and 0 = only one kind of move ever taken */
	public double linearity()
	{
		if(moveHistory.isEmpty())
			return 1D;
		
		double tally = 0D;
		for(int i=1; i<moveHistory.size(); i++)
			if(moveHistory.get(i) != moveHistory.get(i - 1))
				tally++;
		return tally / movesTaken();
	}
	
	/** Returns the average weight value of all moves taken by this state */
	public double avgCost()
	{
		if(moveHistory.isEmpty())
			return 1D;
		
		double totalWeight = 0D;
		for(PathingMove move : moveHistory)
			totalWeight += move.cost();
		return totalWeight / moveHistory.size();
	}
	
	/**
	 * Applies the given pathing move to this state
	 * @param moveIn The move to apply to this state
	 * @param world The level this move is occuring in, or NULL if we're just applying it for path calculation
	 * @return A new modified state
	 */
	public ComplexNodeState applyMove(PathingMove moveIn, @Nullable Level world)
	{
		ComplexNodeState node = moveIn.apply(clone(), world);
		node.moveHistory.add(moveIn);
		return node;
	}
	
	public List<BlockPos> movesToPath()
	{
		List<BlockPos> path = Lists.newArrayList();
		path.add(start);
		for(PathingMove move : moveHistory)
			move.addToPath(path);
		
		return path;
	}
	
	public boolean equals(ComplexNodeState stateIn)
	{
		if(stateIn.placedBlocks.size() != this.placedBlocks.size())
			return false;
		else if(!this.placedBlocks.isEmpty())
			for(BlockPos pos : this.placedBlocks)
				if(!stateIn.placedBlocks.contains(pos))
					return false;
		return super.equals(stateIn) && stateIn.accruedFallDamage() == this.accruedFallDamage();
	}
	
	public boolean noCollision(Level world, AABB bounds)
	{
		if(!placedBlocks.isEmpty())
			for(BlockPos pos : placedBlocks)
				if(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).intersects(bounds))
					return false;
		return world.noCollision(bounds);
	}
	
	public void placeBlock(BlockPos pos) { this.placedBlocks.add(pos); }
	public boolean canPlaceBlockAt(@Nonnull Level world, BlockPos pos)
	{
		if(placedBlocks.contains(pos) || !(world.isEmptyBlock(pos) || world.getBlockState(pos).getMaterial().isReplaceable()))
			return false;
		
		for(Direction dir : Direction.values())
		{
			BlockPos offset = pos.offset(dir.getNormal());
			if(placedBlocks.contains(offset) || !world.getBlockState(offset).getShape(world, offset).isEmpty())
				return true;
		}
		
		return false;
	}
	
	public boolean falling() { return this.fallDistance > 0; }
	/** Increments fall distance */
	public ComplexNodeState fall(int dist) { this.fallDistance += dist; return this; }
	/** Adds fall damage accrued, resets fall distance */
	public ComplexNodeState land(boolean damage)
	{
		if(damage)
			this.totalFallDamage += Math.max(0, this.fallDistance - 3);
		this.fallDistance = 0;
		return this;
	}
	/** Total accrued amount of fall damage at this node */
	public int accruedFallDamage() { return this.totalFallDamage + Math.max(0, this.fallDistance - 3); }
	
	private ComplexNodeState initialState() { return new ComplexNodeState(start, dest); }
	
	public List<BlockPos> path()
	{
		List<BlockPos> path = Lists.newArrayList();
		
		ComplexNodeState state = initialState();
		for(PathingMove move : moveHistory)
		{
			path.add(state.position());
			state = state.applyMove(move, null);
		}
		
		return path;
	}
}