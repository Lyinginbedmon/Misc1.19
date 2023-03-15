package com.example.examplemod.utility.pathfinding;

import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import com.example.examplemod.utility.pathfinding.AdvPathingSearch.Face;
import com.example.examplemod.utility.pathfinding.SimplePathingSearch.PathingMoves;

import net.minecraft.core.BlockPos;

public class AdvNodeState extends AbstractNodeState<AdvNodeState>
{
	private final Face face;
	
	private Face facing;
	private int fallDamage = 0;
	
	private List<PathingMoves> moveHistory = Lists.newArrayList();
	
	public AdvNodeState(BlockPos initialPos, Face initialFace, BlockPos destination)
	{
		super(initialPos, destination);
		face = initialFace;
		facing = initialFace;
	}
	
	public AdvNodeState clone()
	{
		AdvNodeState state = new AdvNodeState(start, face, dest);
		state.pos = this.pos;
		state.facing = this.facing;
		state.fallDamage = this.fallDamage;
		state.moveHistory.addAll(this.moveHistory);
		return state;
	}
	
	public String toString() { return "State:{["+position().toShortString()+"], facing "+facing().getSerializedName()+"}"; }
	
	public int movesTaken() { return moveHistory.size(); }
	
	public AdvNodeState applyMove(PathingMoves moveIn) { return clone().addMove(moveIn); }
	
	private AdvNodeState addMove(PathingMoves moveIn)
	{
		moveHistory.add(moveIn);
		this.pos = this.pos.offset(moveIn.offset());
		return this;
	}
	
	public boolean equals(AdvNodeState stateIn)
	{
		return super.equals(stateIn) && stateIn.facing() == facing();
	}
	
	public Pair<BlockPos, Face> initialState() { return Pair.of(start, face); }
	public Face facing() { return facing; }
	
	public List<BlockPos> path()
	{
		List<BlockPos> path = Lists.newArrayList();
		
		Pair<BlockPos, Face> state = initialState();
		for(PathingMoves move : moveHistory)
		{
			path.add(state.getKey());
			state = Pair.of(state.getKey().offset(move.offset()), state.getValue());
		}
		
		return path;
	}
}