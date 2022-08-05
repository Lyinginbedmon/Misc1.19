package com.example.examplemod.entity.ai;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.Node.Decorator;
import com.example.examplemod.entity.ai.Node.LeafSingle;
import com.example.examplemod.entity.ai.Node.Parallel;
import com.example.examplemod.entity.ai.Node.Selector;
import com.example.examplemod.entity.ai.Node.Sequence;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.reference.Reference;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public class Branches
{
	/** Moves to random positions near self */
	public static final Node wanderBasic()
	{
		return new Checks.HasValue("has_blockpos", MobWhiteboard.MOB_POS_BLOCK, new Sequence("wander_cycle",
					new LeafSingle("set_random_pos")
					{
						public void doAction(Mob mobIn, Whiteboard<?> storage)
						{
							BlockPos mobPos = storage.getBlockPos(MobWhiteboard.MOB_POS_BLOCK);
							RandomSource random = mobIn.getRandom();
							
							BlockPos target = addRandom(mobPos, random);
							PathNavigation navigation = mobIn.getNavigation();
							int tries = 50;
							while(navigation.createPath(target, 64) == null && --tries > 0)
								target = addRandom(mobPos, random);
							
							storage.setValue("wander_target", target);
						}
						
						private BlockPos addRandom(BlockPos origin, RandomSource random) { return origin.offset(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5); }
					},
					new Parallel("movement",
							Actions.LookAtConstant.lazy("wander_target"),
						new Actions.MoveTo("wander_target", 0.35D)),
					new Actions.Wait(Reference.Values.TICKS_PER_SECOND * 3, Reference.Values.TICKS_PER_SECOND * 15)));
	}
	
	public static final Node wander()
	{
		return new Checks.HasValue("has_blockpos", MobWhiteboard.MOB_POS_BLOCK, new Sequence("wander_cycle",
				new LeafSingle("set_random_pos")
				{
					public void doAction(Mob mobIn, Whiteboard<?> storage)
					{
						BlockPos mobPos = storage.getBlockPos(MobWhiteboard.MOB_POS_BLOCK);
						RandomSource random = mobIn.getRandom();
						
						BlockPos target = addRandom(mobPos, random);
						PathNavigation navigation = mobIn.getNavigation();
						int tries = 50;
						while(navigation.createPath(target, 64) == null && --tries > 0)
							target = addRandom(mobPos, random);
						
						storage.setValue("wander_target", target);
					}
					
					private BlockPos addRandom(BlockPos origin, RandomSource random) { return origin.offset(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5); }
				},
				new Actions.MoveTo("wander_target", 0.35D),
				new Actions.Wait(Reference.Values.TICKS_PER_SECOND * 3, Reference.Values.TICKS_PER_SECOND * 15)));
	}
	
	/** Moves towards attack target and periodically uses melee attack when close enough */
	public static final Node attackMelee()
	{
		return new Parallel("melee_combat",
				Actions.LookAtConstant.instant(MobWhiteboard.MOB_TARGET),
				new Selector("melee_loop",
					new Decorator("move_closer", new Predicate<Pair<Mob,Whiteboard<?>>>()
					{
						public boolean apply(Pair<Mob,Whiteboard<?>> input)
						{
							Entity target = input.getSecond().getEntity(MobWhiteboard.MOB_TARGET);
							return target != null && input.getFirst().distanceToSqr(target) > Actions.AttackMelee.getAttackReachSqr(target, input.getFirst());
						}
					}, new Actions.MoveTo(MobWhiteboard.MOB_TARGET, 0.5D)),
					new Parallel("attack", Parallel.Style.OR,
						new Actions.Wait(Reference.Values.TICKS_PER_SECOND),
						new Actions.AttackMelee(MobWhiteboard.MOB_TARGET))));
	}
	
	public static final Node equipBestGear()
	{
		return equipBestGear(
				Whiteboard.Expansions.BEST_SWORD, 
				Whiteboard.Expansions.BEST_HEAD, 
				Whiteboard.Expansions.BEST_CHEST, 
				Whiteboard.Expansions.BEST_LEGS, 
				Whiteboard.Expansions.BEST_FEET);
	}
	
	public static final Node equipBestGear(String sword, String head, String chest, String legs, String feet)
	{
		return new Selector("equip_best_gear",
				equipSwordIfBetter(sword),
				equipArmorIfBetter(chest, EquipmentSlot.CHEST),
				equipArmorIfBetter(legs, EquipmentSlot.LEGS),
				equipArmorIfBetter(head, EquipmentSlot.HEAD),
				equipArmorIfBetter(feet, EquipmentSlot.FEET)).setLock();
	}
	
	public static final Node equipSwordIfBetter(String targetAddress)
	{
		return new Decorator("is_nearest_sword_better", new Predicate<Pair<Mob,Whiteboard<?>>>()
			{
				public boolean apply(Pair<Mob, Whiteboard<?>> input)
				{
					Mob mob = input.getFirst();
					Whiteboard<?> storage = input.getSecond();
					ItemEntity nearest = storage.hasValue(targetAddress) ? (ItemEntity)storage.getEntity(targetAddress) : null;
					if(nearest == null || nearest.isRemoved()) return false;
					
					ItemStack stack = nearest.getItem();
					if(stack.isEmpty()) return false;
					
					double stackDmg = getDamageBonus(stack, mob);
					double myDmg = getDamageBonus(mob.getMainHandItem(), mob);
					
					return stackDmg > myDmg || (stackDmg == myDmg && stack.getDamageValue() < mob.getMainHandItem().getDamageValue());
				}
			}, equipFromEntity(targetAddress));
	}
	
	public static final Node equipArmorIfBetter(String address, EquipmentSlot slot)
	{
		return new Decorator("is_nearest_"+slot.name().toLowerCase()+"_better", new Predicate<Pair<Mob,Whiteboard<?>>>()
		{
			public boolean apply(Pair<Mob, Whiteboard<?>> input)
			{
				Mob mob = input.getFirst();
				Whiteboard<?> storage = input.getSecond();
				ItemEntity nearest = storage.hasValue(address) ? (ItemEntity)storage.getEntity(address) : null;
				if(nearest == null || nearest.isRemoved())
					return false;
				
				ItemStack stack = nearest.getItem();
				if(stack.isEmpty())
					return false;
				
				return getArmorBonus(stack, slot) > getArmorBonus(mob.getItemBySlot(slot), slot);
			}
		}, equipFromEntity(address));
	}
	
	public static double getDamageBonus(ItemStack stack, @Nullable Mob mobIn)
	{
		if(stack.isEmpty())
			return 0D;
		
		double bonus = EnchantmentHelper.getDamageBonus(stack, (mobIn == null || mobIn.getTarget() == null) ? MobType.UNDEFINED : mobIn.getTarget().getMobType());
		
		Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
		for(AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE))
			bonus += modifier.getAmount();
		
		return bonus;
	}
	
	public static double getArmorBonus(ItemStack stack, EquipmentSlot slot)
	{
		if(stack.isEmpty()) return 0D;
		double bonus = 0;
		Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
		for(AttributeModifier modifier : modifiers.get(Attributes.ARMOR))
			bonus += modifier.getAmount();
		return bonus;
	}
	
	/** Attempts to equip the item entity at the given whiteboard address */
	public static final Node equipFromEntity(String targetAddressIn)
	{
		Predicate<Pair<Mob,Whiteboard<?>>> entityValid = new Predicate<Pair<Mob,Whiteboard<?>>>()
		{
			public boolean apply(Pair<Mob,Whiteboard<?>> input)
			{
				Whiteboard<?> storage = input.getSecond();
				return storage.hasValue(targetAddressIn) && storage.getEntity(targetAddressIn) != null && !storage.getEntity(targetAddressIn).isRemoved();
			}
		};
		
		return new Decorator("has_valid_target", entityValid, new Sequence("equip", moveToPickUp(targetAddressIn).setToInterrupt(Predicates.not(entityValid)), Actions.equipHeldItem()));
	}
	
	/** Moves towards the given item entity and attempts to pick it up */
	public static final Node moveToPickUp(String targetAddressIn)
	{
		return new Parallel("pickup",
				Actions.LookAtConstant.instant(targetAddressIn),
				new Selector("move_to_pick_up",
					Branches.tryPickUp(targetAddressIn),
					new Decorator("need_move", new Predicate<Pair<Mob,Whiteboard<?>>>()
					{
						public boolean apply(Pair<Mob,Whiteboard<?>> input)
						{
							Entity target = input.getSecond().getEntity(targetAddressIn);
							Mob mob = input.getFirst();
							return target != null && !mob.getBoundingBox().inflate(1).intersects(target.getBoundingBox());
						}
					}, new Actions.MoveTo(targetAddressIn, 0.5D))));
	}
	
	/** Attempts to pick up the given item entity, manipulating hand inventory if necessary */
	public static final Node tryPickUp(String targetAddress)
	{
		return new Selector("try_pickup",
				new Actions.PickUpItem(targetAddress),
				new Checks.HasItemInSlot("hand_full", EquipmentSlot.MAINHAND, 
					new Selector("clear_main_hand",
						new Checks.IsSlotEmpty("offhand_empty", EquipmentSlot.OFFHAND, Actions.swapItems()),
						new Actions.DropItem())));
	}
	
	public static final Node lookRandom(int min, int max)
	{
		return new Sequence("random_look",
				new LeafSingle("set_random_look")
				{
					public void doAction(Mob mobIn, Whiteboard<?> storage)
					{
						Vec3 eyePos = mobIn.getEyePosition();
						Vec3 target = addRandom(eyePos, mobIn.getRandom());
						storage.setValue("look_target", target);
					}
					
					private Vec3 addRandom(Vec3 origin, RandomSource random)
					{
						int amount = 1 + random.nextInt(5);
						double xOff = (random.nextDouble() - 0.5D) * amount;
						double yOff = (random.nextDouble() - 0.5D);
						double zOff = (random.nextDouble() - 0.5D) * amount;
						return origin.add(xOff, yOff, zOff);
					}
				},
				new Parallel("looking", Actions.LookAtConstant.normal("look_target"), new Actions.Wait(min, max)));
	}
}
