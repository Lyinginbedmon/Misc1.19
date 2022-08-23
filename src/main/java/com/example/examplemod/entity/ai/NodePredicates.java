package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.ai.TreeNode.NodePredicate;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.google.common.base.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class NodePredicates
{
	public static final NodePredicate HAS_LIVING_TARGET = (mob, storage) ->
			{
				Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
				return storage.hasValue(MobWhiteboard.MOB_TARGET) && target != null && target.isAlive() && !target.isRemoved();
			};
	
	public static final NodePredicate CAN_SEE_TARGET = (mob, storage) ->
			{
				return HAS_LIVING_TARGET.test(mob, storage) && storage.getBoolean(MobWhiteboard.MOB_TARGET_VISIBLE);
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
}