package com.example.examplemod.entity.ai.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.GroupWhiteboard;
import com.example.examplemod.entity.ai.group.action.GroupAction;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class GroupGeneric implements IMobGroup
{
	private Map<UUID, LivingEntity> memberMap = new HashMap<>();
	private GroupWhiteboard<ITreeEntity> storage = new GroupWhiteboard<ITreeEntity>();
	private List<LivingEntity> targets = Lists.newArrayList();
	
	private GroupAction currentAction = new GroupAction.ActionGeneric();
	
	private boolean dirty = false;
	
	public GroupGeneric(PathfinderMob... membersIn)
	{
		for(PathfinderMob member : membersIn)
			add(member);
	}
	
	public ResourceLocation getKey(){ return GroupType.GENERIC; }
	
	public boolean isDirty() { return this.dirty; }
	
	public void setDirty(boolean bool) { this.dirty = bool; }
	
	public boolean shouldListenTo(LivingEntity entity) { return false; }
	
	public Map<UUID, LivingEntity> membership(){ return this.memberMap; }
	
	@Nullable
	public IMobGroup split(LivingEntity... membersIn)
	{
		if(membersIn.length == 0)
			return null;
		
		IMobGroup group = new GroupGeneric();
		for(LivingEntity entity : membersIn)
			if(isMember(entity))
			{
				remove(entity);
				group.add(entity);
			}
		return GroupSaveData.get(membersIn[0].getServer()).register(group);
	}
	
	public List<LivingEntity> targets() { return targets; }
	public void addTarget(LivingEntity entity) { targets.add(entity); }
	public void removeTarget(LivingEntity entity) { targets.remove(entity); }
	
	public void tick(MinecraftServer server)
	{
		if(!isEmpty() && hasAction())
			updateGroupAction();
	}
	
	public boolean hasTarget()
	{
		if(targets.isEmpty())
			return false;
		for(Entity target : targets)
			if(target.isAlive())
				return true;
		
		return false;
	}
	
	public Entity target(int index) { return hasTarget() ? this.targets.get(index%this.targets.size()) : null; }
	
	public Whiteboard<?> getWhiteboard() { return this.storage; }
	
	public void add(@Nullable LivingEntity objIn)
	{
		if(objIn != null && objIn instanceof PathfinderMob)
			memberMap.put(objIn.getUUID(), (PathfinderMob)objIn);
	}
	
	public GroupAction getAction() { return this.currentAction; }
	public void setGroupAction(GroupAction action) { this.currentAction = action; }
	
	public static class Factory implements IGroupFactory
	{
		public IMobGroup create() { return new GroupGeneric(); }
	}
}
