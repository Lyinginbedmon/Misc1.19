package com.example.examplemod.entity.ai.group;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class GroupAction
{
	private final ResourceLocation registryName;
	
	private Status status = Status.STARTING;
	private final int minComplement;
	private int complement = -1;
	
	private List<GroupAction> children = Lists.newArrayList();
	
	protected GroupAction(ResourceLocation nameIn, int complementIn)
	{
		this.registryName = nameIn;
		this.minComplement = complementIn;
	}
	
	public ResourceLocation getRegistryName() { return this.registryName; }
	
	/** Returns how many members this action is allowed to use (or -1 if it can use all members provided) */
	public int getComplement() { return this.complement; }
	public void setComplement(int par1Int) { this.complement = par1Int; }
	
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
			members.addAll(membersIn);
		else if(getComplement() > 0)
			members.addAll(membersIn.subList(0, getComplement() - 1));
		
		// Remove handled members from the input list to prevent two actions trying to command the same member
		membersIn.removeAll(members);
		updateWithComplement(members, targetsIn, world);
	}
	
	private final void updateWithComplement(List<LivingEntity> members, List<LivingEntity> targetsIn, Level world)
	{
		switch(this.status)
		{
			case STARTING:
				if(start(members, targetsIn, world))
					this.status = Status.RUNNING;
				break;
			case RUNNING:
				children.forEach((child) -> child.update(members, targetsIn, world));
				children.removeIf((action) -> action.isComplete());
				tick(members, targetsIn, world);
				break;
			case COMPLETE:
				break;
		}
	}
	
	public final void addChild(GroupAction childIn) { this.children.add(childIn); }
	
	/** Returns the utility value associated with a given plotted graph */
	protected float getInterpolatedUtility(double distance, double minSq, double maxSq, Map<Double,Float> plot)
	{
		Pair<Double,Double> bounds = getPlotBounds(distance, plot.keySet());
		double keyUnder = bounds.getLeft();
		double keyOver = bounds.getRight();
		if(keyUnder == keyOver)
			return plot.get(keyUnder);
		
		double keySep = keyOver - keyUnder;
		double prog = (distance - keyUnder) / keySep;
		
		float lastVal = plot.get(keyUnder);
		float nextVal = plot.get(keyOver);
		float valSep = nextVal - lastVal;
		
		return Mth.clamp(lastVal + (float)(valSep * prog), 0F, 1F);
	}
	
	protected Pair<Double,Double> getPlotBounds(double distance, Set<Double> values) throws NullPointerException
	{
		if(values.isEmpty())
			throw new NullPointerException();
		
		double keyUnder = 0D, keyOver = Double.MIN_VALUE;
		for(double value : values)
			if(value > keyOver)
				keyOver = value;
		
		for(double value : values)
		{
			if(value > keyUnder && value <= distance)
				keyUnder = value;
			
			if(value < keyOver && value >= distance)
				keyOver = value;
		}
		
		return Pair.of(keyUnder, keyOver);
	}
	
	protected static double getAttackReachSqr(Entity ent, Entity member)
	{
		if(ent.getType() == EntityType.PLAYER)
			return ((Player)ent).getAttackRange() + member.getBbWidth();
		else
			return (double)(ent.getBbWidth() * 2F * ent.getBbWidth() + member.getBbWidth());
	}
	
	/** Stores the action in NBT data for transmission and/or storage */
	public final CompoundTag storeInNbt(CompoundTag compound)
	{
		compound.putString("Type", getRegistryName().toString());
		compound.putInt("Complement", getComplement());
		compound.putInt("Status", status().ordinal());
		compound.put("Data", saveToNbt(new CompoundTag()));
		ListTag childActions = new ListTag();
		for(GroupAction child : children)
			childActions.add(child.storeInNbt(new CompoundTag()));
		compound.put("Children", childActions);
		return compound;
	}
	
	protected CompoundTag saveToNbt(CompoundTag compound) { return compound; }
	
	public void loadFromNbt(CompoundTag compound) { }
	
	@Nullable
	protected static LivingEntity tryFindEntityNearby(@Nullable UUID uuidIn, List<LivingEntity> membersIn)
	{
		if(uuidIn != null && !membersIn.isEmpty())
			for(LivingEntity member : membersIn)
				for(LivingEntity entity : member.getLevel().getEntitiesOfClass(LivingEntity.class, member.getBoundingBox().inflate(16D)))
					if(entity.isAddedToWorld() && entity.getUUID().equals(uuidIn))
						return entity;
		return null;
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
	
	protected static class MemberData
	{
		private LivingEntity entity;
		private UUID entityID;
		
		public MemberData(@Nullable LivingEntity memberIn)
		{
			entity = memberIn;
			if(memberIn != null)
				entityID = memberIn.getUUID();
		}
		
		public MemberData(UUID idIn)
		{
			entityID = idIn;
		}
		
		public boolean matches(@Nullable LivingEntity entityIn)
		{
			if(entityIn != null && (entity() == entityIn || entityIn.getUUID().equals(uuid())))
			{
				entity = entityIn;
				return true;
			}
			return false;
		}
		
		/** Returns true if the specified entity has been matched since instantiation */
		public boolean cached() { return this.entity != null; }
		
		public UUID uuid() { return this.entityID; }
		public LivingEntity entity() { return this.entity; }
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
		}
		
		protected boolean start(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
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
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
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
			sortConsignment(blocks, recipient).forEach((pos) -> stack.append(new MobCommand(Mark.MINE, pos))); 
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
	
	public static class ActionFollow extends GroupAction
	{
		private LivingEntity followTarget;
		private UUID followUUID = null;
		
		private double minDist;
		private double maxDist;
		
		public ActionFollow(@Nullable LivingEntity target, double min, double max)
		{
			super(ActionType.FOLLOW, 1);
			this.followTarget = target;
			if(target != null)
				this.followUUID = target.getUUID();
			
			this.minDist = min;
			this.maxDist = max;
		}
		
		public ActionFollow(LivingEntity target, double distA)
		{
			this(target, distA, distA);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			compound.putUUID("UUID", followUUID);
			compound.putDouble("Min", minDist);
			compound.putDouble("Max", maxDist);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			followTarget = null;
			followUUID = compound.getUUID("UUID");
			minDist = compound.getDouble("Min");
			maxDist = compound.getDouble("Max");
		}
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			if(this.followTarget != null)
				for(LivingEntity member : membersIn)
				{
					double dist = member.distanceToSqr(followTarget);
					if(member instanceof PathfinderMob && member instanceof ITreeEntity)
					{
						Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
						if(board != null)
							if(!board.hasCommands() && dist > (maxDist * maxDist))
								board.setCommands(CommandStack.single(Mark.GOTO_MOB, this.followTarget));
							else if(dist < (minDist * minDist) && ((PathfinderMob)member).getNavigation().isInProgress())
								board.setCommands(CommandStack.single(Mark.STOP_MOVING));
					}
				}
			else
				this.followTarget = tryFindEntityNearby(this.followUUID, membersIn);
		}
	}
	
	public static abstract class ActionFormation extends GroupAction
	{
		protected double minDist;
		protected double maxDist;
		
		// List of block positions either occupied or attempting to be occupied by members of this group
		private Map<MemberData, BlockPos> guardFormation = new HashMap<>();
		
		protected ActionFormation(ResourceLocation nameIn, int complementIn)
		{
			super(nameIn, complementIn);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			compound.putDouble("Min", minDist);
			compound.putDouble("Max", maxDist);
			
			ListTag formationData = new ListTag();
			guardFormation.forEach((data,pos) -> 
			{
				CompoundTag tag = new CompoundTag();
				tag.putUUID("UUID", data.uuid());
				tag.put("Pos", NbtUtils.writeBlockPos(pos));
				formationData.add(tag);
			});
			compound.put("Formation", formationData);
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			minDist = compound.getDouble("Min");
			maxDist = compound.getDouble("Max");
			
			guardFormation.clear();
			ListTag formationData = compound.getList("Formation", Tag.TAG_COMPOUND);
			for(int i=0; i<formationData.size(); i++)
			{
				CompoundTag tag = formationData.getCompound(i);
				BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("Pos"));
				MemberData data = new MemberData(tag.getUUID("UUID"));
				guardFormation.put(data, pos);
			}
		}
		
		public Collection<BlockPos> formationPoints() { return guardFormation.values(); }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			// Recache tracked members post-boot
			for(LivingEntity member : membersIn)
				getTrackedPos(member);
		}
		
		protected void addTrackedPos(LivingEntity entity, BlockPos pos)
		{
			guardFormation.put(new MemberData(entity), pos);
		}
		
		protected void removeTrackedEntity(@Nullable LivingEntity entity)
		{
			if(entity == null)
				return;
			
			MemberData entry = null;
			for(MemberData key : guardFormation.keySet())
				if(key.matches(entity))
				{
					entry = key;
					break;
				}
			if(entry != null)
				guardFormation.remove(entry);
		}
		
		protected BlockPos getTrackedPos(@Nullable LivingEntity entity)
		{
			if(entity != null)
				for(MemberData data : guardFormation.keySet())
					if(data.matches(entity))
						return guardFormation.get(data);
			return BlockPos.ZERO;
		}
		
		protected List<LivingEntity> trackedMembers()
		{
			List<LivingEntity> tracked = Lists.newArrayList();
			guardFormation.keySet().forEach((data) -> { if(data.cached()) tracked.add(data.entity()); });
			return tracked;
		}
	}
	
	public static class ActionGuardMob extends ActionFormation
	{
		// Manual curve plot
		private final Map<Double,Float> utilityPlot = new HashMap<>();
		
		private LivingEntity guardTarget;
		private UUID guardUUID;
		
		private BlockPos lastPos = BlockPos.ZERO;
		private int rethinkTicks = 0;
		
		public ActionGuardMob(@Nullable LivingEntity target, double min, double max)
		{
			super(ActionType.GUARD_MOB, 1);
			this.guardTarget = target;
			if(target != null)
			{
				this.guardUUID = target.getUUID();
				this.lastPos = target.blockPosition();
			}
			
			this.minDist = min * min;
			this.maxDist = max * max;
			generateUtilityPlot();
		}
		
		public ActionGuardMob(LivingEntity target, double distA)
		{
			this(target, distA, distA);
		}
		
		public CompoundTag saveToNbt(CompoundTag compound)
		{
			super.saveToNbt(compound);
			compound.putUUID("UUID", guardUUID);
			compound.put("Pos", NbtUtils.writeBlockPos(lastPos));
			return compound;
		}
		
		public void loadFromNbt(CompoundTag compound)
		{
			super.loadFromNbt(compound);
			guardTarget = null;
			guardUUID = compound.getUUID("UUID");
			lastPos = NbtUtils.readBlockPos(compound.getCompound("Pos"));
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
			
			if(guardTarget == null)
			{
				guardTarget = tryFindEntityNearby(this.guardUUID, membersIn);
				return;
			}
			else if(!guardTarget.isAlive())
			{
				markComplete();
				return;
			}
			
			if(guardTarget.blockPosition().distSqr(lastPos) > minDist)
				lastPos = guardTarget.blockPosition();
			
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
					
					double dist = current.distSqr(bestPosWorld.offset(lastPos));
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
				BlockPos dest = getTrackedPos(entity).offset(lastPos);
				dest = new BlockPos(dest.getX(), entity.blockPosition().getY(), dest.getZ());
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.GUARD_POS, dest));
			}
		}
		
		private float flankingUtility(BlockPos pos, Collection<BlockPos> unitPositions, double minSq, double maxSq)
		{
			// Closer to minimum distance to the target, the better
			double length = new Vec3(pos.getX() + 0.5D, 0, pos.getZ() + 0.5D).length();
			float proximity = getInterpolatedUtility(Math.min(maxSq, length), minSq, maxSq, utilityPlot);
			
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
		
		public BlockPos lastPosition() { return this.lastPos; }
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
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.GUARD_POS, dest));
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
			float utility = getInterpolatedUtility(toTarget, minSq, maxSq, utilityPlot);
			
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
				Whiteboard.tryGetWhiteboard(entity).setCommands(CommandStack.single(Mark.GUARD_POS, dest));
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
					Whiteboard.tryGetWhiteboard(bestTarget).setCommands(CommandStack.single(Mark.ATTACK, bestTarget));
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
					
					Whiteboard.tryGetWhiteboard(member).setCommands(CommandStack.single(Mark.GOTO_MOB, bestMember));
				}
		}
		
		private float getUtility(LivingEntity target, LivingEntity member)
		{
			// The closer to the member, the better
			float distance = Mth.clamp(1F - (float)(target.distanceTo(member) / 16D), 0F, 1F);
			// The lower the target's health already is, the better, because only the last hit point matters
			float health = 1F - (target.getHealth() / 20F);
			float armour = 1F - (float)(target.getAttributeValue(Attributes.ARMOR) / 20D);
			return distance * health * armour;
		}
	}
	
	public static class ActionFlank extends GroupAction
	{
		/** The mean position of all targets */
		private Vec3 targetCenter = null;
		/** The lowest distance of any target to the target center */
		private double minDist = Double.MAX_VALUE;
		
		public ActionFlank() { super(ActionType.FLANK, 2); }
		
		protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
		{
			// TODO Auto-generated method stub
			
			/**
			 * Calculate center of target group
			 * Calculate width of target group as greatest distance from any target to group center
			 * Set minDist as group width + 2D if that is lower than minDist's current value 
			 * Apply flanking logic (see GuardPos) to anyone whose attack is on cooldown
			 * Anyone not on cooldown attacks best target (see Brawl)
			 */
			
			// Calculate center of targets based on mean of their position
			targetCenter = null;
			for(LivingEntity target : targetsIn)
				if(targetCenter == null)
					targetCenter = target.position();
				else
					targetCenter = targetCenter.add(target.position());
			targetCenter = targetCenter.scale(1 / targetsIn.size());
			
			// Calculate flanking radius based on spread of targets and the highest attack range among them
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
			reach += 1D;
			radius += reach;
			
			// Only ever allow flanking radius to decrease, never increase, to force targets together
			if(radius < this.minDist)
				this.minDist = radius;
			
			
		}
	}
}
