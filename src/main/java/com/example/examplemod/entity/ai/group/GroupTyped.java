package com.example.examplemod.entity.ai.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.GroupWhiteboard;
import com.example.examplemod.entity.ai.group.action.GroupAction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public abstract class GroupTyped<T extends PathfinderMob & ITreeEntity> implements IMobGroup
{
	protected Map<UUID, LivingEntity> memberMap = new HashMap<>();
	
	public GroupWhiteboard<T> storage = new GroupWhiteboard<T>();
	
	protected List<LivingEntity> targets = Lists.newArrayList();
	
	/** Large-scale action to be undertaken by this group */
	protected GroupAction currentAction = null;
	
	private boolean dirty = false;
	
	@SuppressWarnings("unchecked")
	protected GroupTyped(T... membersIn)
	{
		for(T member : membersIn)
			add(member);
	}
	
	public boolean isDirty() { return this.dirty; }
	
	public void setDirty(boolean bool) { this.dirty = bool; }
	
	public Map<UUID, LivingEntity> membership(){ return this.memberMap; }
	
	@SuppressWarnings("unchecked")
	public List<T> membersTyped()
	{
		List<T> members = Lists.newArrayList();
		members().forEach((member) -> 
		{
			try
			{
				members.add((T)member);
			}
			catch(Exception e) { }
		});
		return members;
	}
	
	public boolean shouldListenTo(LivingEntity entity) { return isEmpty() ? false : entity.getClass() == members().get(0).getClass(); }
	
	public List<LivingEntity> targets()
	{
		this.targets.removeIf((living) -> living == null || !living.isAlive() );
		return this.targets;
	}
	public void addTarget(LivingEntity entity) { targets.add(entity); }
	public void removeTarget(LivingEntity entity) { targets.remove(entity); }
	
	public Entity target(int index) { return hasTarget() ? this.targets.get(index%this.targets.size()) : null; }
	
	public Whiteboard<?> getWhiteboard() { return this.storage; }
	
	public GroupAction getAction() { return this.currentAction; }
	public void setGroupAction(GroupAction action) { this.currentAction = action; setDirty(); }
}
