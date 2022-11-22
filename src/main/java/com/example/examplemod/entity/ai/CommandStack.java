package com.example.examplemod.entity.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.utility.MobCommanding.Mark;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

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
	
	public CommandStack appendAll(CommandStack stackIn)
	{
		stackIn.allTasks().forEach((task) -> stackIn.append(task));
		return this;
	}
	
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
	
	public CommandStack prependAll(CommandStack stackIn)
	{
		this.activeTasks.forEach((task) -> stackIn.append(task));
		return stackIn;
	}
	
	public CommandStack clone()
	{
		return new CommandStack(activeTasks.toArray(new MobCommand[0]));
	}
	
	public MobCommand current() { return activeTasks.get(0); }
	
	public List<MobCommand> allTasks(){ return this.activeTasks; }
	
	public boolean isEmpty() { return this.activeTasks.isEmpty(); }
	
	public boolean isSingle() { return this.activeTasks.size() == 1; }
	
	public void complete(boolean force)
	{
		if(!isEmpty() && (current().type.canBeCompleted() || force))
			this.activeTasks.remove(0);
	}
	
	public int size() { return this.activeTasks.size(); }
	
	public static CommandStack single(MobCommand command) { return new CommandStack(command); }
	public static CommandStack single(Mark markIn) { return single(markIn, new HashMap<String, Object>()); }
	public static CommandStack single(Mark markIn, Map<String, Object> objectIn) { return single(markIn.makeCommand(objectIn)); }
	
	public CompoundTag saveToNbt(CompoundTag compound)
	{
		ListTag data = new ListTag();
		for(MobCommand command : activeTasks)
			data.add(command.saveToNBT(new CompoundTag()));
		
		compound.put("Stack", data);
		return compound;
	}
	
	public static CommandStack loadFromNbt(CompoundTag compound, Level world)
	{
		ListTag data = compound.getList("Stack", Tag.TAG_COMPOUND);
		List<MobCommand> commands = Lists.newArrayList();
		for(int i=0; i<data.size(); i++)
		{
			MobCommand command = MobCommand.loadFromNBT(data.getCompound(i), 16D, world);
			if(command != null)
				commands.add(command);
		}
		
		return new CommandStack(commands.toArray(new MobCommand[0]));
	}
}
