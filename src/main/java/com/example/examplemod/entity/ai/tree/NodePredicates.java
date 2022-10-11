package com.example.examplemod.entity.ai.tree;

import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.tree.TreeNode.NodePredicate;
import com.example.examplemod.utility.GroupSaveData;
import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NodePredicates
{
	public static final NodePredicate HAS_LIVING_TARGET = (mob, storage) ->
			{
				Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
				return storage.hasValue(MobWhiteboard.ATTACK_TARGET) && target != null && target.isAlive() && !target.isRemoved();
			};
	
	public static final NodePredicate CAN_SEE_TARGET = (mob, storage) ->
			{
				return HAS_LIVING_TARGET.test(mob, storage) && storage.getBoolean(MobWhiteboard.MOB_TARGET_VISIBLE);
			};
	public static final NodePredicate IN_GROUP = (mob, storage) ->
			{
				return GroupSaveData.get(mob.getServer()).isInAnyGroup(mob);
			};
	
	public static NodePredicate hasValue(String address)
	{
		return (mob, storage) -> storage.hasValue(address);
	}
	
	public static NodePredicate canPathTo(String addressIn)
	{
		return (mob, storage) ->
		{
				Vec3 dest = Whiteboard.getDest(storage, addressIn);
				if(dest == null)
					return false;
				return mob.getNavigation().createPath(dest.x, dest.y, dest.z, 1) != null;
		};
	}
	
	public static NodePredicate isSlotEmpty(EquipmentSlot slot)
	{
		return (mob, storage) -> MobWhiteboard.getItemInSlot(storage, slot).isEmpty();
	}
	
	public static NodePredicate hasItemInSlot(EquipmentSlot slot)
	{
		return (mob, storage) -> !MobWhiteboard.getItemInSlot(storage, slot).isEmpty();
	}
	
	public static NodePredicate isItemValid(Predicate<ItemStack> predicateIn, String addressIn)
	{
		return (mob, storage) -> storage.hasValue(addressIn) && predicateIn.apply(storage.getItemStack(addressIn));
	}
	
	public static NodePredicate isTimerZero(ResourceLocation addressIn)
	{
		return (mob, storage) -> { return storage.getTimer(addressIn) == 0; };
	}
	
	public static NodePredicate isBlockMinable(String addressIn)
	{
		return (mob,storage) -> 
		{
			Level world = mob.getLevel();
			BlockPos pos = storage.getBlockPos(addressIn);
			BlockState state = world.getBlockState(pos);
			return !(world.isEmptyBlock(pos) || state.is(BlockTags.WITHER_IMMUNE) || state.getBlock().defaultDestroyTime() < 0F);
		};
	}
}