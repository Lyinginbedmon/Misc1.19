package com.example.examplemod.utility;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class PathingSearch extends AbstractPathingSearch
{
	/** Record of path taken to each node evaluated */
	private Map<BlockPos, List<BlockPos>> nodeHistory = new HashMap<>();
	private BlockPos latestNode;
	
	/** Positions to be evaluated and their origins */
	private List<Pair<BlockPos, BlockPos>> nodesToEvaluate = Lists.newArrayList();
	
	public PathingSearch(BlockPos fromPos, BlockPos toPos)
	{
		super(fromPos, toPos);
		reset();
	}
	
	/** True if this search has reached an end state */
	public boolean finished() { return successful() || nodesToEvaluate.isEmpty(); }
	
	public void reset()
	{
		super.reset();
		
		nodeHistory.clear();
		nodesToEvaluate.clear();
		latestNode = position;
		markForEvaluation(position, position);
	}
	
	public int nodesSearched() { return this.nodeHistory.size(); }
	
	/**
	 * Retraces the nodeHistory map to identify the generated path
	 * @return
	 */
	public List<BlockPos> createPath()
	{
		if(success)
			return nodeHistory.get(destination);
		return Lists.newArrayList();
	}
	
	public void evaluateNextNode(Level world)
	{
		if(finished())
			return;
		
		if(nodesToEvaluate.size() > 1)
			nodesToEvaluate.sort(new Comparator<Pair<BlockPos,BlockPos>>() 
			{
				public int compare(Pair<BlockPos,BlockPos> o1, Pair<BlockPos,BlockPos> o2)
				{
					double dist1 = o1.getKey().distSqr(destination);
					double dist2 = o2.getKey().distSqr(destination);
					return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
				}
			});
		
		Pair<BlockPos, BlockPos> evaluating = nodesToEvaluate.remove(0);
		BlockPos node = evaluating.getKey();
		BlockPos origin = evaluating.getValue();
		
		success = evaluateNode(node, origin, world);
	}
	
	private boolean evaluateNode(BlockPos node, BlockPos from, Level world)
	{
		linkNodes(node, from);
		if(node.distSqr(destination) == 0D)
			return true;
		
		for(Moves move : Moves.values())
		{
			BlockPos offset = node.offset(move.offset());
			if(!hasBeenEvaluated(offset) && move.checkValid(node, world) && offset.distSqr(position) < HearthLightPathfinder.SEARCH_SQR)
				markForEvaluation(offset, node);
		}
		
		markEvaluated(node);
		return false;
	}
	
	private void markEvaluated(BlockPos node)
	{
		latestNode = node;
		nodesToEvaluate.removeIf((pair) -> pair.getKey().distSqr(node) == 0D);
	}
	
	private void markForEvaluation(BlockPos node, BlockPos origin)
	{
		nodesToEvaluate.add(Pair.of(node, origin));
	}
	
	private boolean hasBeenEvaluated(BlockPos position)
	{
		return nodeHistory.containsKey(position);
	}
	
	private void linkNodes(BlockPos node, BlockPos from)
	{
		List<BlockPos> history = Lists.newArrayList();
		history.addAll(nodeHistory.getOrDefault(from, Lists.newArrayList()));
		history.add(node);
		nodeHistory.put(node, history);
	}
	
	public List<BlockPos> getLatestPath() { return nodeHistory.getOrDefault(latestNode, Lists.newArrayList()); }
	public List<BlockPos> getEvaluatingList()
	{
		List<BlockPos> list = Lists.newArrayList();
		nodesToEvaluate.forEach((pair) -> list.add(pair.getKey()));
		return list;
	}
}