package com.example.examplemod.deities.miracle;

import java.util.Optional;
import java.util.Random;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.api.event.MiracleEvent.CheckMiracleEvent;
import com.example.examplemod.api.event.MiracleEvent.PerformMiracleEvent;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.reference.Reference;
import com.ibm.icu.impl.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public abstract class Miracle
{
	public static final ResourceKey<Registry<Miracle>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "miracles"));
	
	public void addListeners(IEventBus bus) { }
	
	public abstract float getUtility(Player playerIn, Level worldIn);
	
	public final boolean is(TagKey<Miracle> keyIn)
	{
		Optional<Holder<Miracle>> holder = ExRegistries.MIRACLES_REGISTRY.get().getHolder(this);
		return holder.isPresent() && holder.get().is(keyIn);
	}
	
	public final RegistryObject<Miracle> getRegistryName(){ return Miracles.getRegistryName(this); }
	
	public Pair<Integer, Integer> cooldownRange() { return Pair.of(Reference.Values.TICKS_PER_MINUTE, Reference.Values.TICKS_PER_MINUTE * 5); }
	
	public static boolean isPlayerImmortal(Player playerIn)
	{
		return playerIn.getAbilities().invulnerable;
	}
	
	public static boolean checkMiracle(Player playerIn, Miracle miracleIn)
	{
		CheckMiracleEvent miracleEvent = new CheckMiracleEvent(playerIn, PlayerData.getCapability(playerIn).getDeity(), miracleIn);
		
		PlayerData data = PlayerData.getCapability(playerIn);
		Deity god = miracleEvent.godResponsible();
		if(god == null || !god.hasMiracle(miracleIn))
		{
			ExampleMod.LOG.info("Miracle denied! No deity or does not have "+miracleIn.getRegistryName().getId());
			miracleEvent.setResult(Result.DENY);
		}
		else if(!data.canHaveMiracle())
		{
			ExampleMod.LOG.info("Miracle denied! On cooldown");
			miracleEvent.setResult(Result.DENY);
		}
		else
		{
			double opinion = data.getOpinion();
			Random rng = god.getRandom();
			if(opinion < rng.nextFloat())
			{
				ExampleMod.LOG.info("Miracle denied! Bad odds/opinion");
				miracleEvent.setResult(Result.DENY);
			}
		}
		
		ExampleMod.EVENT_BUS.post(miracleEvent);
		return miracleEvent.getResult() != Result.DENY;
	}
	
	public static void reportMiracle(Player playerIn, Miracle miracleIn)
	{
		ExampleMod.LOG.info("Miracle performed! "+miracleIn.getRegistryName().getId());
		PlayerData data = PlayerData.getCapability(playerIn);
		Deity god = data.getDeity();
		Pair<Integer, Integer> cooldown = miracleIn.cooldownRange();
		data.setMiracleCooldown(cooldown.first + (int)((cooldown.second - cooldown.first) * god.getRandom().nextDouble()));
		
		ExampleMod.EVENT_BUS.post(new PerformMiracleEvent(playerIn, PlayerData.getCapability(playerIn).getDeity(), miracleIn));
	}
}
