package com.example.examplemod.utility;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PathingSearchAdv extends AbstractPathingSearch
{
	private List<NodeState> nodesEvaluated = Lists.newArrayList();
	private List<NodeState> nodesToEvaluate = Lists.newArrayList();
	
	private NodeState latestNode;
	
	public PathingSearchAdv(BlockPos pos, BlockPos dest)
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
		nodesToEvaluate.add(latestNode = new NodeState(position, closestFace, destination));
	}
	
	public List<BlockPos> getLatestPath() { return latestNode.path(); }
	
	public List<BlockPos> getEvaluatingList()
	{
		List<BlockPos> evaluating = Lists.newArrayList();
		for(NodeState state : nodesToEvaluate)
			evaluating.add(state.pos());
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
		if(success)
			return latestNode.path();
		return Lists.newArrayList();
	}
	
	public void evaluateNextNode(Level world)
	{
		if(finished())
			return;
		
		if(nodesToEvaluate.size() > 1)
			nodesToEvaluate.sort(new Comparator<NodeState>() 
			{
				public int compare(NodeState o1, NodeState o2)
				{
					double dist1 = o1.distSqr();
					double dist2 = o2.distSqr();
					return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
				}
			});
		
		success = evaluateNode(nodesToEvaluate.remove(0), world);
	}
	
	private boolean evaluateNode(NodeState node, Level world)
	{
		latestNode = node;
		System.out.println("Evaluating "+latestNode.toString());
		if(node.distSqr() == 0D)
			return true;
		
		for(Moves move : Moves.values())
		{
			NodeState result = node.applyMove(move);
			if(!hasBeenEvaluated(result) && move.checkValid(node.pos(), world) && result.pos().distSqr(position) < HearthLightPathfinder.SEARCH_SQR)
				nodesToEvaluate.add(result);
		}
		
		nodesEvaluated.add(latestNode);
		return false;
	}
	
	public boolean hasBeenEvaluated(NodeState state)
	{
		for(NodeState oldState : nodesEvaluated)
			if(oldState.equals(state))
				return true;
		return false;
	}
	
	private class NodeState
	{
		private final BlockPos start;
		private final Face face;
		private final BlockPos dest;
		
		private BlockPos pos;
		private Face facing;
		private int fallDamage = 0;
		
		private List<Moves> moveHistory = Lists.newArrayList();
		
		public NodeState(BlockPos initialPos, Face initialFace, BlockPos destination)
		{
			start = initialPos;
			face = initialFace;
			dest = destination;
			
			pos = initialPos;
			facing = initialFace;
		}
		
		public String toString() { return "State:["+pos().toShortString()+"], "+facing().getSerializedName(); }
		
		public NodeState applyMove(Moves moveIn)
		{
			NodeState state = new NodeState(start, face, dest);
			moveHistory.forEach((move) -> state.addMove(move));
			state.addMove(moveIn);
			return state;
		}
		
		private void addMove(Moves moveIn)
		{
			moveHistory.add(moveIn);
			this.pos = this.pos.offset(moveIn.offset());
		}
		
		public double distSqr() { return pos().distSqr(destination); }
		
		public boolean equals(NodeState stateIn)
		{
			return stateIn.pos().distSqr(pos()) == 0D && stateIn.facing() == facing();
		}
		
		public Pair<BlockPos, Face> initialState() { return Pair.of(start, face); }
		
		public BlockPos pos() { return pos; }
		public Face facing() { return facing; }
		
		public List<BlockPos> path()
		{
			List<BlockPos> path = Lists.newArrayList();
			
			Pair<BlockPos, Face> state = initialState();
			for(Moves move : moveHistory)
			{
				path.add(state.getKey());
				state = Pair.of(state.getKey().offset(move.offset()), state.getValue());
			}
			
			return path;
		}
	}
	
	private static final EntityDimensions CROUCHING_PLAYER = EntityDimensions.fixed(0.6F, 1.5F);
	
	private enum Face implements StringRepresentable
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
