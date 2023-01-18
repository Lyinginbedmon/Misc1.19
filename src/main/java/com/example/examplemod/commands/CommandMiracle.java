package com.example.examplemod.commands;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.miracle.Miracle;
import com.example.examplemod.deities.miracle.Miracles;
import com.example.examplemod.reference.Reference;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

public class CommandMiracle
{
 	public static final SuggestionProvider<CommandSourceStack> MIRACLE_NAMES = SuggestionProviders.register(new ResourceLocation("miracle_list"), (context, builder) -> {
 		return SharedSuggestionProvider.suggest(Miracles.getMiracleNames(), builder);
 		});
	private static final String translationSlug = "command."+Reference.ModInfo.MOD_ID+".miracle.";
	
	private static final String ENTITY = "target";
	private static final String MIRACLE = "miracle";
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal("miracle").requires((source) -> { return source.hasPermission(2); } )
				.then(Commands.argument(MIRACLE, ResourceLocationArgument.id()).suggests(MIRACLE_NAMES)
					.executes((source) -> { return forceMiracleSelf(ResourceLocationArgument.getId(source, MIRACLE), source.getSource()); })
						.then(Commands.argument(ENTITY, EntityArgument.player())
							.executes((source) -> { return forceMiracle(ResourceLocationArgument.getId(source, MIRACLE), source.getSource(), EntityArgument.getPlayer(source, ENTITY)); })))
				.then(Commands.literal("clear")
					.executes((source) -> { return clearMiracleSelf(source.getSource()); } )
						.then(Commands.argument(ENTITY, EntityArgument.player())
							.executes((source) -> { return clearMiracle(source.getSource(), EntityArgument.getPlayer(source, ENTITY)); })));
		
		dispatcher.register(literal);
	}
	
	private static int forceMiracleSelf(ResourceLocation miracleName, CommandSourceStack source)
	{
		return forceMiracle(miracleName, source, source.getPlayer());
	}
	
	private static int clearMiracleSelf(CommandSourceStack source)
	{
		return clearMiracle(source, source.getPlayer());
	}
	
	private static int forceMiracle(ResourceLocation miracleName, CommandSourceStack source, ServerPlayer entity)
	{
		if(entity == null)
		{
			source.sendFailure(Component.translatable(translationSlug+"invalid"));
			return 0;
		}
		else
		{
			PlayerData data = PlayerData.getCapability((Player)entity);
			RegistryObject<Miracle> miracle = Miracles.getByName(miracleName);
			if(data != null && miracle != null)
			{
				data.setForceMiracle(miracle == null ? null : miracle.getId());
				source.sendSuccess(Component.translatable(translationSlug+"success", entity.getDisplayName(), miracle.getId()), true);
				return 15;
			}
		}
		
		source.sendFailure(Component.translatable(translationSlug+"failed", source.getDisplayName()));
		return 0;
	}
	
	private static int clearMiracle(CommandSourceStack source, ServerPlayer entity)
	{
		PlayerData data = PlayerData.getCapability(entity);
		if(data.hasForcedMiracle())
		{
			data.clearForceMiracle();
			source.sendSuccess(Component.translatable(translationSlug+"clear.success", entity.getDisplayName()), true);
			return 15;
		}
		source.sendFailure(Component.translatable(translationSlug+"clear.failed", source.getDisplayName()));
		
		return 0;
	}
}
