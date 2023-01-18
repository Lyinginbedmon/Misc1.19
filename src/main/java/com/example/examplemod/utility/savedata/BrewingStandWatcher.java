package com.example.examplemod.utility.savedata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

// FIXME Convert to world save data to preserve values between loads
public class BrewingStandWatcher
{
	private static Map<BlockPos, BrewData> standData = new HashMap<>();
	private static Map<UUID, BlockPos> lastTouchedStand = new HashMap<>();
	
	public static void setLastTouched(UUID playerId, BlockPos pos) { lastTouchedStand.put(playerId, pos); }
	
	@Nullable
	public static BlockPos lastTouched(UUID playerId) { return lastTouchedStand.getOrDefault(playerId, null); }
	
	public static BrewData getData(BlockPos pos)
	{
		BrewData data = standData.getOrDefault(pos, new BrewData());
		if(!standData.containsKey(pos))
			standData.put(pos, data);
		return data;
	}
	
	public static void cycleSlot(BlockPos pos, int slot, ItemStack stack)
	{
		getData(pos).cycleSlot(slot, stack);
	}
	
	public static void emptySlot(BlockPos pos, int slot)
	{
		getData(pos).emptySlot(slot);
	}
	
	public static int extractStack(BlockPos pos, ItemStack stack)
	{
		return getData(pos).extract(stack);
	}
	
	private static class BrewData
	{
		private NonNullList<ItemStack> contents = NonNullList.withSize(3, ItemStack.EMPTY);
		private NonNullList<Integer> value = NonNullList.withSize(3, 0);
		
		public BrewData() { }
		
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
		
		@SuppressWarnings("unused")
		public boolean isEmpty()
		{
			for(int val : value)
				if(val > 0)
					return false;
			return true;
		}
	}
}
