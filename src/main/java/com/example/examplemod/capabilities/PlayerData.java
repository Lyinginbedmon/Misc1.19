package com.example.examplemod.capabilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.DeityRegistry;
import com.example.examplemod.deities.personality.ContextQuotient;
import com.example.examplemod.deities.personality.ContextQuotients;
import com.example.examplemod.init.ExCapabilities;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSyncPlayerData;
import com.example.examplemod.proxy.CommonProxy;
import com.example.examplemod.reference.Reference;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerData implements ICapabilitySerializable<CompoundTag>
{
	public static final ResourceLocation IDENTIFIER = new ResourceLocation(Reference.ModInfo.MOD_ID, "player_data");
	private static final long OPINION_RATE = Reference.Values.TICKS_PER_SECOND * 5;
	private static final int DIET_LIMIT = 12;
	
	private Player thePlayer;
	
	private Map<ResourceLocation, Double> quotients = new HashMap<>();
	private List<TagKey<Item>> recentDiet = Lists.newArrayList();
	
	private String deityName;
	private double prevOpinion, currentOpinion;
	private long ticksSinceOpinion = 0;
	
	private int ticksToMiracle = 0;
	
	private boolean isDirty = true;
	
	public PlayerData(Player playerIn)
	{
		this.thePlayer = playerIn;
	}
	
	public void setPlayer(Player playerIn) { this.thePlayer = playerIn; }
	
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
	{
		return ExCapabilities.PLAYER_DATA.orEmpty(cap, LazyOptional.of(() -> this));
	}
	
	public static PlayerData getCapability(Player player)
	{
		if(player == null)
			return null;
		else if(player.getLevel().isClientSide())
			return ((CommonProxy)ExampleMod.PROXY).getPlayerData(player);
		
		PlayerData data = player.getCapability(ExCapabilities.PLAYER_DATA).orElse(new PlayerData(player));
		data.thePlayer = player;
		return data;
	}
	
	public CompoundTag serializeNBT()
	{
		CompoundTag data = new CompoundTag();
		data.putString("Deity", this.deityName);
		data.putDouble("OpinionPrev", this.prevOpinion);
		data.putDouble("OpinionNow", this.currentOpinion);
		data.putLong("CheckTime", this.ticksSinceOpinion);
		data.putInt("Cooldown", this.ticksToMiracle);
		
		ListTag quotients = new ListTag();
		for(Entry<ResourceLocation, Double> entry : this.quotients.entrySet())
		{
			if(entry.getValue() <= 0D)
				continue;
			
			CompoundTag val = new CompoundTag();
			val.putString("Name", entry.getKey().toString());
			val.putDouble("Val", entry.getValue());
			
			quotients.add(val);
		}
		data.put("Values", quotients);
		
		ListTag diet = new ListTag();
		this.recentDiet.forEach((tag) -> diet.add(StringTag.valueOf(tag.location().toString())));
		data.put("Diet", diet);
		
		return data;
	}
	
	public void deserializeNBT(CompoundTag nbt)
	{
		this.deityName = nbt.getString("Deity");
		this.prevOpinion = nbt.getDouble("OpinionPrev");
		this.currentOpinion = nbt.getDouble("OpinionNow");
		this.ticksSinceOpinion = nbt.getLong("CheckTime");
		this.ticksToMiracle = nbt.getInt("Cooldown");
		
		this.quotients.clear();
		ListTag quotients = nbt.getList("Values", Tag.TAG_COMPOUND);
		for(int i=0; i<quotients.size(); i++)
		{
			CompoundTag data = quotients.getCompound(i);
			this.quotients.put(new ResourceLocation(data.getString("Name")), data.getDouble("Val"));
		}
		
		this.recentDiet.clear();
		ListTag diet = nbt.getList("Diet", Tag.TAG_STRING);
		for(int i=0; i<diet.size(); i++)
		{
			ResourceLocation registryName = new ResourceLocation(diet.getString(i));
			TagKey<Item> dietary = TagKey.create(Registry.ITEM_REGISTRY, registryName);
			if(dietary != null)
				recentDiet.add(dietary);
		}
	}
	
	public String getDeityName() { return this.deityName; }
	@Nullable
	public Deity getDeity() { return DeityRegistry.getInstance().getDeity(this.deityName); }
	public void setDeity(Deity deityIn)
	{
		this.deityName = deityIn.simpleName(); markDirty();
		
		Deity god = getDeity();
		if(god == null)
			this.prevOpinion = this.currentOpinion = 0D;
		else
			this.prevOpinion = this.currentOpinion = god.opinionOf(thePlayer);
	}
	
	public double getOpinion() { return this.prevOpinion + ((currentOpinion - prevOpinion) * ((double)ticksSinceOpinion / (double)OPINION_RATE)); }
	
	public void setQuotient(ResourceLocation quotientName, double value)
	{
		this.quotients.put(quotientName, value);
		markDirty();
	}
	public void addQuotient(ResourceLocation quotientName, double value)
	{
		setQuotient(quotientName, getQuotient(quotientName) + value);
	}
	public double getQuotient(ResourceLocation quotientName) { return this.quotients.getOrDefault(quotientName, 0D); }
	
	public Map<ResourceLocation, Double> getQuotients() { return this.quotients; }
	
	public void addTagToDiet(TagKey<Item> tag)
	{
		this.recentDiet.add(tag);
		while(recentDiet.size() > DIET_LIMIT)
			recentDiet.remove(0);
		markDirty();
	}
	
	public Map<TagKey<Item>, Double> getRecentDiet()
	{
		Map<TagKey<Item>, Integer> tally = new HashMap<>();
		for(TagKey<Item> tag : this.recentDiet)
			tally.put(tag, 1 + tally.getOrDefault(tag, 0));
		
		Map<TagKey<Item>, Double> diet = new HashMap<>();
		tally.forEach((tag,count) -> diet.put(tag, (double)count / (double)this.recentDiet.size()));
		return diet;
	}
	
	public void tick()
	{
		Deity god = getDeity();
		if(god == null || thePlayer == null || thePlayer.getLevel().isClientSide())
			return;
		
		if(!this.quotients.isEmpty())
		{
			Map<ResourceLocation, Double> quotientMap = new HashMap<>();
			for(Entry<ResourceLocation, Double> entry : this.quotients.entrySet())
			{
				ContextQuotient quotient = ContextQuotients.getByName(entry.getKey()).get();
				double val = entry.getValue() - quotient.decayRate();
				if(val > 0)
					quotientMap.put(entry.getKey(), val);
			}
			
			this.quotients.clear();
			this.quotients.putAll(quotientMap);
			markDirty();
		}
		
		if(thePlayer.getRandom().nextInt((int)Math.max(1, OPINION_RATE - this.ticksSinceOpinion++)) == 0)
		{
			this.prevOpinion = this.currentOpinion;
			this.currentOpinion = god.opinionOf(thePlayer);
			this.ticksSinceOpinion = 0;
			markDirty();
		}
		
		if(this.ticksToMiracle > 0)
		{
			--this.ticksToMiracle;
			markDirty();
		}
		
		if(isDirty && !this.thePlayer.getLevel().isClientSide())
		{
			PacketHandler.sendTo((ServerPlayer)this.thePlayer, new PacketSyncPlayerData(this.thePlayer.getUUID(), this));
			this.isDirty = false;
		}
	}
	
	public boolean canHaveMiracle() { return this.ticksToMiracle <= 0; }
	public void setMiracleCooldown(int par1Int)
	{
		this.ticksToMiracle = par1Int;
		markDirty();
	}
	
	public void markDirty() { this.isDirty = true; }
}
