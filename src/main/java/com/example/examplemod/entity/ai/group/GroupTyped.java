package com.example.examplemod.entity.ai.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.GroupWhiteboard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.HitResult.Type;

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
	
	public void giveCommandToAll(CommandStack stack)
	{
		if(stack.isSingle())
		{
			MobCommand order = stack.current();
			switch(order.type)
			{
				case PICK_UP:
				case EQUIP:
				case MINE:
				case ACTIVATE:
					// Give to closest member
					BlockPos pos = order.type().inputType() == Type.BLOCK ? (BlockPos)order.variable(0) : ((Entity)order.variable(0)).blockPosition();
					
					double minDistToBlock = Double.MAX_VALUE;
					LivingEntity closeToBlock = null;
					for(LivingEntity member : members())
					{
						double dist = pos.distSqr(member.blockPosition());
						if(dist < minDistToBlock)
						{
							dist = minDistToBlock;
							closeToBlock = member;
						}
					}
					
					if(closeToBlock != null)
						Whiteboard.tryGetWhiteboard(closeToBlock).setCommands(stack);
					return;
				case MOUNT:
					// Give to closest unmounted member
					Entity vehicle = (Entity)order.variable(0);
					
					double minDistToEnt = Double.MAX_VALUE;
					LivingEntity closeToEnt = null;
					for(LivingEntity member : members())
					{
						double dist = member.distanceToSqr(vehicle);
						if(member.getVehicle() == null && dist < minDistToEnt)
						{
							dist = minDistToEnt;
							closeToEnt = member;
						}
					}
					
					if(closeToEnt != null)
						Whiteboard.tryGetWhiteboard(closeToEnt).setCommands(stack);
					
					return;
				case FOLLOW_MOB:
					Entity followEnt = (Entity)order.variable(0);
				case GUARD_MOB:
					Entity guardEnt = (Entity)order.variable(0);
					break;
				case GUARD_POS:
					BlockPos guardPos = (BlockPos)order.variable(0);
					break;
				case QUARRY:
					if(order.variables() > 2)
						currentAction = new GroupAction.ActionQuarry<T>((BlockPos)order.variable(0), (BlockPos)order.variable(2), (Direction)order.variable(1));
					else
					{
						BlockPos corePos = (BlockPos)order.variable(0);
						Direction facing = (Direction)order.variable(1);
						Vec3i min = new Vec3i(-5,0,-5);
						Vec3i max = new Vec3i(5,3,5);
						
						currentAction = new GroupAction.ActionQuarry<T>(corePos.offset(min), corePos.offset(max), facing);
					}
					return;
				default:
					break;
			}
		}
		
		members().forEach((member) -> Whiteboard.tryGetWhiteboard(member).setCommands(stack));
	}
	
	public List<LivingEntity> targets() { return this.targets; }
	
	protected void updateGroupAction()
	{
		if(this.currentAction == null)
			return;
		
		this.currentAction.update(members(), members().get(0).getLevel());
		if(this.currentAction.isComplete())
			this.currentAction = null;
	}
	
	public Entity target(int index) { return hasTarget() ? this.targets.get(index%this.targets.size()) : null; }
	
	public Whiteboard<?> getWhiteboard() { return this.storage; }
}
