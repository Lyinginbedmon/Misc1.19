package com.example.examplemod.utility.pathfinding;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ComplexPathingSearch extends AbstractPathingSearch
{
	private static final double WEIGHT_SPEED = 4D;
	private static final double WEIGHT_SAFETY = 2D;
	private static final double WEIGHT_EFFICIENCY = 3D;
	private static final Comparator<ComplexNodeState> NODE_SORT = (o1,o2) ->
		{
			double score1 = (o1.distToEnd() / o1.distToPath() * WEIGHT_SPEED) + (o1.avgCost() * WEIGHT_SAFETY) + (o1.linearity() * WEIGHT_EFFICIENCY);
			double score2 = (o2.distToEnd() / o2.distToPath() * WEIGHT_SPEED) + (o2.avgCost() * WEIGHT_SAFETY) + (o2.linearity() * WEIGHT_EFFICIENCY);
			return  score1 < score2 ? -1 : score1 > score2 ? 1 : 0;
		};
	private List<ComplexNodeState> nodesEvaluated = Lists.newArrayList();
	private List<ComplexNodeState> nodesToEvaluate = Lists.newArrayList();
	
	private ComplexNodeState latestNode;
	
	public ComplexPathingSearch(BlockPos pos, BlockPos dest)
	{
		super(pos, dest);
		reset();
	}
	
	public void reset()
	{
		super.reset();
		nodesToEvaluate.clear();
		nodesToEvaluate.add(latestNode = new ComplexNodeState(position, destination));
	}
	
	public List<BlockPos> getLatestPath() { return latestNode.path(); }
	
	public List<BlockPos> getEvaluatingList()
	{
		List<BlockPos> evaluating = Lists.newArrayList();
		for(ComplexNodeState state : nodesToEvaluate)
			evaluating.add(state.position());
		return evaluating;
	}
	
	public boolean finished() { return successful() || nodesToEvaluate.isEmpty(); }
	
	public int nodesSearched() { return nodesEvaluated.size(); }
	
	/**
	 * Retraces the nodeHistory map to identify the generated path
	 * @return
	 */
	public List<BlockPos> createPath()
	{
//		ExampleMod.LOG.info("Path generated:");
//		latestNode.moveHistory.forEach((move) -> ExampleMod.LOG.info(" # "+move.getName()));
		return successful() ? latestNode.movesToPath() : Lists.newArrayList();
	}
	
	public void evaluateNextNode(Level world)
	{
		if(finished())
			return;
		
		if(nodesToEvaluate.size() > 1)
			nodesToEvaluate.sort(NODE_SORT);
		
		setSuccess(evaluateNode(nodesToEvaluate.remove(0), world));
	}
	
	/** Evaluates the given node and returns true if it has reached the destination */
	private boolean evaluateNode(ComplexNodeState node, Level world)
	{
		latestNode = node;
		if(node.distToEnd() == 0D)
			return true;
		
		for(PathingMove move : PathingMove.values())
		{
			ComplexNodeState result = node.applyMove(move, world);
			if(!hasBeenEvaluated(result) && move.checkValid(node, world) && result.distToStart() < HearthLightPathfinder.SEARCH_SQR)
				nodesToEvaluate.add(result);
		}
		
		nodesEvaluated.add(latestNode);
		nodesToEvaluate.removeIf((nodeIn) -> nodeIn.equals(node));
		return false;
	}
	
	private boolean hasBeenEvaluated(ComplexNodeState state)
	{
		for(ComplexNodeState oldState : nodesEvaluated)
			if(oldState.equals(state))
				return true;
		return false;
	}
}
