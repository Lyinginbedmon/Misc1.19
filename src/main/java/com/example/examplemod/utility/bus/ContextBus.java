package com.example.examplemod.utility.bus;

import com.example.examplemod.api.event.LivingConsumableEvent.Drink;
import com.example.examplemod.api.event.LivingConsumableEvent.Eat;
import com.example.examplemod.api.event.PlayerEnchantItemEvent;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.data.ExEntityTags;
import com.example.examplemod.data.ExItemTags;
import com.example.examplemod.deities.personality.ContextQuotients;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.savedata.BrewingStandWatcher;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContextBus
{
	public static void handleStatusEffect(MobEffectInstance current, MobEffectInstance original, PlayerData data)
	{
		if(current == null || current.getDuration() <= 0)
			return;
		
		MobEffect effect = current.getEffect();
		// Ignore any status effects that we already had and which have not been improved
		if(original != null && current.getAmplifier() <= original.getAmplifier())
			return;
		
		double amount = (current.getDuration() / Reference.Values.TICKS_PER_SECOND) * Math.pow(1 + current.getAmplifier(), 3);
		data.addQuotient(ContextQuotients.getPotionQuotients().get(effect).getId(), amount);
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent event)
	{
		if(event.isCanceled())
			return;
		
		LivingEntity victim = event.getEntity();
		Entity assailant = event.getSource().getEntity();
		double dmg = event.getAmount();
		
		if(victim.getType() == EntityType.PLAYER)
		{
			PlayerData data = PlayerData.getCapability((Player)victim);
			data.addQuotient(ContextQuotients.DAMAGE_TAKEN.getId(), dmg);
		}
		
		if(assailant != null && assailant.getType() == EntityType.PLAYER)
		{
			PlayerData data = PlayerData.getCapability((Player)assailant);
			
			if(victim.getType().is(ExEntityTags.BOSS))
				data.addQuotient(ContextQuotients.DAMAGE_BOSS.getId(), dmg);
			else
				data.addQuotient(ContextQuotients.DAMAGE_GIVEN.getId(), dmg);
			
			if(event.getSource().getDirectEntity() == assailant)
				data.addQuotient(ContextQuotients.MELEE.getId(), dmg);
			else
				data.addQuotient(ContextQuotients.ARCHERY.getId(), dmg);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.isCanceled())
			return;
		
		LivingEntity victim = event.getEntity();
		Entity assailant = event.getSource().getEntity();
		
		if(victim.getType().is(ExEntityTags.BOSS) && assailant != null && assailant.getType() == EntityType.PLAYER)
		{
			PlayerData.getCapability((Player)assailant).addQuotient(ContextQuotients.KILL_BOSS.getId(), 1);
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
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onFoodEat(Eat event)
	{
		if(!event.isCanceled() && event.getEntity().getType() == EntityType.PLAYER && !event.getEntity().getLevel().isClientSide())
			addDietTags((Player)event.getEntity(), event.getItem());
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onPotionDrink(Drink event)
	{
		if(!event.isCanceled() && event.getEntity().getType() == EntityType.PLAYER && !event.getEntity().getLevel().isClientSide())
		{
			Player player = (Player)event.getEntity();
			ItemStack item = event.getItem();
			addDietTags(player, item);
			
			if(item.getItem() == Items.POTION && !PotionUtils.getMobEffects(item).isEmpty())
				PlayerData.getCapability(player).addQuotient(ContextQuotients.DRINK_POTION.getId(), 1);
		}
	}
	
	public static void addDietTags(Player player, ItemStack item)
	{
		PlayerData data = PlayerData.getCapability(player);
		if(item.is(ExItemTags.TABOO))
			data.addQuotient(ContextQuotients.EAT_TABOO.getId(), 1);
		
		for(TagKey<Item> tag : ExItemTags.DIET_TAGS)
			if(item.is(tag))
			{
				data.addTagToDiet(tag);
				break;
			}
	}
	
	@SubscribeEvent
	public static void onPlayerEnchantItem(PlayerEnchantItemEvent event)
	{
		PlayerData data = PlayerData.getCapability(event.getEntity());
		data.addQuotient(ContextQuotients.ENCHANTING.getId(), event.getLevel());
	}
	
	@SubscribeEvent
	public static void onPlayerRepairItem(AnvilRepairEvent event)
	{
		PlayerData data = PlayerData.getCapability(event.getEntity());
		if(event.getRight().getItem() == Items.ENCHANTED_BOOK)
			data.addQuotient(ContextQuotients.ENCHANTING.getId(), 1);
	}
	
	@SubscribeEvent
	public static void onPlayerBrewPotion(PlayerBrewedPotionEvent event)
	{
		BrewingStandWatcher watcher = BrewingStandWatcher.instance(event.getEntity().getLevel());
		BlockPos pos = watcher.lastTouched(event.getEntity().getUUID());
		if(pos == null)
			return;
		
		int value = watcher.extractStack(pos, event.getStack());
		if(value > 0)
			PlayerData.getCapability(event.getEntity()).addQuotient(ContextQuotients.BREWING.getId(), value);
	}
}
