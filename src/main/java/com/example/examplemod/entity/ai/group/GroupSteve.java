package com.example.examplemod.entity.ai.group;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.group.Strategy.Status;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class GroupSteve extends GroupTyped<TestEntity>
{
	private final List<Strategy<TestEntity>> possibleStrats = Lists.newArrayList();
	private Strategy<TestEntity> currentStrat = null;
	/** Delay between strategy implementations */
	private int stratTimer = Reference.Values.TICKS_PER_SECOND * 15;
	
	public GroupSteve(TestEntity... membersIn)
	{
		super(membersIn);
		possibleStrats.add(new Strategy.StrategyFlank<>());
		possibleStrats.add(new Strategy.StrategyAggroFlank<>());
		possibleStrats.add(new Strategy.StrategyBrawl<>());
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
				
				if(currentAction != null)
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
			else if(currentStrat != null && currentStrat.update(this) != Status.RUNNING)
				currentStrat = null;
			else if(--stratTimer <= 0)
			{
				assignBestStrat();
				stratTimer = Reference.Values.TICKS_PER_SECOND * 15;
			}
		}
	}
	
	public void setTarget(LivingEntity targetIn)
	{
		if(targets.contains(targetIn) || targetIn == null || !targetIn.isAlive() || !targetIn.isAddedToWorld())
			return;
		
		boolean shouldFlank = targetIn != null && !hasTarget();
		targets.add(targetIn);
		if(shouldFlank)
			assignBestStrat();
	}
	
	public void assignBestStrat()
	{
		GroupTyped<TestEntity> theGroup = this;
		possibleStrats.sort(new Comparator<>()
				{
					public int compare(Strategy<TestEntity> o1, Strategy<TestEntity> o2)
					{
						float util1 = Mth.clamp(o1.utility(theGroup), 0F, 1F);
						float util2 = Mth.clamp(o2.utility(theGroup), 0F, 1F);
						return util1 > util2 ? -1 : util1 < util2 ? 1 : 0;
					}
				});
		
		for(Strategy<TestEntity> strat : possibleStrats)
		{
			if(strat.tick(theGroup) != Status.FAILURE)
			{
				currentStrat = strat;
				break;
			}
		}
	}
	
	public Strategy<?> getStrategy() { return this.currentStrat; }
	
	public static class Factory implements IGroupFactory
	{
		public IMobGroup create() { return new GroupSteve(); }
	}
}
