package com.example.examplemod.utility.pathfinding;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.utility.pathfinding.SimplePathingSearch.PathingMoves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AdvPathingSearch extends AbstractPathingSearch
{
	private List<AdvNodeState> nodesEvaluated = Lists.newArrayList();
	private List<AdvNodeState> nodesToEvaluate = Lists.newArrayList();
	
	private AdvNodeState latestNode;
	
	public AdvPathingSearch(BlockPos pos, BlockPos dest)
	{
		super(pos, dest);
		reset();
	}
	
	public void reset()
	{
		super.reset();
		
		Vec3 offset = (new Vec3(destination.getX(), destination.getY(), destination.getZ())).subtract((new Vec3(position.getX(), position.getY(), position.getZ()))).normalize();
		Face closestFace = Face.NORTH;
		double minDist = new Vec3(closestFace.getNormal().getX(), closestFace.getNormal().getY(), closestFace.getNormal().getZ()).distanceToSqr(offset);
		for(Face face : Face.values())
		{
			double dist = new Vec3(face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ()).distanceToSqr(offset);
			if(dist < minDist)
			{
				closestFace = face;
				minDist = dist;
			}
		}
		
		nodesToEvaluate.clear();
		nodesToEvaluate.add(latestNode = new AdvNodeState(position, closestFace, destination));
	}
	
	public List<BlockPos> getLatestPath() { return latestNode.path(); }
	
	public List<BlockPos> getEvaluatingList()
	{
		List<BlockPos> evaluating = Lists.newArrayList();
		for(AdvNodeState state : nodesToEvaluate)
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
		if(successful())
			return latestNode.path();
		return Lists.newArrayList();
	}
	
	public void evaluateNextNode(Level world)
	{
		if(finished())
			return;
		
		if(nodesToEvaluate.size() > 1)
			nodesToEvaluate.sort(new Comparator<AdvNodeState>() 
			{
				public int compare(AdvNodeState o1, AdvNodeState o2)
				{
					double dist1 = o1.distToEnd();
					double dist2 = o2.distToEnd();
					return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
				}
			});
		
		setSuccess(evaluateNode(nodesToEvaluate.remove(0), world));
	}
	
	private boolean evaluateNode(AdvNodeState node, Level world)
	{
		latestNode = node;
		if(node.distToEnd() == 0D)
			return true;
		
		for(PathingMoves move : PathingMoves.values())
		{
			AdvNodeState result = node.applyMove(move);
			if(!hasBeenEvaluated(result) && move.checkValid(node.position(), world) && result.distToStart() < HearthLightPathfinder.SEARCH_SQR)
				nodesToEvaluate.add(result);
		}
		
		nodesEvaluated.add(latestNode);
		return false;
	}
	
	public boolean hasBeenEvaluated(AdvNodeState state)
	{
		for(AdvNodeState oldState : nodesEvaluated)
			if(oldState.equals(state))
				return true;
		return false;
	}
	
	enum Face implements StringRepresentable
	{
		NORTH(Direction.NORTH),
		NORTHEAST(Direction.NORTH, Direction.EAST),
		EAST(Direction.EAST),
		SOUTHEAST(Direction.NORTH, Direction.EAST),
		SOUTH(Direction.SOUTH),
		SOUTHWEST(Direction.NORTH, Direction.WEST),
		WEST(Direction.WEST),
		NORTHWEST(Direction.NORTH, Direction.WEST);
		
		private final Direction[] directions;
		
		private Face(Direction... dirIn)
		{
			directions = dirIn;
		}
		
		public Face getClockWise() { return values()[(ordinal() + 1)%values().length]; }
		public Face getCounterClockWise()
		{
			int ord = ordinal() - 1;
			if(ord < 0)
				return NORTHWEST;
			else
				return values()[ord];
		}
		
		public Vec3i getNormal()
		{
			Vec3i normal = Vec3i.ZERO;
			for(Direction dir : directions)
				normal = normal.offset(dir.getNormal());
			return normal;
		}
		
		public String getSerializedName() { return name().toLowerCase(); }
	}
}
