package com.example.examplemod.capabilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.DeityRegistry;
import com.example.examplemod.init.ExCapabilities;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSyncPlayerData;
import com.example.examplemod.reference.Reference;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerData implements ICapabilitySerializable<CompoundTag>
{
	public static final ResourceLocation IDENTIFIER = new ResourceLocation(Reference.ModInfo.MOD_ID, "player_data");
	private Player thePlayer;
	
	private Map<ResourceLocation, Double> quotients = new HashMap<>();
	private static final double QUOTIENT_DECAY = 0.01D;
	
	private String deityName;
	
	@SuppressWarnings("unused")
	private int prevOpinion, currentOpinion;
	
	public PlayerData(Player playerIn)
	{
		this.thePlayer = playerIn;
	}
	
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
	{
		return ExCapabilities.PLAYER_DATA.orEmpty(cap, LazyOptional.of(() -> this));
	}
	
	public static PlayerData getCapability(Player player)
	{
		if(player == null)
			return null;
		PlayerData data = player.getCapability(ExCapabilities.PLAYER_DATA).orElse(new PlayerData(player));
		data.thePlayer = player;
		return data;
	}
	
	public CompoundTag serializeNBT()
	{
		CompoundTag data = new CompoundTag();
		data.putString("Deity", this.deityName);
		
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
		return data;
	}
	
	public void deserializeNBT(CompoundTag nbt)
	{
		this.deityName = nbt.getString("Deity");
		
		ListTag quotients = nbt.getList("Values", Tag.TAG_COMPOUND);
		for(int i=0; i<quotients.size(); i++)
		{
			CompoundTag data = quotients.getCompound(i);
			this.quotients.put(new ResourceLocation(data.getString("Name")), data.getDouble("Val"));
		}
	}
	
	public String getDeityName() { return this.deityName; }
	@Nullable
	public Deity getDeity() { return DeityRegistry.getInstance().getDeity(this.deityName); }
	public void setDeity(Deity deityIn) { this.deityName = deityIn.simpleName(); markDirty(); }
	
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
	
	public void tick()
	{
		if(this.quotients.isEmpty())
			return;
		
		Map<ResourceLocation, Double> quotientMap = new HashMap<>();
		for(Entry<ResourceLocation, Double> entry : this.quotients.entrySet())
		{
			double val = entry.getValue() - QUOTIENT_DECAY;
			if(val > 0)
				quotientMap.put(entry.getKey(), val);
		}
		
		this.quotients.clear();
		this.quotients.putAll(quotientMap);
		markDirty();
	}
	
	public void markDirty()
	{
		if(this.thePlayer != null && !this.thePlayer.getLevel().isClientSide())
			PacketHandler.sendTo((ServerPlayer)this.thePlayer, new PacketSyncPlayerData(this.thePlayer.getUUID(), this));
	}
}
