package com.example.examplemod.entity.ai.group;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class GroupAction
{
	private int completion = 0;
	
	protected abstract boolean start(List<LivingEntity> membersIn, Level world);
	
	protected abstract void tick(List<LivingEntity> membersIn, Level world);
	
	public final void markComplete() { this.completion = 2; }
	public final boolean isComplete() { return this.completion == 2; }
	
	public final void update(List<LivingEntity> membersIn, Level world)
	{
		switch(this.completion)
		{
			case 0:
				if(start(membersIn, world))
					this.completion++;
				break;
			case 1:
				tick(membersIn, world);
				break;
			case 2:
				break;
		}
	}
	
	public static class ActionQuarry<T extends PathfinderMob & ITreeEntity> extends GroupAction
	{
		public static final BiPredicate<BlockPos, Level> IS_MINABLE = (pos, world) ->
		{
			BlockState state = world.getBlockState(pos);
			boolean hasOpenSide = false;
			for(Direction dir : Direction.values())
			{
				BlockPos neighbour = pos.relative(dir);
				if(world.isEmptyBlock(neighbour) || world.getBlockState(neighbour).getCollisionShape(world, neighbour).isEmpty())
				{
					hasOpenSide = true;
					break;
				}
			}
			
			return hasOpenSide && !(world.isEmptyBlock(pos) || state.is(BlockTags.WITHER_IMMUNE) || state.getBlock().defaultDestroyTime() < 0F);
		};
		private final BlockPos minPos, maxPos;
		private final Direction orientation;
		private final Predicate<BlockPos> isInArea;
		
		// Members currently unoccupied
		private List<T> availableWorkers = Lists.newArrayList();
		// Blocks members are already mining
		private List<BlockPos> miningBlocks = Lists.newArrayList();
		
		public ActionQuarry(BlockPos pointA, BlockPos pointB, Direction facing)
		{
			this.minPos = new BlockPos(Math.min(pointA.getX(), pointB.getX()), Math.min(pointA.getY(), pointB.getY()), Math.min(pointA.getZ(), pointB.getZ()));
			this.maxPos = new BlockPos(Math.max(pointA.getX(), pointB.getX()), Math.max(pointA.getY(), pointB.getY()), Math.max(pointA.getZ(), pointB.getZ()));
			this.orientation = facing;
			
			this.isInArea = (input) -> {
				boolean xInside = input.getX() <= maxPos.getX() && input.getX() >= minPos.getX();
				boolean yInside = input.getY() <= maxPos.getY() && input.getY() >= minPos.getY();
				boolean zInside = input.getZ() <= maxPos.getZ() && input.getZ() >= minPos.getZ();
				return xInside && yInside && zInside;
			};
		}
		
		protected boolean start(List<LivingEntity> membersIn, Level world)
		{
			assessLabourers(membersIn);
			
			// Everyone is preoccupied
			if(availableWorkers.isEmpty())
				return false;
			
			// Let members already within the quarry area to mine the first nearby blocks
			boolean needsReassessment = false;
			for(T member : availableWorkers)
			{
				Vec3 eyePos = member.getEyePosition();
				BlockPos headPos = new BlockPos(eyePos.x, eyePos.y, eyePos.z);
				if(isInArea.test(headPos))
				{
					List<BlockPos> consignment = makeConsignmentFor(member, minPos, maxPos, world, miningBlocks, 2 + member.getRandom().nextInt(3));
					if(!consignment.isEmpty())
					{
						assignConsignment(member, consignment);
						needsReassessment = true;
					}
				}
			};
			if(needsReassessment)
				assessLabourers(membersIn);
			if(availableWorkers.isEmpty())
				return true;
			
			// Assign everyone else to just mine the outer edge
			List<BlockPos> consignment = makeConsignment(minPos, maxPos, orientation, world, miningBlocks, 2 + world.getRandom().nextInt(3));
			while(!consignment.isEmpty() && !availableWorkers.isEmpty())
			{
				// Find nearest available worker to consignment
				int x = 0, y = 0, z = 0;
				for(BlockPos pos : consignment)
				{
					x += pos.getX();
					y += pos.getY();
					z += pos.getZ();
				}
				x /= consignment.size();
				y /= consignment.size();
				z /= consignment.size();
				BlockPos avgPos = new BlockPos(x, y, z);
				
				T worker = null;
				double minDist = Double.MAX_VALUE;
				for(T member : availableWorkers)
				{
					double dist = avgPos.distSqr(member.blockPosition());
					if(dist < minDist && member.getNavigation().createPath(consignment.get(0), 64) != null)
					{
						minDist = dist;
						worker = member;
					}
				}
				
				// No-one can path to the area, so we skip these blocks for the moment
				if(worker == null)
					continue;
				
				assignConsignment(worker, consignment);
				availableWorkers.remove(worker);
				
				miningBlocks.addAll(consignment);
				consignment = makeConsignment(minPos, maxPos, orientation, world, miningBlocks, 2 + world.getRandom().nextInt(3));
			}
			return true;
		}
		
		protected void tick(List<LivingEntity> membersIn, Level world)
		{
			assessLabourers(membersIn);
			
			// Everyone is preoccupied
			if(availableWorkers.isEmpty())
				return;
			
			boolean foundMinable = false;
			for(T member : availableWorkers)
			{
				List<BlockPos> consignment = makeConsignmentFor(member, minPos, maxPos, world, miningBlocks, 2 + member.getRandom().nextInt(3));
				if(!consignment.isEmpty())
					foundMinable = true;
				assignConsignment(member, consignment);
				miningBlocks.addAll(consignment);
			}
			if(!foundMinable && miningBlocks.isEmpty())
				markComplete();
		}
		
		@SuppressWarnings("unchecked")
		private void assessLabourers(List<LivingEntity> membersIn)
		{
			availableWorkers.clear();
			miningBlocks.clear();
			membersIn.forEach((living) -> 
			{
				T member = null;
				try
				{
					member = (T)living;
				}
				catch(Exception e) { }
				if(member == null)
					return;
				
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
				if(!board.hasCommands())
					availableWorkers.add(member);
				else
				{
					CommandStack stack = board.getCommands();
					stack.allTasks().forEach((task) -> { if(task.type() == Mark.MINE) miningBlocks.add((BlockPos)task.variable(0)); });
				}
			});
		}
		
		private void assignConsignment(T recipient, List<BlockPos> blocks)
		{
			CommandStack stack = new CommandStack();
			sortConsignment(blocks, recipient).forEach((pos) -> stack.addTask(new MobCommand(Mark.MINE, pos))); 
			Whiteboard.tryGetWhiteboard(recipient).setCommands(stack);
		}
		
		/** Sorts the given blocks into the most cohesive cluster possible, minimising the need to move around */
		private List<BlockPos> sortConsignment(List<BlockPos> blocksIn, T recipient)
		{
			List<BlockPos> blocks = Lists.newArrayList();
			blocks.addAll(blocksIn);
			
			if(blocks.isEmpty() || blocks.size() == 1)
				return blocks;
			
			List<BlockPos> sorted = Lists.newArrayList();
			
			// Find closest block to recipient's head
			blocks.sort((o1,o2) -> 
			{
				double d1 = recipient.getEyePosition().distanceToSqr(o1.getX() + 0.5D, o1.getY() + 0.5D, o1.getZ() + 0.5D);
				double d2 = recipient.getEyePosition().distanceToSqr(o2.getX() + 0.5D, o2.getY() + 0.5D, o2.getZ() + 0.5D);
				return d1 < d2 ? -1 : d1 > d2 ? 1 : 0;
			});
			sorted.add(blocks.remove(0));
			
			// Sort remaining blocks by distance to last sorted
			while(!blocks.isEmpty())
			{
				blocks.sort((o1, o2) -> 
				{
					BlockPos precedent = sorted.get(sorted.size() - 1);
					double d1 = o1.distSqr(precedent);
					double d2 = o2.distSqr(precedent);
					return d1 < d2 ? -1 : d1 > d2 ? 1 : 0;
				});
				sorted.add(blocks.remove(0));
			}
			
			return sorted;
		}
		
		/** Creates a consignment of minable blocks within the area of the two points */
		public static List<BlockPos> makeConsignment(BlockPos minPos, BlockPos maxPos, Direction orientation, Level world, List<BlockPos> occupied, int size)
		{
			List<BlockPos> consignment = Lists.newArrayList();
			Predicate<BlockPos> isValid = (input) -> { return !occupied.contains(input) && IS_MINABLE.test(input, world); };
			
			// Search directions
			Vec3i forward = orientation.getNormal();
			Vec3i right = orientation.getClockWise().getNormal();
			Vec3i up = orientation.getAxis() == Axis.Y ? Direction.NORTH.getNormal() : Direction.UP.getNormal();
			
			// Overall direction of search
			Vec3i search = forward.offset(right).offset(up);
			BlockPos startPos = new BlockPos(
					search.getX() > 0 ? minPos.getX() : maxPos.getX(), 
					search.getY() > 0 ? minPos.getY() : maxPos.getY(), 
					search.getZ() > 0 ? minPos.getZ() : maxPos.getZ()
					);
			BlockPos endPos = new BlockPos(
					search.getX() > 0 ? maxPos.getX() : minPos.getX(), 
					search.getY() > 0 ? maxPos.getY() : minPos.getY(), 
					search.getZ() > 0 ? maxPos.getZ() : minPos.getZ()
					);
			
			Predicate<BlockPos> isInArea = (input) -> {
				boolean xInside = input.getX() <= maxPos.getX() && input.getX() >= minPos.getX();
				boolean yInside = input.getY() <= maxPos.getY() && input.getY() >= minPos.getY();
				boolean zInside = input.getZ() <= maxPos.getZ() && input.getZ() >= minPos.getZ();
				return xInside && yInside && zInside;
			};
			
			int stepForward = 0, stepRight = 0, stepUp = 0;
			BlockPos current = startPos;
			while(current != endPos && consignment.size() < size)
			{
				// Step right when we've completed an upward sequence
				current = startPos.offset(forward.multiply(stepForward)).offset(right.multiply(stepRight)).offset(up.multiply(stepUp));
				if(!isInArea.test(current))
				{
					stepUp = 0;
					stepRight++;
				}
				
				// Step forward when we've completed a right-ward sequence
				current = startPos.offset(forward.multiply(stepForward)).offset(right.multiply(stepRight)).offset(up.multiply(stepUp));
				if(!isInArea.test(current))
				{
					stepRight = 0;
					stepForward++;
				}
				
				// If still outside of the area, we've completed searching the entire region
				current = startPos.offset(forward.multiply(stepForward)).offset(right.multiply(stepRight)).offset(up.multiply(stepUp));
				if(!isInArea.test(current))
					break;
				
				if(isValid.test(current))
					consignment.add(current);
				
				stepUp++;
			}
			
			return consignment;
		}
		
		/** Creates a consignment of minable blocks within the area closest to the given member */
		private List<BlockPos> makeConsignmentFor(T entity, BlockPos minPos, BlockPos maxPos, Level world, List<BlockPos> occupied, int size)
		{
			List<BlockPos> consignment = Lists.newArrayList();
			
			int maxX = Math.max(minPos.getX(), maxPos.getX()); int minX = Math.min(minPos.getX(), maxPos.getX());
			int maxY = Math.max(minPos.getY(), maxPos.getY()); int minY = Math.min(minPos.getY(), maxPos.getY());
			int maxZ = Math.max(minPos.getZ(), maxPos.getZ()); int minZ = Math.min(minPos.getZ(), maxPos.getZ());
			int area = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
			
			BlockPos headPos = new BlockPos(entity.getEyePosition().x, entity.getEyePosition().y, entity.getEyePosition().z);
			
			Predicate<BlockPos> isValid = (input) -> { return !occupied.contains(input) && IS_MINABLE.test(input, world); };
			
			List<BlockPos> checked = Lists.newArrayList();
			List<BlockPos> nextCheck = Lists.newArrayList();
			
			// Start search from the head position, clamped to within the quarry area
			nextCheck.add(new BlockPos(Mth.clamp(headPos.getX(), minX, maxX), Mth.clamp(headPos.getY(), minY, maxY), Mth.clamp(headPos.getZ(), minZ, maxZ)));
			while(!nextCheck.isEmpty() && consignment.size() < size && checked.size() < area)
			{
				List<BlockPos> thisCheck = Lists.newArrayList();
				thisCheck.addAll(nextCheck);
				nextCheck.clear();
				
				for(BlockPos pos : thisCheck)
				{
					if(checked.contains(pos))
						continue;
					
					checked.add(pos);
					if(isValid.apply(pos))
					{
						consignment.add(pos);
						if(consignment.size() >= size)
							break;
					}
					
					for(Direction dir : Direction.values())
					{
						BlockPos offset = pos.relative(dir);
						if(isInArea.test(offset))
							nextCheck.add(offset);
					}
				}
			}
			
			consignment.sort(new Comparator<BlockPos>() 
			{
				public int compare(BlockPos o1, BlockPos o2)
				{
					double dist1 = o1.distSqr(headPos);
					double dist2 = o2.distSqr(headPos);
					return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
				}
			});
			
			return consignment;
		}
	}
}
