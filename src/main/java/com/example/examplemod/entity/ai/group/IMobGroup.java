package com.example.examplemod.entity.ai.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.google.common.base.Predicates;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface IMobGroup
{
	public ResourceLocation getKey();
	
	/** Returns the whiteboard of this group */
	public Whiteboard<?> getWhiteboard();
	
	/** Returns the live map of members to their UUIDs */
	public Map<UUID, LivingEntity> membership();
	
	/** Returns a list of known hostile targets */
	public List<LivingEntity> targets();
	
	/** Returns true if this group should take commands from the given entity */
	public boolean shouldListenTo(LivingEntity entity);
	
	/** Registers a new group of the same class with the given members added to it from this one */
	@Nullable
	public IMobGroup split(LivingEntity... membersIn);
	
	/** Updates any internal logic of this group, such as AI */
	public void tick(MinecraftServer server);
	
	/** Returns true if this group has changed in a way that requires synchronising to clients */
	public boolean isDirty();
	public default void setDirty() { setDirty(true); }
	public void setDirty(boolean bool);
	
	/** Delivers the given command stack to all members of this group */
	public default void giveCommandToAll(CommandStack stack)
	{
		members().forEach((member) ->
		{
			if(member != null && member instanceof ITreeEntity)
				Whiteboard.tryGetWhiteboard(member).setCommands(stack);
		});
	}
	
	/** Clears all extant commands from all members */
	public default void clearCommands() { giveCommandToAll(null); }
	
	/** Stores the group in NBT data, primarily in the form of a series of member UUIDs */
	public default CompoundTag saveToNbt(CompoundTag compound)
	{
		saveMemberIds(compound);
		saveAction(compound);
		return compound;
	}
	
	public default void loadFromNbt(CompoundTag compound)
	{
		loadMemberIds(compound);
		loadAction(compound);
	}
	
	public default void saveMemberIds(CompoundTag compound)
	{
		ListTag ids = new ListTag();
		membership().keySet().forEach((uuid) -> ids.add(NbtUtils.createUUID(uuid)));
		compound.put("Members", ids);
	}
	
	public default void loadMemberIds(CompoundTag compound)
	{
		loadMemberIds(compound.getList("Members", Tag.TAG_INT_ARRAY));
	}
	
	public default void loadMemberIds(@Nullable ListTag ids)
	{
		if(ids == null || ids.isEmpty())
			return;
		ids.forEach((tag) -> 
		{
			UUID id = NbtUtils.loadUUID(tag);
			if(id != null)
				membership().put(id, null);
		});
	}
	
	public default CompoundTag saveAction(CompoundTag compound)
	{
		if(!hasAction())
			return compound;
		
		compound.put("Action", getAction().storeInNbt(new CompoundTag()));
		return compound;
	}
	
	public default void loadAction(CompoundTag compound)
	{
		if(!compound.contains("Action", Tag.TAG_COMPOUND))
			return;
		
		CompoundTag actionData = compound.getCompound("Action");
		GroupAction action = ActionType.createActionFromNbt(new ResourceLocation(actionData.getString("Type")), actionData);
		if(action != null)
			setAction(action);
	}
	
	/** Returns true if this group should not be stored, usually because it's empty */
	public default boolean shouldNotPersist() { return isEmpty(); }
	/** Returns true if this group has no members */
	public default boolean isEmpty() { return membership().isEmpty(); }
	/** Returns the number of members of this group */
	public default int size() { return membership().size(); }
	
	/** Returns true if the given object is a member of this group */
	public default boolean isMember(@Nullable UUID objIn) { return objIn != null && membership().containsKey(objIn); }
	public default boolean isMember(@Nullable LivingEntity objIn)
	{
		if(objIn != null)
		{
			UUID id = objIn.getUUID();
			if(isMember(id))
			{
				// Cache entity in membership map for ease of reference
				membership().put(id, objIn);
				return true;
			}
		}
		return false;
	}
	
	public default boolean isMemberCached(UUID objIn) { return membership().get(objIn) != null; }
	
	public default void add(@Nullable LivingEntity objIn)
	{
		if(objIn != null && !isMember(objIn))
		{
			membership().put(objIn.getUUID(), objIn);
			setDirty();
		}
	}
	public default void add(@Nullable UUID objIn) { membership().put(objIn, null); }
	
	public default void remove(@Nullable LivingEntity objIn) { if(objIn != null) remove(objIn.getUUID()); }
	public default void remove(@Nullable UUID objIn)
	{
		if(membership().remove(objIn) != null)
			setDirty();
	}
	
	/** Returns a non-null list of all cached members of this group */
	public default List<LivingEntity> members()
	{
		List<LivingEntity> list = Lists.newArrayList();
		list.addAll(membership().values());
		// Membership map may include null values for members that haven't been identified post-load
		list.removeIf(Predicates.isNull());
		list.removeIf((entity) -> { return !entity.isAlive() || !entity.isAddedToWorld(); });
		return list;
	}
	
	/** Returns the general position of this group, usually based on its cached members */
	public default Vec3 position()
	{
		return getMeanPosition(members());
	}
	
	public static Vec3 getMeanPosition(List<LivingEntity> entities)
	{
		double x = 0D, y = 0D, z = 0D;
		for(LivingEntity mob : entities)
		{
			x += mob.xo;
			y += mob.yo;
			z += mob.zo;
		}
		x /= entities.size();
		y /= entities.size();
		z /= entities.size();
		return new Vec3(x, y, z);
	}
	
	public static Vec3 getWeightedPosition(List<LivingEntity> entities)
	{
		if(entities.isEmpty())
			return Vec3.ZERO;
		
		int size = entities.size();
		if(size == 1)
			return entities.get(0).position();
		
		// Lowest average distance between targets
		double minDist = Double.MAX_VALUE;
		
		// Map of targets to their average distance to all other targets
		Map<Entity, Double> distances = new HashMap<>();
		for(Entity member : entities)
		{
			double avgDist = 0D;
			for(Entity targetB : entities)
				if(targetB != member)
					avgDist += member.distanceToSqr(targetB);
			avgDist /= size - 1;
			if(avgDist < minDist)
				minDist = avgDist;
			
			distances.put(member, avgDist);
		}
		
		// Weighted position, reflecting the area with the highest density of targets
		double weightSum = 0D;
		Vec3 weightedPos = Vec3.ZERO;
		for(Entity member : entities)
		{
			double weight = minDist / distances.get(member);
			weightedPos = weightedPos.add(member.position().scale(weight));
			weightSum += weight;
		}
		return weightedPos.scale(1 / weightSum);
	}
	
	/** Returns true if this group has any living attack target */
	public default boolean hasTarget() { return !targets().isEmpty(); }
	@Nullable
	public default Entity target(int index) { return targets().get(index); }
	@Nullable
	public default Entity target() { return target(0); }
	
	public default boolean hasAction() { return getAction() != null; }
	public void setAction(GroupAction actionIn);
	public default GroupAction getAction() { return null; }
	
	public default void updateGroupAction()
	{
		if(!hasAction())
			return;
		else if(getAction().isComplete())
		{
			setAction(null);
			return;
		}
		
		List<LivingEntity> members = members();
		if(!members.isEmpty())
		{
			getAction().update(members, targets(), members.get(0).getLevel());
			setDirty();
		}
	}
}
