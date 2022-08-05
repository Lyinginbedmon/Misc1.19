package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.ai.Node.Decorator;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.google.common.base.Predicate;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class Checks
{
	public static final Predicate<Pair<Mob,Whiteboard<?>>> HAS_TARGET = new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob,Whiteboard<?>> input)
				{
					Whiteboard<?> storage = input.getSecond();
					return storage.hasValue(MobWhiteboard.MOB_TARGET) && storage.getEntity(MobWhiteboard.MOB_TARGET) != null;
				}
			};
	
	public static class HasValue extends Decorator
	{
		public HasValue(String nameIn, String address, Node childIn)
		{
			super(nameIn, new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input){ return input.getSecond().hasValue(address); }
			}, childIn);
		}
	}
	
	public static class CanPathTo extends Decorator
	{
		public CanPathTo(String nameIn, String addressIn, Node childIn)
		{
			super(nameIn, new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input)
				{
					Vec3 dest = Whiteboard.getDest(input.getSecond(), addressIn);
					if(dest == null)
						return false;
					return input.getFirst().getNavigation().createPath(dest.x, dest.y, dest.z, 1) != null;
				}
			}, childIn);
		}
	}
	
	public static class IsSlotEmpty extends Decorator
	{
		public IsSlotEmpty(String nameIn, EquipmentSlot slot, Node childIn)
		{
			super(nameIn, new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input)
				{
					return MobWhiteboard.getItemInSlot(input.getSecond(), slot).isEmpty();
				}
			}, childIn);
		}
	}
	
	public static class HasItemInSlot extends Decorator
	{
		public HasItemInSlot(String nameIn, EquipmentSlot slot, Node childIn)
		{
			super(nameIn, new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input){ return !MobWhiteboard.getItemInSlot(input.getSecond(), slot).isEmpty(); }
			}, childIn);
		}
	}
	
	public static class IsItemValid extends Decorator
	{
		public IsItemValid(String nameIn, Predicate<ItemStack> predicateIn, String addressIn, Node childIn)
		{
			super(nameIn, new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input)
				{
					Whiteboard<?> storage = input.getSecond();
					return storage.hasValue(addressIn) && predicateIn.apply(storage.getItemStack(addressIn));
				}
			}, childIn);
		}
	}
}