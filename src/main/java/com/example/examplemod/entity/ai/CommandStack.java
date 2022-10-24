package com.example.examplemod.entity.ai;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.utility.MobCommanding.Mark;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class CommandStack
{
	private List<MobCommand> activeTasks = Lists.newArrayList();
	
	public CommandStack(MobCommand... commandsIn)
	{
		for(int i=0; i<commandsIn.length; i++)
			activeTasks.add(commandsIn[i]);
	}
	
	/** Adds the given command to the bottom of this stack, to be executed last */
	public CommandStack append(@Nullable MobCommand taskIn)
	{
		if(taskIn != null)
			activeTasks.add(taskIn);
		return this;
	}
	public CommandStack append(Mark markIn, Object... objectIn) { return append(new MobCommand(markIn, objectIn)); }
	
	/** Adds the given command to the top of this stack, to be executed first */
	public CommandStack prepend(@Nullable MobCommand taskIn)
	{
		if(taskIn != null)
		{
			List<MobCommand> tasks = Lists.newArrayList();
			tasks.add(taskIn);
			tasks.addAll(activeTasks);
			activeTasks = tasks;
		}
		return this;
	}
	public CommandStack prepend(Mark markIn, Object... objectIn) { return prepend(new MobCommand(markIn, objectIn)); }
	
	public CommandStack clone()
	{
		return new CommandStack(activeTasks.toArray(new MobCommand[0]));
	}
	
	public MobCommand current() { return activeTasks.get(0); }
	
	public List<MobCommand> allTasks(){ return this.activeTasks; }
	
	public boolean isEmpty() { return this.activeTasks.isEmpty(); }
	
	public boolean isSingle() { return this.activeTasks.size() == 1; }
	
	public void complete()
	{
		if(!isEmpty() && current().type.canBeCompleted())
			this.activeTasks.remove(0);
	}
	
	public static CommandStack single(MobCommand command) { return new CommandStack(command); }
	public static CommandStack single(Mark markIn, Object... objectIn) { return single(new MobCommand(markIn, objectIn)); }
	
	public CompoundTag saveToNbt(CompoundTag compound)
	{
		ListTag data = new ListTag();
		for(MobCommand command : activeTasks)
			data.add(command.saveToNBT(new CompoundTag()));
		
		compound.put("Stack", data);
		return compound;
	}
}
