package com.example.examplemod.entity.ai.group;

import java.util.List;

import javax.annotation.Nullable;

import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.utility.GroupSaveData;
import com.example.examplemod.utility.MobCommanding.Mark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

public class GroupSteve extends GroupTyped<TestEntity>
{
	public GroupSteve(TestEntity... membersIn)
	{
		super(membersIn);
	}
	
	public ResourceLocation getKey(){ return GroupType.STEVE; }
	
	@Nullable
	public IMobGroup split(LivingEntity... membersIn)
	{
		if(membersIn.length == 0)
			return null;
		IMobGroup group = new GroupSteve();
		for(LivingEntity entity : membersIn)
			if(isMember(entity))
			{
				remove(entity);
				group.add(entity);
			}
		return GroupSaveData.get(membersIn[0].getServer()).register(group);
	}
	
	public void tick(MinecraftServer server)
	{
		// Determine current priority target based on known targets amongst Steves
		if(!isEmpty())
		{
			LivingEntity priority = null;
			for(TestEntity steve : membersTyped())
			{
				Whiteboard<?> storage = steve.getWhiteboard(steve);
				if(storage.getEntity(MobWhiteboard.MOB_TARGET) != null)
				{
					priority = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
					break;
				}
			}
			setTarget(priority);
			
			if(!hasTarget())
			{
				// Clear attack targets
				membersTyped().forEach((steve) -> 
				{
					if(steve.getTarget() != null)
					{
						Whiteboard<?> board = steve.getWhiteboard(steve);
						board.setCommands(CommandStack.single(Mark.CEASEFIRE));
					}
				});
				
				if(hasAction())
					updateGroupAction();
				else
				{
					List<TestEntity> steves = membersTyped();
					// Group Steves together
					for(int i=0; i<steves.size(); i++)
					{
						TestEntity steve1 = steves.get(i);
						TestEntity steve2 = steves.get((i + 1) % steves.size());
						if(steve1.blockPosition() == null || steve2.blockPosition() == null)
							continue;
						
						if(steve1.distanceTo(steve2) > 6D && !steve1.getWhiteboard(steve1).hasCommands() && !steve2.getWhiteboard(steve2).hasCommands())
							steve1.getWhiteboard(steve1).setCommands(CommandStack.single(Mark.GOTO_MOB, steve2));
					}
				}
			}
		}
	}
	
	public void setTarget(LivingEntity targetIn)
	{
		if(targets.contains(targetIn) || targetIn == null || !targetIn.isAlive() || !targetIn.isAddedToWorld())
			return;
		
		targets.add(targetIn);
	}
	
	public static class Factory implements IGroupFactory
	{
		public IMobGroup create() { return new GroupSteve(); }
	}
}