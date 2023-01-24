package com.example.examplemod.utility.savedata;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;

import com.example.examplemod.reference.Reference;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class BrewingStandWatcher extends SavedData
{
	protected static final String DATA_NAME = Reference.ModInfo.MOD_ID+"_brewing_stand_watcher";
	
	private Map<BlockPos, BrewData> standData = new HashMap<>();
	private Map<UUID, BlockPos> lastTouchedStand = new HashMap<>();
	
	public void setLastTouched(UUID playerId, BlockPos pos)
	{
		lastTouchedStand.put(playerId, pos);
		setDirty();
	}
	
	@Nullable
	public BlockPos lastTouched(UUID playerId) { return lastTouchedStand.getOrDefault(playerId, null); }
	
	public static BrewingStandWatcher instance(Level worldIn)
	{
		if(worldIn.isClientSide())
			return new BrewingStandWatcher();
		
		return ((ServerLevel)worldIn).getDataStorage().computeIfAbsent(BrewingStandWatcher::fromNbt, BrewingStandWatcher::new, DATA_NAME);
	}
	
	public CompoundTag save(CompoundTag data)
	{
		ListTag stands = new ListTag();
		
		for(Entry<BlockPos, BrewData> entry : standData.entrySet())
		{
			if(entry.getValue().isEmpty()) continue;
			CompoundTag tag = new CompoundTag();
			tag.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
			tag.put("Stand", entry.getValue().writeToNbt(new CompoundTag()));
			stands.add(tag);
		}
		data.put("Stands", stands);
		
		ListTag touches = new ListTag();
		for(Entry<UUID, BlockPos> entry : lastTouchedStand.entrySet())
		{
			CompoundTag tag = new CompoundTag();
			tag.putUUID("ID", entry.getKey());
			tag.put("Pos", NbtUtils.writeBlockPos(entry.getValue()));
			touches.add(tag);
		}
		data.put("Touches", touches);
		return data;
	}
	
	public void read(CompoundTag data)
	{
		standData.clear();
		if(data.contains("Stands", Tag.TAG_LIST))
		{
			ListTag stands = data.getList("Stands", Tag.TAG_COMPOUND);
			for(int i=0; i<stands.size(); i++)
			{
				CompoundTag tag = stands.getCompound(i);
				standData.put(NbtUtils.readBlockPos(tag.getCompound("Pos")), BrewData.fromNbt(tag.getCompound("Stand")));
			}
		}
		
		lastTouchedStand.clear();
		if(data.contains("Touches", Tag.TAG_LIST))
		{
			ListTag touches = data.getList("Touches", Tag.TAG_COMPOUND);
			for(int i=0; i<touches.size(); i++)
			{
				CompoundTag tag = touches.getCompound(i);
				lastTouchedStand.put(tag.getUUID("ID"), NbtUtils.readBlockPos(tag.getCompound("Pos")));
			}
		}
	}
	
	public static BrewingStandWatcher fromNbt(CompoundTag tag)
	{
		BrewingStandWatcher watcher = new BrewingStandWatcher();
		watcher.read(tag);
		return watcher;
	}
	
	private BrewData getData(BlockPos pos)
	{
		BrewData data = standData.getOrDefault(pos, new BrewData());
		if(!standData.containsKey(pos))
			standData.put(pos, data);
		return data;
	}
	
	public void cycleSlot(BlockPos pos, int slot, ItemStack stack)
	{
		getData(pos).cycleSlot(slot, stack);
		setDirty();
	}
	
	public void emptySlot(BlockPos pos, int slot)
	{
		getData(pos).emptySlot(slot);
		setDirty();
	}
	
	public int extractStack(BlockPos pos, ItemStack stack)
	{
		setDirty();
		return getData(pos).extract(stack);
	}
	
	private static class BrewData
	{
		private NonNullList<ItemStack> contents = NonNullList.withSize(3, ItemStack.EMPTY);
		private NonNullList<Integer> value = NonNullList.withSize(3, 0);
		
		public BrewData() { }
		
		public CompoundTag writeToNbt(CompoundTag data)
		{
			ListTag stacks = new ListTag();
			contents.forEach((stack) -> stacks.add(stack.save(new CompoundTag())));
			data.put("Stacks", stacks);
			data.putIntArray("Values", value);
			return data;
		}
		
		public static BrewData fromNbt(CompoundTag tag)
		{
			BrewData data = new BrewData();
			
			ListTag stacks = tag.getList("Stacks", Tag.TAG_COMPOUND);
			for(int i=0; i<data.contents.size(); i++)
				data.contents.set(i, ItemStack.of(stacks.getCompound(i)));
			
			int[] values = tag.getIntArray("Values");
			for(int i=0; i<data.value.size(); i++)
				data.value.set(i, values[i]);
			return data;
		}
		
		/**
		 * Increments the value of the given slot and sets the known contents of it.
		 * @param slot
		 * @param stack
		 */
		public void cycleSlot(int slot, ItemStack stack)
		{
			value.set(slot, value.get(slot) + 1);
			contents.set(slot, stack.copy());
		}
		
		/**
		 * Finds the first matching stack in this brewing stand and returns the stored value of it.<br>
		 * The value and contents are then cleared.
		 * @param stack
		 * @return
		 */
		public int extract(ItemStack stack)
		{
			for(int i=0; i<3; i++)
				if(contents.get(i).equals(stack, false))
					return emptySlot(i);
			
			return 0;
		}
		
		public int emptySlot(int slot)
		{
			contents.set(slot, ItemStack.EMPTY);
			return value.set(slot, 0);
		}
		
		public boolean isEmpty()
		{
			for(int val : value)
				if(val > 0)
					return false;
			return true;
		}
	}
}
