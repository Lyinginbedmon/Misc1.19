package com.example.examplemod.utility.bus;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.data.ExItemTags;
import com.example.examplemod.deities.personality.ContextQuotients;
import com.example.examplemod.reference.Reference;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContextBus
{
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent event)
	{
		LivingEntity victim = event.getEntity();
		Entity assailant = event.getSource().getEntity();
		
		if(victim.getType() == EntityType.PLAYER)
		{
			PlayerData data = PlayerData.getCapability((Player)victim);
			data.addQuotient(ContextQuotients.DAMAGE_TAKEN.getId(), event.getAmount());
		}
		
		if(assailant != null && assailant.getType() == EntityType.PLAYER)
		{
			PlayerData data = PlayerData.getCapability((Player)assailant);
			if(event.getSource().getDirectEntity() == assailant)
				data.addQuotient(ContextQuotients.MELEE.getId(), event.getAmount());
			else
				data.addQuotient(ContextQuotients.ARCHERY.getId(), event.getAmount());
		}
	}
	
	@SubscribeEvent
	public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
	{
		Player player = event.getEntity();
		PlayerData data = PlayerData.getCapability(player);
		data.addQuotient(ContextQuotients.CRAFTING.getId(), event.getCrafting().getCount());
	}
	
	@SubscribeEvent
	public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event)
	{
		Player player = event.getEntity();
		PlayerData data = PlayerData.getCapability(player);
		data.addQuotient(ContextQuotients.SMELTING.getId(), event.getSmelting().getCount());
	}
	
	@SubscribeEvent
	public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event)
	{
		ItemStack item = event.getItem();
		if(event.getEntity().getType() == EntityType.PLAYER && !event.getEntity().getLevel().isClientSide())
		{
			Player player = (Player)event.getEntity();
			PlayerData data = PlayerData.getCapability(player);
			// FIXME Event fires AFTER item applies effects so need to handle this BEFORE
			if(item.isEdible())
			{
//				item.getFoodProperties(player).getEffects().forEach((pair) -> handleStatusEffect(player, data, pair.getFirst()));
				
				if(item.is(ExItemTags.TABOO))
					data.addQuotient(ContextQuotients.EAT_TABOO.getId(), 1);
				
				for(TagKey<Item> tag : ExItemTags.DIET_TAGS)
					if(item.is(tag))
					{
						data.addTagToDiet(tag);
						break;
					}
			}
			else if(item.getItem() == Items.POTION)
			{
//				PotionUtils.getMobEffects(item).forEach((instance) -> handleStatusEffect(player, data, instance));
				data.addQuotient(ContextQuotients.DRINK_POTION.getId(), 1);
			}
		}
	}
}
