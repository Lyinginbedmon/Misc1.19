package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends BrewingStandBlockEntity
{
	private static final int[] SLOTS_FOR_POTIONS = new int[] {0, 1, 2};
	
	public NonNullList<ItemStack> itemsPrev = NonNullList.withSize(5, ItemStack.EMPTY);
	public boolean brewEvent = false;
	
	/** Flags for each inventory slot that was altered by the last brew event */
	public NonNullList<Boolean> slotsBrewed = NonNullList.withSize(5, false);
	
	@Shadow
	private NonNullList<ItemStack> items;
	
	@Shadow
	int brewTime;
	
	@Shadow
	private static boolean isBrewable(NonNullList<ItemStack> itemsIn) { return false; }
	
	public BrewingStandBlockEntityMixin(BlockPos p_155283_, BlockState p_155284_) { super(p_155283_, p_155284_); }
	
	/** Clears the flag for a removed potion, to be called when the slot is taken from */
	public boolean removeItem(int slot) { return slotsBrewed.set(slot, false); }
	
	@Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V", at = @At("HEAD"))
	private static void serverTickStart(Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity brewingStand)
	{
		/**
		 * If isBrewable and brewTime == 1, then a brew event is about to happen
		 * Set the current contents of all potion slots in itemsPrev
		 */
		BrewingStandBlockEntityMixin stand = (BrewingStandBlockEntityMixin)brewingStand;
		boolean flag = isBrewable(stand.items);
		boolean flag1 = stand.brewTime == 1;	// This will be decremented to 0 by the main function body if flag == true
		if(stand.brewEvent = flag && flag1)
		{
			System.out.println("Firing brew event fired at "+pos.toString());
			for(int slot : SLOTS_FOR_POTIONS)
				stand.itemsPrev.set(slot, stand.getItem(slot));
		}
	}
	
	@Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V", at = @At("RETURN"))
	private static void serverTickEnd(Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity brewingStand)
	{
		/**
		 * If a brew event has happened, then
		 * Check all potion slots for contents different to itemsPrev
		 * Flag any changed non-empty slots as brewed
		 */
		BrewingStandBlockEntityMixin stand = (BrewingStandBlockEntityMixin)brewingStand;
		if(stand.brewEvent)
		{
			System.out.println("Brew event fired for brewing stand at "+pos.toString());
			stand.brewEvent = false;
			for(int slot : SLOTS_FOR_POTIONS)
				stand.slotsBrewed.set(slot, !stand.getItem(slot).equals(stand.itemsPrev.get(slot), false));
		}
	}
	
	/**
	 * TODO Identify specific player and specific slot when potion is removed<br>
	 * This is not given in PlayerBrewedPotionEvent >:|
	 * We need to know:
	 * 	The player whose quotient we need to modify
	 * 	The brewing stand being extracted from to know if the potion has been brewed
	 */
}
