package com.example.examplemod.entity.ai.group.action;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.entity.ai.group.action.ActionUtils.MemberData;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;

public abstract class GroupAction
{
	private final ResourceLocation registryName;
	
	private Status status = Status.STARTING;
	private final int minComplement;
	private int complement = -1;
	
	private int maxChildren = 1;
	private Map<ResourceLocation, GroupAction> children = new HashMap<>();
	private List<ActionOption> potentialChildren = Lists.newArrayList();
	
	protected GroupAction(ResourceLocation nameIn, int complementIn)
	{
		this.registryName = nameIn;
		this.minComplement = complementIn;
	}
	
	public ResourceLocation getRegistryName() { return this.registryName; }
	
	public GroupAction addOption(ActionOption optionIn) { this.potentialChildren.add(optionIn); return this; }
	
	public boolean hasChildren() { return !this.children.isEmpty(); }
	public Collection<GroupAction> children() { return this.children.values(); }
	public void setMaxChildren(int par1Int) { this.maxChildren = Math.max(0, par1Int); }
	public int maxChildren() { return this.maxChildren; }
	
	/** Returns how many members this action is allowed to use (or -1 if it can use all members provided) */
	public int getComplement() { return this.complement; }
	public GroupAction setComplement(int par1Int) { this.complement = par1Int; return this; }
	
	/** Returns the minimum number of participants for this action to begin */
	public int minimumComplement() { return this.minComplement; }
	
	/** Performs any necessary initialisation operations */
	protected boolean start(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world) { return true; }
	
	/** Updates the internal logic of this action */
	protected abstract void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world);
	
	public final Status status() { return this.status; }
	public final void setStatus(Status statusIn) { this.status = statusIn; }
	public final void markComplete() { setStatus(Status.COMPLETE); }
	public final boolean isComplete() { return this.status == Status.COMPLETE; }
	
	/** Clears all command stacks in all given members.<br>Used when changing from some long-form group actions. */
	public static final void clearOrders(List<LivingEntity> membersIn)
	{
		membersIn.forEach((entity) -> { if(entity instanceof ITreeEntity) Whiteboard.tryGetWhiteboard(entity).setCommands(null); });
	}
	
	/**
	 * Initialises or updates this group action
	 * @param membersIn List of the available group members for this action
	 * @param targetsIn Any attack targets of the group
	 * @param world The world this group is operating in
	 * @param groupSize The total size of this group
	 */
	public final void update(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
	{
		if(isComplete() || getComplement() == 0 || this.status == Status.STARTING && membersIn.size() < minimumComplement())
		{
			markComplete();
			return;
		}
		
		List<LivingEntity> members = Lists.newArrayList();
		if(getComplement() < 0)
		{
			members.addAll(membersIn);
			membersIn.clear();
		}
		else if(getComplement() > 0)
		{
			List<LivingEntity> sub = membersIn.subList(0, getComplement());
			members.addAll(sub);
			membersIn.removeAll(sub);
		}
		
		if(members.isEmpty())
			return;
		
		// Remove handled members from the input list to prevent two actions trying to command the same member
		updateWithComplement(members, targetsIn, world);
	}
	
	private final void updateWithComplement(List<LivingEntity> members, List<LivingEntity> targetsIn, Level world)
	{
		if(!this.children.isEmpty())
		{
			List<ResourceLocation> completed = Lists.newArrayList();
			for(GroupAction child : this.children.values())
			{
				if(child.isComplete())
					completed.add(child.getRegistryName());
				else
					child.update(members, targetsIn, world);
			}
			completed.forEach((name) -> this.children.remove(name));
		}
		
		int size = members.size();
		if(size > 1 && this.children.size() < maxChildren())
		{
			GroupAction bestOption = evaluateOptions(targetsIn, size - 1);
			if(bestOption != null)
				addChild(bestOption, true);
		}
		
		switch(this.status)
		{
			case STARTING:
				if(start(members, targetsIn, world))
					this.status = Status.RUNNING;
				break;
			case RUNNING:
				tick(members, targetsIn, world);
				break;
			case COMPLETE:
				break;
		}
	}
	
	public final void addChild(GroupAction childIn, boolean report)
	{
		this.children.put(childIn.getRegistryName(), childIn);
		if(report)
			ExampleMod.LOG.info("Added child action "+childIn.getRegistryName()+" to "+getRegistryName());
	}
	
	/** Stores the action in NBT data for transmission and/or storage */
	public final CompoundTag storeInNbt(CompoundTag compound)
	{
		compound.putString("Type", getRegistryName().toString());
		compound.putInt("Complement", getComplement());
		compound.putInt("Status", status().ordinal());
		compound.put("Data", saveToNbt(new CompoundTag()));
		ListTag childActions = new ListTag();
		for(GroupAction child : children.values())
			childActions.add(child.storeInNbt(new CompoundTag()));
		compound.put("Children", childActions);
		compound.putInt("ChildLimit", this.maxChildren);
		return compound;
	}
	
	protected CompoundTag saveToNbt(CompoundTag compound) { return compound; }
	
	public void loadFromNbt(CompoundTag compound) { }
	
	@Nullable
	public GroupAction evaluateOptions(List<LivingEntity> targetsIn, int supply)
	{
		if(supply < 0)
			supply = 0;
		
		ActionOption bestOption = null;
		float utility = Float.MIN_VALUE;
		for(ActionOption option : this.potentialChildren)
		{
			GroupAction action = option.get(this, supply);
			if(option.isValid(targetsIn, this, supply) && !this.children.containsKey(action.getRegistryName()) && supply >= action.minimumComplement())
			{
				float util = option.utility(targetsIn, this, supply);
				if(util > utility)
				{
					utility = util;
					bestOption = option;
				}
			}
		}
		
		return bestOption != null ? bestOption.get(this, supply) : null;
	}
	
	public static enum Status implements StringRepresentable
	{
		STARTING,
		RUNNING,
		COMPLETE;
		
		public String getSerializedName() { return name().toLowerCase(); }
		
		public static Status fromName(String nameIn)
		{
			for(Status status : values())
				if(status.getSerializedName().equalsIgnoreCase(nameIn))
					return status;
			return STARTING;
		}
	}
	
	public static class ActionQuarry extends GroupAction
	{
		/** Returns true if the given position has an open neighbour and passes minability checks*/
		public static final BiPredicate<BlockPos, Level> IS_MINABLE = (pos, world) ->
		{
			BlockState state = world.getBlockState(pos);
			if(world.isEmptyBlock(pos) || state.getCollisionShape(world, pos).isEmpty() || state.is(BlockTags.WITHER_IMMUNE) || state.getBlock().defaultDestroyTime() < 0F)
				return false;
			
			for(Direction dir : Direction.values())
			{
				BlockPos neighbour = pos.relative(dir);
				if(world.isEmptyBlock(neighbour) || world.getBlockState(neighbour).getCollisionShape(world, neighbour).isEmpty())
					return true;
			}
			
			return false;
		};
		private BlockPos minPos, maxPos;
		private Direction orientation;
		private final Predicate<BlockPos> isInArea;
		
		// Members currently unoccupied
		private List<LivingEntity> availableWorkers = Lists.newArrayList();
		// Blocks members are already mining
		private List<BlockPos> miningBlocks = Lists.newArrayList();
		
		public ActionQuarry(BlockPos pointA, BlockPos pointB, Direction facing)
		{
			super(ActionType.QUARRY, 1);
			this.minPos = new BlockPos(Math.min(pointA.getX(), pointB.getX()), Math.min(pointA.getY(), pointB.getY()), Math.min(pointA.getZ(), pointB.getZ()));
			this.maxPos = new BlockPos(Math.max(pointA.getX(), pointB.getX()), Math.max(pointA.getY(), pointB.getY()), Math.max(pointA.getZ(), pointB.getZ()));
			this.orientation = facing;
			
			this.isInArea = (input) -> {
				boolean xInside = input.getX() <= maxPos.getX() && input.getX() >= minPos.getX();
				boolean yInside = input.getY() <= maxPos.getY() && input.getY() >= minPos.getY();
				boolean zInside = input.getZ() <= maxPos.getZ() && input.getZ() >= minPos.getZ();
				return xInside && yInside && zInside;
			};
			
			addOption(new ActionOption(
					(targets,action,supply) -> { return supply > 1 ? 1F : -1F; },
					(action,supply) -> new ActionPickUp(this.minPos, this.maxPos).setComplement(1)
					));
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Min", NbtUtils.writeBlockPos(minPos));
			compound.put("Max", NbtUtils.writeBlockPos(maxPos));
			compound.putString("Face", this.orientation.getSerializedName());
			
			ListTag blocks = new ListTag();
			for(BlockPos pos : miningBlocks)
				blocks.add(NbtUtils.writeBlockPos(pos));
			compound.put("Blocks", blocks);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			this.minPos = NbtUtils.readBlockPos(compound.getCompound("Min"));
			this.maxPos = NbtUtils.readBlockPos(compound.getCompound("Max"));
			this.orientation = Direction.byName(compound.getString("Face"));
			
			ListTag blocks = compound.getList("Blocks", Tag.TAG_COMPOUND);
			for(int i=0; i<blocks.size(); i++)
				this.miningBlocks.add(NbtUtils.readBlockPos(blocks.getCompound(i)));
		}
		
		public AABB getBounds() { return new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1D, maxPos.getY() + 1D, maxPos.getZ() + 1D); }
		public List<BlockPos> currentLot() { return this.miningBlocks; }
		
		protected boolean start(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			assessLabourers(membersIn);
			
			// Everyone is preoccupied
			if(availableWorkers.isEmpty())
				return false;
			
			// Let members already within the quarry area to mine the first nearby blocks
			boolean needsReassessment = false;
			for(LivingEntity member : availableWorkers)
			{
				Vec3 eyePos = member.getEyePosition();
				BlockPos headPos = new BlockPos(eyePos.x, eyePos.y, eyePos.z);
				if(isInArea.test(headPos) && member instanceof ITreeEntity)
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
				
				LivingEntity worker = null;
				double minDist = Double.MAX_VALUE;
				for(LivingEntity member : availableWorkers)
				{
					if(!(member instanceof PathfinderMob))
						continue;
					double dist = avgPos.distSqr(member.blockPosition());
					if(dist < minDist && ((PathfinderMob)member).getNavigation().createPath(consignment.get(0), 64) != null)
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
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			assessLabourers(membersIn);
			
			// Everyone is preoccupied
			if(availableWorkers.isEmpty())
				return;
			
			boolean foundMinable = false;
			for(LivingEntity member : availableWorkers)
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
		
		private void assessLabourers(List<LivingEntity> membersIn)
		{
			availableWorkers.clear();
			miningBlocks.clear();
			membersIn.forEach((living) -> 
			{
				if(!(living instanceof ITreeEntity))
					return;
				
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(living);
				if(!board.hasCommands())
					availableWorkers.add(living);
				else
				{
					CommandStack stack = board.getCommands();
					stack.allTasks().forEach((task) -> { if(task.type() == Mark.MINE) miningBlocks.add((BlockPos)task.variable("Pos")); });
				}
			});
		}
		
		private void assignConsignment(LivingEntity recipient, List<BlockPos> blocks)
		{
			CommandStack stack = new CommandStack();
			sortConsignment(blocks, (LivingEntity)recipient).forEach((pos) -> stack.append(Mark.atPos(Mark.MINE, pos))); 
			Whiteboard.tryGetWhiteboard(recipient).setCommands(stack);
		}
		
		/** Sorts the given blocks into the most cohesive cluster possible, minimising the need to move around */
		private List<BlockPos> sortConsignment(List<BlockPos> blocksIn, LivingEntity recipient)
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
		private List<BlockPos> makeConsignmentFor(LivingEntity entity, BlockPos minPos, BlockPos maxPos, Level world, List<BlockPos> occupied, int size)
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
	
	public static class ActionFarm extends GroupAction
	{
		public static final Predicate<ItemStack> IS_SEED = (stack) -> stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof IPlantable;
		private static final EquipmentSlot[] SLOTS_SEARCHED = new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
		
		private BlockPos minPos, maxPos;
		// The mapping area marked by its crop state
		private Map<CropState, List<BlockPos>> farmMap = new HashMap<>();
		// Blocks that have been altered since the last major scan
		private List<BlockPos> blocksAltered = Lists.newArrayList();
		
		private int recheckTicks = 0;
		
		public ActionFarm(BlockPos pointA, BlockPos pointB)
		{
			super(ActionType.FARM, 1);
			this.minPos = pointA;
			this.maxPos = pointB;
			
			addOption(new ActionOption(
				(targets,action,supply) -> supply > 0 ? 1F : -1F,
				(parent,supply) -> new ActionPickUpNonSeeds(minPos, maxPos).setComplement(1)));
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Min", NbtUtils.writeBlockPos(minPos));
			compound.put("Max", NbtUtils.writeBlockPos(maxPos));
			
			ListTag states = new ListTag();
			for(CropState state : CropState.values())
			{
				ListTag positions = new ListTag();
				if(farmMap.containsKey(state))
					for(BlockPos pos : farmMap.get(state))
						positions.add(NbtUtils.writeBlockPos(pos));
				states.add(positions);
			}
			compound.put("States", states);
			
			ListTag boned = new ListTag();
			blocksAltered.forEach((pos) -> boned.add(NbtUtils.writeBlockPos(pos)));
			compound.put("Altered", boned);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			this.minPos = NbtUtils.readBlockPos(compound.getCompound("Min"));
			this.maxPos = NbtUtils.readBlockPos(compound.getCompound("Max"));
			
			farmMap.clear();
			ListTag states = compound.getList("States", Tag.TAG_LIST);
			for(int i=0; i<states.size(); i++)
			{
				CropState state = CropState.values()[i];
				ListTag positions = states.getList(i);
				if(positions.isEmpty())
					continue;
				
				List<BlockPos> list = Lists.newArrayList();
				for(int j=0; j<positions.size(); j++)
					list.add(NbtUtils.readBlockPos(positions.getCompound(j)));
				
				farmMap.put(state, list);
			}
			
			blocksAltered.clear();
			ListTag boned = compound.getList("Altered", Tag.TAG_COMPOUND);
			for(Tag tag : boned)
				blocksAltered.add(NbtUtils.readBlockPos((CompoundTag)tag));
		}
		
		public AABB getBounds() { return new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1D, maxPos.getY() + 1D, maxPos.getZ() + 1D); }
		public List<BlockPos> getBlocksOfState(CropState stateIn)
		{
			if(farmMap.containsKey(stateIn))
			{
				List<BlockPos> blocks = Lists.newArrayList();
				blocks.addAll(farmMap.get(stateIn));
				return blocks;
			}
			return Lists.newArrayList();
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			List<LivingEntity> workers = Lists.newArrayList();
			workers.addAll(membersIn);
			workers.removeIf((mob) -> !(mob instanceof ITreeEntity));
			
			// Every major scan, attempt to expand or improve the farm area
			if(recheckTicks++ % (Reference.Values.TICKS_PER_SECOND * 10) == 0)
			{
				blocksAltered.clear();
				farmMap.clear();
				for(int y=minPos.getY(); y<=maxPos.getY(); y++)
					for(int x=minPos.getX(); x<=maxPos.getX(); x++)
						for(int z=minPos.getZ(); z<=maxPos.getZ(); z++)
						{
							BlockPos position = new BlockPos(x, y, z);
							CropState state = getStateOfCrop(position, world);
							if(state != null)
							{
								List<BlockPos> set = farmMap.containsKey(state) ? farmMap.get(state) : Lists.newArrayList();
								set.add(position);
								farmMap.put(state, set);
							}
						}
			}
			// Between major scans, monitor known crops
			else
			{
				manageBonemeal(workers);
				managePlant(world, workers);
				getBlocksOfState(CropState.HARVEST).forEach((pos) -> managePosHarvest(pos, world, workers));
			}
		}
		
		@Nullable
		private CropState updateStateOfPos(BlockPos pos, Level world)
		{
			CropState oldState = stateOfPos(pos);
			CropState newState = getStateOfCrop(pos, world);
			if(oldState != newState)
				registerPosAs(pos, newState);
			return newState;
		}
		
		private void manageBonemeal(List<LivingEntity> workers)
		{
			// List of workers able to bonemeal crops if necessary
			List<LivingEntity> fertilisers = Lists.newArrayList();
			workers.forEach((worker) -> 
			{
				if(worker.getMainHandItem().is(Items.BONE_MEAL) || worker.getOffhandItem().is(Items.BONE_MEAL))
					fertilisers.add(worker);
			});
			
			List<BlockPos> toFertilise = getBlocksOfState(CropState.BONEMEAL);
			toFertilise.removeAll(blocksAltered);
			if(!fertilisers.isEmpty() && !toFertilise.isEmpty())
				for(LivingEntity fertiliser : fertilisers)
				{
					if(toFertilise.isEmpty())
						break;
					
					Whiteboard<?> storage = Whiteboard.tryGetWhiteboard(fertiliser);
					if(storage == null)
						continue;
					
					CommandStack existing = storage.getCommands();
					if(existing != null && !existing.isEmpty())
					{
						boolean alreadyWorking = false;
						for(MobCommand command : existing.allTasks())
							if(command.type() == Mark.BONEMEAL)
							{
								alreadyWorking = true;
								BlockPos position = (BlockPos)command.variable("Pos");
								if(!blocksAltered.contains(position))
									blocksAltered.add(position);
							}
						
						if(alreadyWorking)
							continue;
					}
					
					int supply =
							(fertiliser.getMainHandItem().is(Items.BONE_MEAL) ? fertiliser.getMainHandItem().getCount() : 0) + 
							(fertiliser.getOffhandItem().is(Items.BONE_MEAL) ? fertiliser.getOffhandItem().getCount() : 0);
					BlockPos lastPos = fertiliser.blockPosition();
					CommandStack allotment = new CommandStack();
					while(allotment.size() < Math.min(toFertilise.size(), supply))
					{
						toFertilise = sortByDist(lastPos, toFertilise);
						BlockPos closest = toFertilise.remove(0);
						blocksAltered.add(closest);
						allotment.append(Mark.atPos(Mark.BONEMEAL, closest));
						lastPos = closest;
					}
					storage.setCommands(allotment);
					workers.remove(fertiliser);
				}
		}
		
		private static List<BlockPos> sortByDist(BlockPos lastPos, List<BlockPos> blocks)
		{
			blocks.sort((pos1, pos2) -> 
			{
				double dist1 = lastPos.distSqr(pos1);
				double dist2 = lastPos.distSqr(pos2);
				return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
			});
			return blocks;
		}
		
		private void managePlant(Level world, List<LivingEntity> workers)
		{
			if(workers.isEmpty())
				return;
			
			Map<LivingEntity, List<Block>> seedersToSeeds = Maps.newHashMap();
			List<LivingEntity> seeders = Lists.newArrayList();
			workers.forEach((worker) -> 
			{
				if(!(worker instanceof ITreeEntity) || Whiteboard.tryGetWhiteboard(worker).hasCommands())
					return;
				
				List<Block> seeds = Lists.newArrayList();
				for(EquipmentSlot slot : SLOTS_SEARCHED)
				{
					ItemStack heldItem = worker.getItemBySlot(slot);
					if(IS_SEED.apply(heldItem))
						seeds.add(((BlockItem)heldItem.getItem()).getBlock());
				}
				
				if(!seeds.isEmpty())
				{
					seedersToSeeds.put(worker, seeds);
					seeders.add(worker);
				}
			});
			
			// Prioritise seeding soil close to water
			List<BlockPos> fertileSoil = getBlocksOfState(CropState.PLANT);
			List<BlockPos> waterBlocks = getBlocksOfState(CropState.WATER);
			fertileSoil.sort((pos1, pos2) ->
			{
				double dist1 = Double.MAX_VALUE;
				double dist2 = Double.MAX_VALUE;
				
				for(BlockPos water : waterBlocks)
				{
					dist1 = Math.min(dist1, pos1.distSqr(water));
					dist2 = Math.min(dist2, pos2.distSqr(water));
				}
				
				return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
			});
			
			for(BlockPos pos : getBlocksOfState(CropState.PLANT))
			{
				CropState newState = updateStateOfPos(pos, world);
				if(newState != CropState.PLANT)
					return;
				
				BlockState stateBelow = world.getBlockState(pos.below());
				for(LivingEntity seeder : seeders)
				{
					if(!seedersToSeeds.containsKey(seeder) || Whiteboard.tryGetWhiteboard(seeder).hasCommands())
						continue;
					
					Block seed = null;
					for(Block block : seedersToSeeds.get(seeder))
						if(stateBelow.canSustainPlant(world, pos, Direction.UP, (IPlantable)block))
							seed = block;
					
					if(seed != null)
					{
						Whiteboard.tryGetWhiteboard(seeder).setCommands(new CommandStack(Mark.placeBlock(pos, Direction.DOWN, seed)));
						workers.remove(seeder);
						seedersToSeeds.remove(seeder);
						break;
					}
				}
			}
			
			// Any inactive workers tasked to pick up seeds and bonemeal in the area
			List<ItemEntity> itemsInArea = world.getEntitiesOfClass(ItemEntity.class, new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1D, maxPos.getY() + 1D, maxPos.getZ() + 1D));
			itemsInArea.removeIf((item) -> !IS_SEED.apply(item.getItem()) && !item.getItem().is(Items.BONE_MEAL));
			
			for(ItemEntity item : itemsInArea)
			{
				ItemStack stack = item.getItem();
				
				LivingEntity closest = null;
				double minDist = Double.MAX_VALUE;
				for(LivingEntity member : workers)
				{
					// If someone is already holding this item in a non-full stack, assign them
					boolean foundExisting = false;
					boolean hasEmpty = false;
					for(EquipmentSlot slot : SLOTS_SEARCHED)
					{
						ItemStack heldStack = member.getItemBySlot(slot);
						if(heldStack.isEmpty())
							hasEmpty = true;
						else if(heldStack.getItem() == stack.getItem() && heldStack.areCapsCompatible(stack) && heldStack.areShareTagsEqual(stack) && heldStack.getCount() < heldStack.getMaxStackSize())
						{
							closest = member;
							foundExisting = true;
							break;
						}
					}
					
					// Otherwise pick the closest person with an empty slot
					if(!foundExisting && hasEmpty)
					{
						double dist = member.distanceToSqr(item);
						if(dist < minDist)
						{
							minDist = dist;
							closest = member;
						}
					}
					else
						break;
				}
				
				if(closest != null)
				{
					Whiteboard.tryGetWhiteboard(closest).setCommands(CommandStack.single(Mark.onEntity(Mark.PICK_UP, item)));
					workers.remove(closest);
				}
			}
		}
		
		private void managePosHarvest(BlockPos pos, Level world, List<LivingEntity> workers)
		{
			if(workers.isEmpty())
				return;
			
			CropState newState = updateStateOfPos(pos, world);
			if(newState != CropState.HARVEST || blocksAltered.contains(pos))
				return;
			
			LivingEntity harvester = null;
			double minDist = Double.MAX_VALUE;
			for(LivingEntity member : workers)
			{
				if(Whiteboard.tryGetWhiteboard(member).hasCommands())
					continue;
				
				double dist = member.distanceToSqr(new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
				if(dist < minDist)
				{
					minDist = dist;
					harvester = member;
				}
			}
			
			if(harvester != null)
			{
				Whiteboard.tryGetWhiteboard(harvester).setCommands(CommandStack.single(Mark.atPos(Mark.MINE, pos)));
				workers.remove(harvester);
				blocksAltered.add(pos);
			}
		}
		
		@Nullable
		private CropState stateOfPos(BlockPos pos)
		{
			for(CropState state : CropState.values())
				if(farmMap.containsKey(state) && farmMap.get(state).contains(pos))
					return state;
			return null;
		}
		
		private void registerPosAs(BlockPos pos, @Nullable CropState newState)
		{
			CropState oldState = stateOfPos(pos);
			if(oldState == newState)
				return;
			else if(oldState != null)
			{
				List<BlockPos> oldSet = farmMap.get(oldState);
				oldSet.remove(pos);
				farmMap.put(oldState, oldSet);
			}
			
			if(newState != null)
			{
				List<BlockPos> newSet = farmMap.containsKey(newState) ? farmMap.get(newState) : Lists.newArrayList();
				newSet.add(pos);
				farmMap.put(newState, newSet);
			}
		}
		
		@Nullable
		public static CropState getStateOfCrop(BlockPos pos, Level world)
		{
			BlockState state = world.getBlockState(pos);
			if(state.getFluidState().is(FluidTags.WATER))
				return CropState.WATER;
			else if(world.isEmptyBlock(pos))
				return world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP) || world.getBlockState(pos.below()).is(Blocks.FARMLAND) ? CropState.PLANT : null;
			else
			{
				Block block = state.getBlock();
				if(block instanceof StemGrownBlock)
					return CropState.HARVEST;
				else if(block instanceof BonemealableBlock)
					return ((BonemealableBlock)block).isValidBonemealTarget(world, pos, state, false) ? CropState.BONEMEAL : (block instanceof CropBlock ? CropState.HARVEST : null);
			}
			return null;
		}
		
		public static enum CropState
		{
			WATER(0F, 1F, 1F),
			PLANT(1F, 0F, 0F),
			BONEMEAL(0F, 0F, 1F),
			HARVEST(0F, 1F, 0F);
			
			/** States to monitor inbetween major scans */
			public static final EnumSet<CropState> WATCHABLES = EnumSet.of(PLANT, BONEMEAL, HARVEST);
			
			Float[] colours;
			
			private CropState(float r, float g, float b)
			{
				colours = new Float[] {r, g, b};
			}
			
			public float red() { return colours[0]; }
			public float green() { return colours[1]; }
			public float blue() { return colours[2]; }
		}
	}
	
	public static class ActionFollow extends GroupAction
	{
		private MemberData follow = null;
		private Vec3 followPos = Vec3.ZERO;
		
		private double minDist;
		private double maxDist;
		
		private List<LivingEntity> order = Lists.newArrayList();
		
		public ActionFollow(@Nullable LivingEntity target, double min, double max)
		{
			super(ActionType.FOLLOW, 1);
			
			this.follow = new MemberData(target);
			if(target != null)
				this.followPos = target.position();
			this.minDist = min;
			this.maxDist = max;
		}
		
		public ActionFollow(LivingEntity target, double distA)
		{
			this(target, distA, distA);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			compound.put("Target", this.follow.saveToNbt(new CompoundTag()));
			
			CompoundTag posData = new CompoundTag();
			posData.putDouble("X", followPos.x);
			posData.putDouble("Y", followPos.y);
			posData.putDouble("Z", followPos.z);
			compound.put("Pos", posData);
			compound.putDouble("Min", minDist);
			compound.putDouble("Max", maxDist);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			follow = MemberData.fromNbt(compound.getCompound("Target"));
			CompoundTag posData = compound.getCompound("Pos");
			followPos = new Vec3(posData.getDouble("X"), posData.getDouble("Y"), posData.getDouble("Z"));
			minDist = compound.getDouble("Min");
			maxDist = compound.getDouble("Max");
		}
		
		public Vec3 followPos() { return this.followPos; }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			if(this.follow != null && !this.follow.cached())
				ActionUtils.tryFindEntityNearby(this.follow, membersIn);
			
			if(this.order.isEmpty())
				populateOrder(membersIn);
			
			if(this.follow.cached())
				this.followPos = this.follow.get().position();
			
			for(LivingEntity member : membersIn)
			{
				int index = order.indexOf(member);
				LivingEntity target = index > 0 ? order.get(index - 1) : follow.get();
				if(target == null)
					continue;
				
				double dist = member.distanceToSqr(target);
				if(member instanceof PathfinderMob && member instanceof ITreeEntity)
				{
					Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
					if(board != null)
						if(!board.hasCommands() && dist > (maxDist * maxDist))
							board.setCommands(CommandStack.single(Mark.onEntity(Mark.GOTO_MOB, target)));
						else if(dist < (minDist * minDist) && ((PathfinderMob)member).getNavigation().isInProgress())
							board.setCommands(CommandStack.single(Mark.STOP_MOVING));
				}
			}
		}
		
		private void populateOrder(List<LivingEntity> members)
		{
			order.clear();
			order.addAll(members);
			order.sort((mobA, mobB) ->
			{
				float durabilityA = ActionUtils.assessDurability(mobA);
				float durabilityB = ActionUtils.assessDurability(mobB);
				return durabilityA < durabilityB ? 1 : durabilityA > durabilityB ? -1 : 0;
			});
		}
	}
	
	public static class ActionGuardMob extends ActionFormation
	{
		// Manual curve plot
		private final Map<Double,Float> utilityPlot = new HashMap<>();
		
		private MemberData target = null;
		
		private Vec3 lastPos = Vec3.ZERO;
		private int rethinkTicks = 0;
		
		public ActionGuardMob(@Nullable LivingEntity target, double min, double max)
		{
			super(ActionType.GUARD_MOB, 1);
			if(target != null)
			{
				this.target = new MemberData(target);
				this.lastPos = target.position();
			}
			
			this.minDist = min * min;
			this.maxDist = max * max;
			generateUtilityPlot();
			
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : ((size * 0.3F) / targets.size()) / 2F,
					(action,supply) -> new ActionFlank().setComplement((int)Math.ceil(supply * 0.3D))
					));
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : 0.5F,
					(action,supply) -> new ActionBrawl().setComplement((int)Math.ceil(supply * 0.4D))
					));
		}
		
		public ActionGuardMob(LivingEntity target, double distA)
		{
			this(target, distA, distA);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Target", this.target.saveToNbt(new CompoundTag()));
			
			ListTag posData = new ListTag();
			posData.add(DoubleTag.valueOf(lastPos.x));
			posData.add(DoubleTag.valueOf(lastPos.y));
			posData.add(DoubleTag.valueOf(lastPos.z));
			compound.put("Pos", posData);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			target = MemberData.fromNbt(compound.getCompound("Target"));
			
			ListTag posData = compound.getList("Pos", Tag.TAG_DOUBLE);
			lastPos = new Vec3(posData.getDouble(0), posData.getDouble(1), posData.getDouble(2));
			generateUtilityPlot();
		}
		
		private void generateUtilityPlot()
		{
			utilityPlot.clear();
			utilityPlot.put(0D, 0F);
			utilityPlot.put(minDist / 2, 0.3F);
			utilityPlot.put(minDist, 1F);
			utilityPlot.put((maxDist + minDist) * 0.5D, 0.9F);
			utilityPlot.put(maxDist, 0F);
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			super.tick(membersIn, targetsIn, world);
			
			if(target == null || (target.cached() && !target.get().isAlive()))
			{
				markComplete();
				return;
			}
			else if(!target.cached())
			{
				ActionUtils.tryFindEntityNearby(this.target, membersIn);
				return;
			}
			
			LivingEntity guardTarget = target.get();
			if(guardTarget.position().distanceToSqr(lastPos) > minDist)
				lastPos = guardTarget.position();
			
			// FIXME Relative vector position, not block position
			
			// Once every 5 seconds, clean the formation to account for unit losses or reassignments
			if(++rethinkTicks%(Reference.Values.TICKS_PER_SECOND * 5) == 0)
			{
				List<LivingEntity> toRemove = Lists.newArrayList();
				toRemove.addAll(trackedMembers());
				toRemove.removeAll(membersIn);
				
				toRemove.forEach((member) -> removeTrackedEntity(member));
			}
			
			// Assign one outstanding member to the formation to minimise CPU load
			if(formationPoints().size() < membersIn.size())
			{
				double highest = Double.MIN_VALUE;
				
				Collection<BlockPos> unitPositions = formationPoints();
				List<BlockPos> bestList = Lists.newArrayList(BlockPos.ZERO);
				int range = (int)Math.sqrt(maxDist);
				AABB searchArea = new AABB(-range, 0, -range, range, 1, range);
				for(int x=(int)searchArea.minX; x<searchArea.maxX; x++)
					for(int z=(int)searchArea.minZ; z<searchArea.maxZ; z++)
					{
						BlockPos offset = new BlockPos(x, 0, z);
						if(!unitPositions.contains(offset))
						{
							float utility = flankingUtility(offset, unitPositions, minDist, maxDist);
							if(utility >= highest)
							{
								if(highest != utility)
									bestList.clear();
								
								highest = utility;
								bestList.add(offset);
							}
						}
					}
				
				// Randomly select a position with the best score, if there are more than one
				BlockPos bestPosLocal = bestList.get(0);
				if(bestList.size() > 1)
					bestPosLocal = bestList.get(world.getRandom().nextInt(bestList.size()));
				
				BlockPos bestPosWorld = guardTarget.blockPosition().offset(bestPosLocal);
				
				// Find closest unassigned member to guard position, and assign
				LivingEntity closest = null;
				double lowest = Double.MAX_VALUE;
				
				List<LivingEntity> options = Lists.newArrayList();
				options.addAll(membersIn);
				options.removeAll(trackedMembers());
				
				for(LivingEntity entity : options)
				{
					BlockPos current = entity.blockPosition();
					current = new BlockPos(current.getX(), guardTarget.getY(), current.getZ());
					
					double dist = current.distSqr(bestPosWorld.offset(lastPos.x, lastPos.y, lastPos.z));
					if(dist < lowest)
					{
						closest = entity;
						lowest = dist;
					}
				}
				
				if(closest instanceof ITreeEntity)
					addTrackedPos(closest, bestPosLocal);
			}
			
			for(LivingEntity entity : trackedMembers())
			{
				BlockPos dest = getTrackedPos(entity).offset(lastPos.x, lastPos.y, lastPos.z);
				dest = new BlockPos(dest.getX(), entity.blockPosition().getY(), dest.getZ());
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.atPos(Mark.GUARD_POS, dest)));
			}
		}
		
		private float flankingUtility(BlockPos pos, Collection<BlockPos> unitPositions, double minSq, double maxSq)
		{
			// Closer to minimum distance to the target, the better
			double length = new Vec3(pos.getX() + 0.5D, 0, pos.getZ() + 0.5D).length();
			float proximity = ActionUtils.getInterpolatedUtility(Math.min(maxSq, length), utilityPlot);
			
			// Further from any teammates, the better
			double minDist = Double.MAX_VALUE;
			double separation = Math.sqrt(maxSq);
			for(BlockPos position : unitPositions)
			{
				double distance = Mth.clamp(pos.distSqr(position), 0, separation);
				if(distance < minDist)
					minDist = distance;
			}
			
			return (float)(minDist * proximity);
		}
		
		public Vec3 lastPosition() { return this.lastPos; }
	}
	
	public static class ActionGuardPos extends ActionFormation
	{
		// Manual curve plot
		private final Map<Double,Float> utilityPlot = new HashMap<>();
		
		private BlockPos guardTarget;
		
		public ActionGuardPos(BlockPos target, double min, double max)
		{
			super(ActionType.GUARD_POS, 1);
			this.guardTarget = target;
			
			this.minDist = min * min;
			this.maxDist = max * max;
			generateUtilityPlot();
			
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : ((size * 0.3F) / targets.size()) / 2F,
					(action,supply) -> new ActionFlank().setComplement((int)Math.ceil(supply * 0.3D))
					));
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : 0.5F,
					(action,supply) -> new ActionBrawl().setComplement((int)Math.ceil(supply * 0.4D))
					));
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Pos", NbtUtils.writeBlockPos(guardTarget));
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			guardTarget = NbtUtils.readBlockPos(compound.getCompound("Pos"));
			generateUtilityPlot();
		}
		
		private void generateUtilityPlot()
		{
			utilityPlot.clear();
			utilityPlot.put(0D, 0F);
			utilityPlot.put(minDist / 2, 0.3F);
			utilityPlot.put(minDist, 1F);
			utilityPlot.put((maxDist + minDist) * 0.5D, 0.9F);
			utilityPlot.put(maxDist, 0F);
		}
		
		public BlockPos targetPoint() { return this.guardTarget; }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			super.tick(membersIn, targetsIn, world);
			
			// Assign one new guard position if there are untracked members
			if(formationPoints().size() < membersIn.size())
			{
				BlockPos bestPos = getNextBestPos(world.getRandom());
				
				// Find closest unassigned member to guard position, and assign
				LivingEntity closest = null;
				double lowest = Double.MAX_VALUE;
				
				List<LivingEntity> options = Lists.newArrayList();
				options.addAll(membersIn);
				options.removeAll(trackedMembers());
				
				for(LivingEntity entity : options)
				{
					BlockPos current = entity.blockPosition();
					current = new BlockPos(current.getX(), guardTarget.getY(), current.getZ());
					double dist = current.distSqr(bestPos);
					if(dist < lowest)
					{
						closest = entity;
						lowest = dist;
					}
				}
				
				if(closest instanceof ITreeEntity)
					addTrackedPos(closest, bestPos);
			}
			
			// Reassert guard positions to tracked members
			for(LivingEntity entity : trackedMembers())
			{
				BlockPos dest = getTrackedPos(entity);
				dest = new BlockPos(dest.getX(), entity.blockPosition().getY(), dest.getZ());
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.atPos(Mark.GUARD_POS, dest)));
			}
		}
		
		/** Identifies a favourable position for a new guard assignment via pathing solve */
		private BlockPos getNextBestPos(RandomSource random)
		{
			Collection<BlockPos> unitPositions = formationPoints();
			
			// Cached utility values of encountered points, to reduce calls to the utility function
			Map<BlockPos, Float> utilityCache = new HashMap<>();
			List<Vec2> movements = List.of
					(
						new Vec2(0, 1),
						new Vec2(0, -1),
						new Vec2(1, 0),
						new Vec2(-1, 0),
						new Vec2(1, 1),
						new Vec2(1, -1),
						new Vec2(-1, 1),
						new Vec2(-1, -1)
					);
			
			// Centre of the 3x3 grid with the best utility found so far
			BlockPos currentPos = guardTarget;
			// Utility of the best 3x3 grid found so far
			float currentUtility = calculateUtility(currentPos, unitPositions, guardTarget, minDist, maxDist);
			utilityCache.put(guardTarget, currentUtility);
			
			List<BlockPos> nextSearch = Lists.newArrayList();
			for(Vec2 move : movements)
			{
				BlockPos offset = guardTarget.offset(move.x, 0, move.y);
				float util = calculateUtility(offset, unitPositions, guardTarget, minDist, maxDist);
				currentUtility += util;
				
				utilityCache.put(offset, util);
				nextSearch.add(offset);
			}
			
			while(!nextSearch.isEmpty())
			{
				List<BlockPos> currentSearch = Lists.newArrayList();
				currentSearch.addAll(nextSearch);
				nextSearch.clear();
				
				for(BlockPos point : currentSearch)
				{
					// Average utility value of the 3x3 grid surrounding this point
					float utility = utilityCache.containsKey(point) ? utilityCache.get(point) : calculateUtility(point, unitPositions, guardTarget, minDist, maxDist);
					utilityCache.put(point, utility);
					for(Vec2 move : movements)
					{
						BlockPos offset = point.offset(move.x, 0, move.y);
						float util = utilityCache.containsKey(offset) ? utilityCache.get(offset) : calculateUtility(offset, unitPositions, guardTarget, minDist, maxDist);
						utilityCache.put(offset, util);
						utility += util;
					}
					
					// If this 3x3 is of greater average value than our current, move here
					if(utility > currentUtility)
					{
						currentPos = point;
						currentUtility = utility;
						
						for(Vec2 move : movements)
							nextSearch.add(point.offset(move.x, 0, move.y));
					}
				}
			}
			
			return currentPos;
		}
		
		private float calculateUtility(BlockPos pos, Collection<BlockPos> unitPositions, BlockPos target, double minSq, double maxSq)
		{
			// Closer to minimum distance to the target, the better
			double toTarget = Math.min(maxSq, target.distSqr(pos));
			float utility = ActionUtils.getInterpolatedUtility(toTarget, utilityPlot);
			
			// Further from any teammates, the better
			if(!unitPositions.isEmpty())
			{
				double minDist = Double.MAX_VALUE;
				for(BlockPos position : unitPositions)
				{
					double distance = Mth.clamp(pos.distSqr(position), 0, maxSq);
					if(distance < minDist)
						minDist = distance;
				}
				utility *= minDist;
			}
			
			return utility;
		}
	}
	
	/** Demo action for forming a regular square formation, military-style */
	public static class ActionGrid extends ActionFormation
	{
		private BlockPos guardTarget;
		private double spacing;
		
		public ActionGrid(BlockPos target, double spaceIn)
		{
			super(ActionType.GRID, 1);
			this.guardTarget = target;
			this.spacing = spaceIn;
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Pos", NbtUtils.writeBlockPos(guardTarget));
			compound.putDouble("Spacing", spacing);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			guardTarget = NbtUtils.readBlockPos(compound.getCompound("Pos"));
			spacing = compound.getDouble("Spacing");
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			if(formationPoints().size() < membersIn.size())
			{
				BlockPos bestPos = getFirstUnoccupiedPosition(membersIn.size());
				
				// Find closest unassigned member to guard position, and assign
				LivingEntity closest = null;
				double lowest = Double.MAX_VALUE;
				
				List<LivingEntity> options = Lists.newArrayList();
				options.addAll(membersIn);
				options.removeAll(trackedMembers());
				
				for(LivingEntity entity : options)
				{
					BlockPos current = entity.blockPosition();
					current = new BlockPos(current.getX(), guardTarget.getY(), current.getZ());
					double dist = current.distSqr(bestPos);
					if(dist < lowest)
					{
						closest = entity;
						lowest = dist;
					}
				}
				
				if(closest instanceof ITreeEntity)
					addTrackedPos(closest, bestPos);
			}
			
			for(LivingEntity entity : trackedMembers())
			{
				BlockPos dest = getTrackedPos(entity);
				dest = new BlockPos(dest.getX(), entity.blockPosition().getY(), dest.getZ());
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.atPos(Mark.GUARD_POS, dest)));
			}
		}
		
		private BlockPos getFirstUnoccupiedPosition(int population)
		{
			Collection<BlockPos> unitPositions = formationPoints();
			
			double range = Math.ceil(Math.sqrt(population));
			range /= 2;
			
			AABB searchArea = new AABB(-range, 0, -range, range, 1, range);
			for(int x=(int)searchArea.minX; x<searchArea.maxX; x++)
				for(int z=(int)searchArea.minZ; z<searchArea.maxZ; z++)
				{
					BlockPos offset = new BlockPos(x * spacing, 0, z * spacing).offset(guardTarget);
					if(!unitPositions.contains(offset))
						return offset;
				}
			return guardTarget;
		}
	}
	
	public static class ActionAggroFlank extends GroupAction
	{
		private final ActionBrawl brawl;
		private final ActionFlank flank;
		
		public ActionAggroFlank()
		{
			super(ActionType.AGGRO_FLANK, 3);
			this.brawl = new ActionBrawl();
			this.flank = new ActionFlank();
		}
		
		protected boolean start(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			List<LivingEntity> brawlMembers = Lists.newArrayList();
			List<LivingEntity> flankMembers = Lists.newArrayList();
			int i=0;
			for(LivingEntity living : membersIn)
				if(i++%3 == 0)
					brawlMembers.add(living);
				else
					flankMembers.add(living);
			
			brawl.start(brawlMembers, targetsIn, world);
			flank.start(flankMembers, targetsIn, world);
			membersIn.clear();
			return true;
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			if(brawl.isComplete() && flank.isComplete() || membersIn.size() < 2)
				markComplete();
			
			List<LivingEntity> brawlMembers = Lists.newArrayList();
			List<LivingEntity> flankMembers = Lists.newArrayList();
			int i=0;
			for(LivingEntity living : membersIn)
				if(i++%2 == 0)
					brawlMembers.add(living);
				else
					flankMembers.add(living);
			
			if(!brawl.isComplete())
				brawl.tick(brawlMembers, targetsIn, world);
			
			if(!flank.isComplete())
				flank.tick(membersIn, targetsIn, world);
		}
	}
	
	public static class ActionBrawl extends GroupAction
	{
		public ActionBrawl() { super(ActionType.BRAWL, 1); }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			if(targetsIn.isEmpty())
			{
				markComplete();
				return;
			}
			
			// Members in our complement that we've successfully given a target
			List<LivingEntity> haveTarget = Lists.newArrayList();
			// Members in our complement that we no didn't find a target for, so need to move to someone who has one
			
			List<LivingEntity> shouldBackUp = Lists.newArrayList();
			for(LivingEntity member : membersIn)
			{
				if(!(member instanceof ITreeEntity))
					continue;
				
				LivingEntity bestTarget = null;
				float bestUtility = Float.MAX_VALUE;
				for(LivingEntity target : targetsIn)
				{
					if(!member.canAttack(target) || member instanceof Mob && ((Mob)member).hasLineOfSight(target))
						continue;
					
					float utility = getUtility(target, member);
					if(utility < bestUtility)
					{
						bestUtility = utility;
						bestTarget = target;
					}
				}
				
				if(bestTarget != null)
				{
					Whiteboard.tryGetWhiteboard(member).setCommands(CommandStack.single(Mark.onEntity(Mark.ATTACK, bestTarget)));
					haveTarget.add(member);
				}
				else
					shouldBackUp.add(member);
			}
			
			if(!shouldBackUp.isEmpty() && !haveTarget.isEmpty())
				for(LivingEntity member : shouldBackUp)
				{
					LivingEntity bestMember = null;
					double minDist = Double.MAX_VALUE;
					for(LivingEntity teammate : haveTarget)
					{
						double dist = teammate.distanceToSqr(member);
						if(dist < minDist)
						{
							bestMember = teammate;
							minDist = dist;
						}
					}
					
					Whiteboard.tryGetWhiteboard(member).setCommands(CommandStack.single(Mark.onEntity(Mark.GOTO_MOB, bestMember)));
				}
		}
		
		private float getUtility(LivingEntity target, LivingEntity member)
		{
			// The closer to the member, the better
			float distance = Mth.clamp(1F - (float)(target.distanceTo(member) / 16D), 0F, 1F);
			return distance * (1F - ActionUtils.assessDurability(target));
		}
	}
	
	public static class ActionFlank extends ActionFormation
	{
		private Map<Double, Float> utilityPlot = new HashMap<>();
		
		/** The mean position of all targets */
		private Vec3 targetCenter = Vec3.ZERO;
		
		public ActionFlank()
		{
			super(ActionType.FLANK, 2);
			this.minDist = Double.MAX_VALUE;
			this.maxDist = Double.MAX_VALUE;
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			
			CompoundTag center = new CompoundTag();
			center.putDouble("X", targetCenter.x);
			center.putDouble("Y", targetCenter.y);
			center.putDouble("Z", targetCenter.z);
			compound.put("Center", center);
			
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			
			CompoundTag center = compound.getCompound("Center");
			this.targetCenter = new Vec3(center.getDouble("X"), center.getDouble("Y"), center.getDouble("Z"));
		}
		
		public Vec3 getTargetPoint() { return this.targetCenter; }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			super.tick(membersIn, targetsIn, world);
			if(targetsIn.isEmpty())
				markComplete();
			
			if(isComplete())
				return;
			
			/**
			 * Calculate center of target group
			 * Calculate width of target group as greatest distance from any target to group center
			 * Set minDist as group width + 2D if that is lower than minDist's current value 
			 * Apply flanking logic (see GuardPos) to anyone whose attack is on cooldown
			 * Anyone not on cooldown attacks best target (see Brawl)
			 */
			
			// Calculate center of targets based on weighted mean of their position
			updateTargetCenter(targetsIn);
			
			// Calculate flanking radius based on spread of targets and the highest attack range among them
			updateUtilityPlot(targetsIn);
			
			if(formationPoints().size() < membersIn.size())
			{
				BlockPos bestPos = getNextBestPos(world.getRandom());
				
				// Find closest unassigned member to guard position, and assign
				List<LivingEntity> options = Lists.newArrayList();
				options.addAll(membersIn);
				options.removeAll(trackedMembers());
				
				LivingEntity closest = null;
				double lowest = Double.MAX_VALUE;
				for(LivingEntity entity : options)
				{
					BlockPos current = entity.blockPosition();
					current = new BlockPos(current.getX(), targetCenter.y, current.getZ());
					double dist = current.distSqr(bestPos);
					if(dist < lowest)
					{
						closest = entity;
						lowest = dist;
					}
				}
				
				if(closest instanceof ITreeEntity)
					addTrackedPos(closest, bestPos);
			}
			
			// Anyone not on cooldown attacks best target (see Brawl), else return to formation
			for(LivingEntity member : trackedMembers())
			{
				if(!(member instanceof ITreeEntity))
					continue;
				
				Whiteboard<?> storage = Whiteboard.tryGetWhiteboard(member);
				if(storage == null)
					continue;
				
				updateMember(member, storage, targetsIn, world.getRandom());
			}
		}
		
		private void updateMember(LivingEntity member, Whiteboard<?> storage, List<LivingEntity> targetsIn, RandomSource random)
		{
			if(storage.getTimer(MobWhiteboard.MOB_MELEE_COOLDOWN) > 0)
			{
				if(storage.hasCommands())
					return;
				
				BlockPos dest = getTrackedPos(member);
				dest = new BlockPos(dest.getX(), member.blockPosition().getY(), dest.getZ());
				storage.setCommands(new CommandStack(Mark.CEASEFIRE.makeCommand(), Mark.atPos(Mark.GUARD_POS, dest)));
			}
			else if(storage.getEntity(MobWhiteboard.AI_TARGET) == null && random.nextInt(20) == 0)
			{
				LivingEntity bestTarget = null;
				
				if(targetsIn.size() == 1)
					bestTarget = targetsIn.get(0);
				else
				{
					float utility = Float.MIN_VALUE;
					for(LivingEntity target : targetsIn)
					{
						float util = getCombatUtility(target, member);
						if(utility < util)
						{
							utility = util;
							bestTarget = target;
						}
					}
				}
				
				if(bestTarget != null)
					storage.setCommands(CommandStack.single(Mark.onEntity(Mark.ATTACK, bestTarget)));
			}
		}
		
		/** Adjusts the target center position based on a weighted average of the given targets */
		private void updateTargetCenter(List<LivingEntity> targetsIn)
		{
			Vec3 weightedPos = IMobGroup.getWeightedPosition(targetsIn);
			if(weightedPos.distanceTo(targetCenter) > 5D)
			{
				targetCenter = weightedPos;
				clearFormation();
			}
		}
		
		/** Updates the utility plot based on the spread of the given targets */
		private void updateUtilityPlot(List<LivingEntity> targetsIn)
		{
			if(utilityPlot.isEmpty())
				generateUtilityPlot();
			
			double radius = Double.MAX_VALUE;
			double reach = Double.MIN_VALUE;
			for(LivingEntity target : targetsIn)
			{
				// Target distance to target center
				double dist = target.distanceToSqr(targetCenter);
				if(dist < radius)
					radius = dist;
				
				// Target attack range
				double targetReach = target.getType() == EntityType.PLAYER ? ((Player)target).getAttackRange() : Math.sqrt(target.getBbWidth() * 2F * target.getBbWidth());
				if(targetReach > reach)
					reach = targetReach;
			}
			reach += 1D;	// Add one block of distance just to keep our central line somewhat out of harm's way
			radius += reach;
			
			// Only ever allow flanking radius to decrease, never increase, to force targets together
			double minimumRadius = 3D;
			minimumRadius *= minimumRadius;
			
			radius = Math.max(minimumRadius, radius * radius);
			if(radius < this.minDist)
			{
				this.minDist = radius;
				this.maxDist = minDist + (4D * 4D);
				generateUtilityPlot();
				clearFormation();
			}
		}
		
		private void generateUtilityPlot()
		{
			utilityPlot.clear();
			utilityPlot.put(0D, 0F);
			utilityPlot.put(minDist / 2, 0.3F);
			utilityPlot.put(minDist, 1F);
			utilityPlot.put((maxDist + minDist) * 0.5D, 0.9F);
			utilityPlot.put(maxDist, 0F);
		}
		
		private BlockPos getNextBestPos(RandomSource random)
		{
			Collection<BlockPos> unitPositions = formationPoints();
			BlockPos guardTarget = new BlockPos(targetCenter.x, targetCenter.y, targetCenter.z);
			
			// Cached utility values of encountered points, to reduce calls to the utility function
			Map<BlockPos, Float> utilityCache = new HashMap<>();
			List<Vec2> movements = List.of
					(
						new Vec2(0, 1),
						new Vec2(0, -1),
						new Vec2(1, 0),
						new Vec2(-1, 0),
						new Vec2(1, 1),
						new Vec2(1, -1),
						new Vec2(-1, 1),
						new Vec2(-1, -1)
					);
			
			// Centre of the 3x3 grid with the best utility found so far
			BlockPos currentPos = guardTarget;
			// Utility of the best 3x3 grid found so far
			float currentUtility = calculateUtility(currentPos, unitPositions, guardTarget, minDist, maxDist, utilityPlot);
			utilityCache.put(guardTarget, currentUtility);
			
			List<BlockPos> nextSearch = Lists.newArrayList();
			for(Vec2 move : movements)
			{
				BlockPos offset = guardTarget.offset(move.x, 0, move.y);
				float util = calculateUtility(offset, unitPositions, guardTarget, minDist, maxDist, utilityPlot);
				currentUtility += util;
				
				utilityCache.put(offset, util);
				nextSearch.add(offset);
			}
			
			while(!nextSearch.isEmpty())
			{
				List<BlockPos> currentSearch = Lists.newArrayList();
				currentSearch.addAll(nextSearch);
				nextSearch.clear();
				
				for(BlockPos point : currentSearch)
				{
					// Average utility value of the 3x3 grid surrounding this point
					float utility = utilityCache.containsKey(point) ? utilityCache.get(point) : calculateUtility(point, unitPositions, guardTarget, minDist, maxDist, utilityPlot);
					utilityCache.put(point, utility);
					for(Vec2 move : movements)
					{
						BlockPos offset = point.offset(move.x, 0, move.y);
						float util = utilityCache.containsKey(offset) ? utilityCache.get(offset) : calculateUtility(offset, unitPositions, guardTarget, minDist, maxDist, utilityPlot);
						utilityCache.put(offset, util);
						utility += util;
					}
					
					// If this 3x3 is of greater average value than our current, move here
					if(utility > currentUtility)
					{
						currentPos = point;
						currentUtility = utility;
						
						for(Vec2 move : movements)
							nextSearch.add(point.offset(move.x, 0, move.y));
					}
				}
			}
			
			return currentPos;
		}
		
		private float calculateUtility(BlockPos pos, Collection<BlockPos> unitPositions, BlockPos target, double minSq, double maxSq, Map<Double,Float> utilityPlot)
		{
			// Closer to minimum distance to the target, the better
			double toTarget = Math.min(maxSq, target.distSqr(pos));
			float utility = ActionUtils.getInterpolatedUtility(toTarget, utilityPlot);
			
			// Further from any teammates, the better
			if(!unitPositions.isEmpty())
			{
				double minDist = Double.MAX_VALUE;
				for(BlockPos position : unitPositions)
				{
					double distance = Mth.clamp(pos.distSqr(position), 0, maxSq);
					if(distance < minDist)
						minDist = distance;
				}
				utility *= minDist;
			}
			
			return utility;
		}
		
		private float getCombatUtility(LivingEntity target, LivingEntity member)
		{
			// The closer to the member, the better
			float distance = Mth.clamp(1F - (float)(target.distanceTo(member) / 16D), 0F, 1F);
			// The lower the target's health already is, the better, because only the last hit point matters
			float health = 1F - (target.getHealth() / 20F);
			float armour = 1F - (float)(target.getAttributeValue(Attributes.ARMOR) / 20D);
			return distance * health * armour;
		}
	}
	
	public static class ActionPickUp extends GroupAction
	{
		protected Predicate<ItemStack> qualifier = Predicates.alwaysTrue();
		private BlockPos minPos, maxPos;
		
		protected ActionPickUp(ResourceLocation typeIn, BlockPos minPosIn, BlockPos maxPosIn)
		{
			super(typeIn, 1);
			this.minPos = minPosIn;
			this.maxPos = maxPosIn;
		}
		
		public ActionPickUp(BlockPos minPosIn, BlockPos maxPosIn)
		{
			this(ActionType.PICK_UP, minPosIn, maxPosIn);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.put("Min", NbtUtils.writeBlockPos(minPos));
			compound.put("Max", NbtUtils.writeBlockPos(maxPos));
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			this.minPos = NbtUtils.readBlockPos(compound.getCompound("Min"));
			this.maxPos = NbtUtils.readBlockPos(compound.getCompound("Max"));
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			List<LivingEntity> available = Lists.newArrayList();
			available.addAll(membersIn);
			available.removeIf((living) -> !(living instanceof ITreeEntity));
			
			for(ItemEntity item : world.getEntitiesOfClass(
					ItemEntity.class, 
					new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1D, maxPos.getY() + 1D, maxPos.getZ() + 1D), 
					(item) -> item.isOnGround() && !item.hasPickUpDelay() && qualifier.apply(item.getItem())))
			{
				LivingEntity closest = null;
				double minDist = Double.MAX_VALUE;
				for(LivingEntity member : available)
				{
					double dist = member.distanceToSqr(item);
					if(dist < minDist)
					{
						minDist = dist;
						closest = member;
					}
				}
				
				if(closest != null)
				{
					Whiteboard.tryGetWhiteboard(closest).setCommands(CommandStack.single(Mark.onEntity(Mark.PICK_UP, item)));
					available.remove(closest);
				}
			}
		}
	}
	
	/** Variant of pickup used by farms which ignores seeds and bone meal */
	public static class ActionPickUpNonSeeds extends ActionPickUp
	{
		public ActionPickUpNonSeeds(BlockPos minPosIn, BlockPos maxPosIn)
		{
			super(ActionType.PICK_UP_NON_SEEDS, minPosIn, maxPosIn);
			this.qualifier = (stack) -> !ActionFarm.IS_SEED.apply(stack) && !stack.is(Items.BONE_MEAL);
		}
	}
	
	public static class ActionGeneric extends GroupAction
	{
		public ActionGeneric()
		{
			super(ActionType.GENERIC, -1);
			
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : ((size * 0.3F) / targets.size()) / 2F,
					(action,supply) -> new ActionFlank().setComplement(-1)
					));
			addOption(new ActionOption(
					(targets,action,size) -> targets.isEmpty() ? -1F : 0.5F,
					(action,supply) -> new ActionBrawl().setComplement(-1)
					));
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world){ }
	}
}
