package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.examplemod.utility.savedata.BrewingStandWatcher;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin
{
	private NonNullList<Integer> brewCycles = NonNullList.withSize(3, 0);
	
	private static boolean isBrewFinish = false;
	private static NonNullList<ItemStack> itemsBeforeBrew = NonNullList.withSize(3, ItemStack.EMPTY);
	
	@Shadow
	private static boolean isBrewable(NonNullList<ItemStack> p_155295_) { return false; }
	
	@Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V", at = @At("HEAD"))
	private static void serverTickHead(Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, final CallbackInfo ci)
	{
		AccessorBrewingStandBlockEntity accessor = (AccessorBrewingStandBlockEntity)entity;
		NonNullList<ItemStack> contents = accessor.getItems();
		
		boolean isBrewable = isBrewable(contents);
		boolean isBrewing = accessor.getBrewTime() == 1;
		if(isBrewable && isBrewing)
		{
			/**
			 * Identify contents of potion slots before brewing cycle is completed
			 * Mark brewing stand as about to complete a brewing cycle
			 */
			isBrewFinish = true;
			
			BrewingStandWatcher watcher = BrewingStandWatcher.instance(world);
			for(int i=0; i<3; i++)
			{
				itemsBeforeBrew.set(i, contents.get(i));
				if(contents.get(i).isEmpty())
					watcher.emptySlot(pos, i);
			}
		}
	}
	
	@Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V", at = @At("RETURN"))
	private static void serverTickFoot(Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, final CallbackInfo ci)
	{
		if(isBrewFinish)
		{
			AccessorBrewingStandBlockEntity accessor = (AccessorBrewingStandBlockEntity)entity;
			NonNullList<ItemStack> contents = accessor.getItems();
			
			/**
			 * Compare contents before tick to now and mark changed contents as brewed
			 */
			
			BrewingStandWatcher watcher = BrewingStandWatcher.instance(world);
			for(int i=0; i<3; i++)
			{
				if(!contents.get(i).equals(itemsBeforeBrew.get(i), false))
					watcher.cycleSlot(pos, i, contents.get(i));
			}
			
			isBrewFinish = false;
		}
	}
	
	public void incCycle(int slot) { brewCycles.set(slot%3, brewCycles.get(slot%3) + 1); }
	
	public int removeItem(int slot) { return brewCycles.set(slot%3, 0); }
}
