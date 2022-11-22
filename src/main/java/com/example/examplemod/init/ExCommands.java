package com.example.examplemod.init;

import com.example.examplemod.commands.CommandMobCommand;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public class ExCommands
{
	public static void init(RegisterCommandsEvent event)
	{
    	CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    	
    	CommandMobCommand.register(dispatcher);
	}
}
