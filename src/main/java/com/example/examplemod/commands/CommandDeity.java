package com.example.examplemod.commands;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.DeityRegistry;
import com.example.examplemod.reference.Reference;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CommandDeity
{
 	public static final SuggestionProvider<CommandSourceStack> DEITY_NAMES = SuggestionProviders.register(new ResourceLocation("deity_names"), (context, builder) -> {
 		return SharedSuggestionProvider.suggest(DeityRegistry.getInstance().getDeityNames(), builder);
 		});
	private static final String translationSlug = "command."+Reference.ModInfo.MOD_ID+".deity.";
	
	private static final String ENTITY = "target";
	private static final String GOD = "deity";
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal("deity").requires((source) -> { return source.hasPermission(2); } )
				.then(Commands.argument(GOD, StringArgumentType.word()).suggests(DEITY_NAMES)
					.executes((source) -> { return worshipSelf(StringArgumentType.getString(source, GOD), source.getSource()); })
						.then(Commands.argument(ENTITY, EntityArgument.player())
							.executes((source) -> { return worshipTarget(StringArgumentType.getString(source, GOD), source.getSource(), EntityArgument.getPlayer(source, ENTITY)); })));
		
		dispatcher.register(literal);
	}
	
	private static int worshipSelf(String godName, CommandSourceStack source)
	{
		return worshipTarget(godName, source, source.getPlayer());
	}
	
	private static int worshipTarget(String godName, CommandSourceStack source, ServerPlayer entity)
	{
		if(entity == null)
		{
			source.sendFailure(Component.translatable(translationSlug+"invalid"));
			return 0;
		}
		else
		{
			PlayerData data = PlayerData.getCapability(entity);
			Deity god = DeityRegistry.getInstance().getDeity(godName);
			
			if(data != null && god != null)
			{
				data.setDeity(god);
				source.sendSuccess(Component.translatable(translationSlug+"success", entity.getDisplayName()), true);
				return 15;
			}
		}
		
		source.sendFailure(Component.translatable(translationSlug+"failed", source.getDisplayName()));
		return 0;
	}
}
