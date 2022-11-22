package com.example.examplemod.commands;

import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CommandMobCommand
{
	private static final String translationSlug = "command."+Reference.ModInfo.MOD_ID+".give_order.";
	
	private static final String ENTITY = "recipient";
	private static final String DATA = "nbt";
	private static final String GROUP = "to_group";
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal("give_order").requires((source) -> { return source.hasPermission(2); } )
				.then(Commands.argument(ENTITY, EntityArgument.entity())
					.then(Commands.argument(DATA, CompoundTagArgument.compoundTag())
						.executes((source) -> { return giveCommand(source.getSource(), EntityArgument.getEntity(source, ENTITY), CompoundTagArgument.getCompoundTag(source, DATA), false); })
						.then(Commands.argument(GROUP, BoolArgumentType.bool())
							.executes((source) -> { return giveCommand(source.getSource(), EntityArgument.getEntity(source, ENTITY), CompoundTagArgument.getCompoundTag(source, DATA), BoolArgumentType.getBool(source, GROUP)); }))));
		
		dispatcher.register(literal);
	}
	
	private static int giveCommand(CommandSourceStack source, Entity entity, CompoundTag compound, boolean group)
	{
		if(entity == null || !(entity instanceof LivingEntity))
		{
			source.sendFailure(Component.translatable(translationSlug+"invalid"));
			return 0;
		}
		
		CommandStack stack = null;
		try
		{
			stack = CommandStack.loadFromNbt(compound, source.getLevel());
		}
		catch(Exception e) { }
		
		try
		{
			stack = new CommandStack(MobCommand.loadFromNBT(compound, 16D, source.getLevel()));
		}
		catch(Exception e) { }
		
		if(stack == null || stack.isEmpty())
		{
			source.sendFailure(Component.translatable(translationSlug+"invalid"));
			return 0;
		}
		
		if(!group)
		{
			Whiteboard<?> whiteboard = Whiteboard.tryGetWhiteboard((LivingEntity)entity);
			if(whiteboard != null)
			{
				if(whiteboard.hasCommands())
					whiteboard.setCommands(whiteboard.getCommands().prependAll(stack));
				else
					whiteboard.setCommands(stack);
				
				source.sendSuccess(Component.translatable(translationSlug+"single.success", stack.size(), entity.getDisplayName()), true);
				return 15;
			}
			
			source.sendFailure(Component.translatable(translationSlug+"single.failed", entity.getDisplayName()));
			return 0;
		}
		
		IMobGroup recipientGroup = GroupSaveData.get(entity.getServer()).getGroup((LivingEntity)entity);
		if(recipientGroup != null)
		{
			recipientGroup.giveCommandToAll(stack);
			source.sendSuccess(Component.translatable(translationSlug+"group.success", stack.size(), entity.getDisplayName()), true);
			return 15;
		}
		
		source.sendFailure(Component.translatable(translationSlug+"group.failed", entity.getDisplayName()));
		return 0;
	}
}
