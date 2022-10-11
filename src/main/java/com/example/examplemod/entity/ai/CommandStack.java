package com.example.examplemod.entity.ai;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.utility.MobCommanding.Mark;

public class CommandStack
{
	private List<MobCommand> activeTasks = Lists.newArrayList();
	
	public CommandStack(MobCommand... commandsIn)
	{
		for(int i=0; i<commandsIn.length; i++)
			activeTasks.add(commandsIn[i]);
	}
	
	public CommandStack addTask(MobCommand taskIn)
	{
		activeTasks.add(taskIn);
		return this;
	}
	
	public CommandStack clone()
	{
		return new CommandStack(activeTasks.toArray(new MobCommand[0]));
	}
	
	public CommandStack addTask(Mark markIn, Object... objectIn) { return addTask(new MobCommand(markIn, objectIn)); }
	
	public MobCommand current() { return activeTasks.get(0); }
	
	public List<MobCommand> allTasks(){ return this.activeTasks; }
	
	public boolean isEmpty() { return this.activeTasks.isEmpty(); }
	
	public boolean isSingle() { return this.activeTasks.size() == 1; }
	
	public void complete() { this.activeTasks.remove(0); }
	
	public static CommandStack single(MobCommand command) { return new CommandStack(command); }
	public static CommandStack single(Mark markIn, Object... objectIn) { return single(new MobCommand(markIn, objectIn)); }
}
