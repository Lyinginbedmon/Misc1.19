package com.example.examplemod.utility;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import com.example.examplemod.ExampleMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class PathingSearch
	{
		private final BlockPos position;
		private final BlockPos destination;
		
		/** Positions to their most recently-evaluated neighbour */
		public Map<BlockPos, BlockPos> nodeGraph = new HashMap<>();
		
		/** Positions to be evaluated and their origins */
		public List<Pair<BlockPos, BlockPos>> nodesToEvaluate = Lists.newArrayList();
		
		private List<BlockPos> nodesEvaluated = Lists.newArrayList();
		
		private boolean success = false;
		
		public PathingSearch(BlockPos fromPos, BlockPos toPos)
		{
			position = fromPos;
			destination = toPos;
			reset();
		}
		
		/** True if this search has reached an end state */
		public boolean finished() { return success || nodesToEvaluate.isEmpty(); }
		
		/** True if this search evaluated the destination node */
		public boolean successful() { return success; }
		
		public void reset()
		{
			success = false;
			nodeGraph.clear();
			nodesToEvaluate.clear();
			nodesEvaluated.clear();
			markForEvaluation(position, position);
		}
		
		public int nodesSearched() { return this.nodesEvaluated.size(); }
		
		/**
		 * Retraces the nodeHistory map to identify the generated path
		 * @return
		 */
		public List<BlockPos> createPath()
		{
			if(success)
			{
				List<BlockPos> path = Lists.newArrayList();
				if(!historyTest())
					return path;
				
				path.add(position);
				BlockPos node = position;
				while(node != destination && path.size() < position.distSqr(destination))
				{
					node = nodeGraph.getOrDefault(node, null);
					if(node == null)
						break;
					
					if(!path.contains(node))
						path.add(node);
				}
				
				return path;
			}
			return Lists.newArrayList();
		}
		
		public boolean historyTest()
		{
			if(finished())
			{
				BlockPos pos = position;
				while(pos.distSqr(destination) > 0D)
				{
					BlockPos next = nodeGraph.getOrDefault(pos, null);
					if(next == null)
					{
						ExampleMod.LOG.warn("Hearth Light pathfinding search failed a history test. Cause: NULL node from "+pos.toShortString());
						return false;
					}
					pos = next;
				}
				
				if(pos.distSqr(destination) == 0D)
					return true;
				
				ExampleMod.LOG.warn("Hearth Light pathfinding search failed a history test. Cause: did not reach destination");
				return false;
			}
			ExampleMod.LOG.warn("Hearth Light pathfinding search failed a history test. Cause: search unfinished");
			return false;
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
			System.out.println("# Evaluating "+node.toShortString()+" dist "+(int)node.distSqr(destination));
			nodeGraph.put(from, node);
			if(node.equals(destination))
				return true;
			
			for(PathingSearch.Moves move : Moves.values())
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
			nodesEvaluated.add(node);
			nodesToEvaluate.removeIf((pair) -> pair.getKey().distSqr(node) == 0D);
		}
		
		private void markForEvaluation(BlockPos node, BlockPos origin)
		{
			nodesToEvaluate.add(Pair.of(node, origin));
		}
		
		private boolean hasBeenEvaluated(BlockPos position)
		{
			for(BlockPos pos : nodesEvaluated)
				if(pos.distSqr(position) == 0D)
					return true;
			return false;
		}
		
		private enum Moves
		{
//			FALL(0, -1, 0),
//			PILLAR(0, 1, 0),
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