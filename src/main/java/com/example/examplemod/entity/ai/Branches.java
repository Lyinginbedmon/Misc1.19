package com.example.examplemod.entity.ai;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.Actions.WaitUntil;
import com.example.examplemod.entity.ai.TreeNode.*;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.reference.Reference;
import com.google.common.collect.Multimap;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public class Branches
{
	/** Moves to random positions near self */
	public static final TreeNode wanderBasic()
	{
		return Sequence.sequence(
					new Condition(Checks.hasValue(MobWhiteboard.MOB_POS_BLOCK)),
					new LeafSingle()
					{
						public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
						{
							BlockPos mobPos = storage.getBlockPos(MobWhiteboard.MOB_POS_BLOCK);
							RandomSource random = mobIn.getRandom();
							
							BlockPos target = addRandom(mobPos, random);
							PathNavigation navigation = mobIn.getNavigation();
							int tries = 50;
							while(navigation.createPath(target, 64) == null && --tries > 0)
								target = addRandom(mobPos, random);
							
							storage.setValue("wander_target", target);
							return true;
						}
						
						private BlockPos addRandom(BlockPos origin, RandomSource random) { return origin.offset(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5); }
					},
					Sequence.reactive(
						Actions.LookAtConstant.lazy("wander_target"),
						new Actions.MoveTo("wander_target", 0.35D)),
					new Actions.Wait(Reference.Values.TICKS_PER_SECOND * 3, Reference.Values.TICKS_PER_SECOND * 15));
	}
	
	public static final TreeNode wander()
	{
		return Sequence.sequence(
				new Condition(Checks.hasValue(MobWhiteboard.MOB_POS_BLOCK)),
				new LeafSingle()
				{
					public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
					{
						BlockPos mobPos = storage.getBlockPos(MobWhiteboard.MOB_POS_BLOCK);
						RandomSource random = mobIn.getRandom();
						
						BlockPos target = addRandom(mobPos, random);
						PathNavigation navigation = mobIn.getNavigation();
						int tries = 50;
						while(navigation.createPath(target, 64) == null && --tries > 0)
							target = addRandom(mobPos, random);
						
						storage.setValue("wander_target", target);
						return true;
					}
					
					private BlockPos addRandom(BlockPos origin, RandomSource random) { return origin.offset(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5); }
				}.setCustomName("set_destination"),
				new Actions.MoveTo("wander_target", 0.35D),
				new Actions.Wait(Reference.Values.TICKS_PER_SECOND * 3, Reference.Values.TICKS_PER_SECOND * 15)).setCustomName("basic_wander");
	}
	
	/** Moves towards attack target and periodically uses melee attack when close enough */
	public static final TreeNode attackMelee()
	{
		return Sequence.reactive(
				Actions.LookAtConstant.instant(MobWhiteboard.MOB_TARGET),
				new Selector(
					TreeNode.conditional((mob, storage) ->
					{
						Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
						return target != null && mob.distanceToSqr(target) > Actions.AttackMelee.getAttackReachSqr(target, mob);
					}, new Actions.MoveTo(MobWhiteboard.MOB_TARGET, 0.5D)).setCustomName("move_closer").setDiscrete(),
					Sequence.sequence(
						new Actions.Wait(Reference.Values.TICKS_PER_SECOND),
						new Actions.AttackMelee(MobWhiteboard.MOB_TARGET)).setCustomName("basic_melee").setDiscrete())).setCustomName("melee_attacks");
	}
	
	public static final TreeNode equipBestGear(double speed)
	{
		return equipBestGear(
				Whiteboard.Expansions.BEST_SWORD, 
				Whiteboard.Expansions.BEST_HEAD, 
				Whiteboard.Expansions.BEST_CHEST, 
				Whiteboard.Expansions.BEST_LEGS, 
				Whiteboard.Expansions.BEST_FEET, speed).setCustomName("equip_best_gear");
	}
	
	public static final TreeNode equipBestGear(String sword, String head, String chest, String legs, String feet, double speed)
	{
		return new Selector(
				equipSwordIfBetter(sword, speed),
				equipArmorIfBetter(chest, EquipmentSlot.CHEST, speed),
				equipArmorIfBetter(legs, EquipmentSlot.LEGS, speed),
				equipArmorIfBetter(head, EquipmentSlot.HEAD, speed),
				equipArmorIfBetter(feet, EquipmentSlot.FEET, speed)).setCustomName("equip_best_gear");
	}
	
	public static final TreeNode equipSwordIfBetter(String targetAddress, double speed)
	{
		return TreeNode.conditional((mob, storage) ->
				{
					ItemEntity nearest = storage.hasValue(targetAddress) ? (ItemEntity)storage.getEntity(targetAddress) : null;
					if(nearest == null || nearest.isRemoved()) return false;
					
					ItemStack stack = nearest.getItem();
					if(stack.isEmpty()) return false;
					
					double stackDmg = getDamageBonus(stack, mob);
					double myDmg = getDamageBonus(mob.getMainHandItem(), mob);
					
					return stackDmg > myDmg || (stackDmg == myDmg && stack.getDamageValue() < mob.getMainHandItem().getDamageValue());
				}, equipFromEntity(targetAddress, speed)).setCustomName("equip_sword_if_better").setDiscrete();
	}
	
	public static final TreeNode equipArmorIfBetter(String address, EquipmentSlot slot, double speed)
	{
		return TreeNode.conditional((mob, storage) ->
		{
			ItemEntity nearest = storage.hasValue(address) ? (ItemEntity)storage.getEntity(address) : null;
			if(nearest == null || nearest.isRemoved())
				return false;
			
			ItemStack stack = nearest.getItem();
			if(stack.isEmpty())
				return false;
			
			return getArmorBonus(stack, slot) > getArmorBonus(mob.getItemBySlot(slot), slot);
		}, equipFromEntity(address, speed)).setCustomName("equip_"+slot.name().toLowerCase()+"_if_better").setDiscrete();
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
	public static final TreeNode equipFromEntity(String targetAddressIn, double speed)
	{
		NodePredicate entityValid = (mob, storage) ->
		{
			return storage.hasValue(targetAddressIn) && storage.getEntity(targetAddressIn) != null && !storage.getEntity(targetAddressIn).isRemoved();
		};
		
		return TreeNode.conditional(entityValid, Sequence.sequence( 
				TreeNode.conditional((mob, storage) ->
				{
					Entity target = storage.getEntity(targetAddressIn);
					return target != null && !mob.getBoundingBox().inflate(1,0,1).intersects(target.getBoundingBox());
				}, new Actions.MoveTo(targetAddressIn, speed)), 
				Actions.equipFromEntity(targetAddressIn))).setCustomName("equip_from_entity");
	}
	
	/** Moves towards the given item entity and attempts to pick it up */
	public static final TreeNode moveToPickUp(String targetAddressIn)
	{
		return Sequence.reactive(
				Actions.LookAtConstant.instant(targetAddressIn),
				new Selector(
					Branches.tryPickUp(targetAddressIn),
					TreeNode.conditional((mob, storage) ->
					{
						Entity target = storage.getEntity(targetAddressIn);
						return target != null && !mob.getBoundingBox().inflate(1,0,1).intersects(target.getBoundingBox());
					}, new Actions.MoveTo(targetAddressIn, 0.5D))));
	}
	
	/** Attempts to pick up the given item entity, manipulating hand inventory if necessary */
	public static final TreeNode tryPickUp(String targetAddress)
	{
		return new Selector(
				new Actions.PickUpItem(targetAddress),
				Sequence.reactive(
					new Condition(Checks.hasItemInSlot(EquipmentSlot.MAINHAND)), 
					new Selector(
						TreeNode.conditional(Checks.isSlotEmpty(EquipmentSlot.OFFHAND), Actions.swapItems()),
						new Actions.DropItem()))).setCustomName("try_pick_up");
	}
	
	public static final TreeNode lookRandom(int min, int max)
	{
		return Sequence.sequence(
				new LeafSingle()
				{
					public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
					{
						Vec3 eyePos = mobIn.getEyePosition();
						Vec3 target = addRandom(eyePos, mobIn.getRandom());
						storage.setValue("look_target", target);
						return true;
					}
					
					private Vec3 addRandom(Vec3 origin, RandomSource random)
					{
						int amount = 1 + random.nextInt(5);
						double xOff = (random.nextDouble() - 0.5D) * amount;
						double yOff = (random.nextDouble() - 0.5D);
						double zOff = (random.nextDouble() - 0.5D) * amount;
						return origin.add(xOff, yOff, zOff);
					}
				}.setCustomName("set_target"),
				Sequence.reactive(
					Decorator.forceFailure(new Actions.Wait(min, max)),
					Actions.LookAtConstant.normal("look_target"))).setCustomName("random_look");
	}
	
	public static final TreeNode rangeAttackMotion()
	{
		NodePredicate targetClose = (mob, storage) ->
		{
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
			return target != null && target.distanceTo(mob) <= Actions.AttackMelee.getAttackReachSqr(mob, target) + 1D;
		};
		
		NodePredicate targetFar = (mob, storage) ->
		{
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
			return target != null && target.distanceTo(mob) > Actions.AttackMelee.getAttackReachSqr(mob, target) + 5D;
		};
		
		return new Selector(
				TreeNode.conditional(targetFar, new Actions.MoveTo(MobWhiteboard.MOB_TARGET, 0.5D)).setCustomName("move_closer").setDiscrete(),
				TreeNode.conditional(targetClose, new Actions.MoveAwayFrom(MobWhiteboard.MOB_TARGET, 0.5D, 8D)).setCustomName("move_farther").setDiscrete()).setCustomName("manage_distance");
	}
	
	public static final TreeNode attackRanged()
	{
		return Parallel.any(
				rangeAttackMotion(),
				new Selector(attackRangeBow(), attackRangeCrossbow())).setCustomName("ranged_attacks");
	}
	
	public static final TreeNode attackRangeBow()
	{
		/** Returns true if ticks using is at least 1 second and target is too close OR ticks using is at least 3 seconds and target is far away */
		NodePredicate shouldShoot = (mob, storage) ->
		{
			int ticksUsing = storage.getInt(MobWhiteboard.MOB_TICKS_USING);
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
			if(target == null || !target.isAlive() || !mob.getSensing().hasLineOfSight(target))
				return false;
			else
			{
				if(target.distanceTo(mob) > Actions.AttackMelee.getAttackReachSqr(mob, target))
					return ticksUsing >= (Reference.Values.TICKS_PER_SECOND * 3);
				else
					return ticksUsing >= Reference.Values.TICKS_PER_SECOND;
			}
		};
		
		return TreeNode.conditional((mob, storage) ->
		{
			return mob.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof net.minecraft.world.item.BowItem;
		}, Sequence.sequence(
					Actions.startUsingItem(),
					new WaitUntil(shouldShoot),
					new LeafSingle()
					{
						public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
						{
							LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
							int drawTime = storage.getInt(MobWhiteboard.MOB_TICKS_USING);
							float draw = BowItem.getPowerForTime(drawTime);
							
							mobIn.stopUsingItem();
							
							ItemStack bowStack = mobIn.getProjectile(mobIn.getItemInHand(ProjectileUtil.getWeaponHoldingHand(mobIn, item -> item instanceof net.minecraft.world.item.BowItem)));
							AbstractArrow arrow = ProjectileUtil.getMobArrow(mobIn, bowStack, draw);
							if (mobIn.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
								arrow = ((net.minecraft.world.item.BowItem)mobIn.getMainHandItem().getItem()).customArrow(arrow);
							
							double d0 = target.getX() - mobIn.getX();
							double d1 = target.getY(0.3333333333333333D) - arrow.getY();
							double d2 = target.getZ() - mobIn.getZ();
							double d3 = Math.sqrt(d0 * d0 + d2 * d2);
							arrow.shoot(d0, d1 + d3 * (double)0.2F, d2, 1.6F, (float)(14 - mobIn.level.getDifficulty().getId() * 4));
							
							mobIn.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (mobIn.getRandom().nextFloat() * 0.4F + 0.8F));
							mobIn.level.addFreshEntity(arrow);
							return true;
						}
					}.setCustomName("shoot_bow"))).setCustomName("bow_attack");
	}
	
	public static final TreeNode attackRangeCrossbow()
	{
		return TreeNode.conditional((mob, storage) -> { return mob.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof net.minecraft.world.item.CrossbowItem; },
				new Selector(
					TreeNode.conditional((mob, storage) ->
					{
						return !CrossbowItem.isCharged(mob.getItemInHand(InteractionHand.MAIN_HAND));
					}, Sequence.sequence(
							Actions.startUsingItem(),
							new WaitUntil((mob, storage) ->
							{
								int ticksUsing = storage.getInt(MobWhiteboard.MOB_TICKS_USING);
								int ticksNeeded = CrossbowItem.getChargeDuration(mob.getUseItem());
								return ticksUsing >= ticksNeeded;
							}),
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
								{
									ItemStack stack = mobIn.getUseItem();
									mobIn.releaseUsingItem();
									CrossbowItem.setCharged(stack, true);
									mobIn.setItemInHand(InteractionHand.MAIN_HAND, stack);
									return true;
								}
							}.setCustomName("finish_loading")).setCustomName("reload")).setCustomName("if_unloaded"),
					TreeNode.conditional((mob, storage) ->
					{
						return CrossbowItem.isCharged(mob.getItemInHand(InteractionHand.MAIN_HAND)) && storage.hasValue(MobWhiteboard.MOB_TARGET) && mob.getSensing().hasLineOfSight(storage.getEntity(MobWhiteboard.MOB_TARGET));
					}, new LeafSingle()
						{
							public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
							{
								LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.MOB_TARGET);
								if(target == null || !target.isAlive())
									return false;
								
								ItemStack crossbow = mobIn.getItemInHand(InteractionHand.MAIN_HAND);
								
								Projectile arrow = ((ArrowItem)Items.ARROW).createArrow(mobIn.getLevel(), crossbow, mobIn);
								double d0 = target.getX() - mobIn.getX();
							    double d1 = target.getZ() - mobIn.getZ();
							    double d2 = Math.sqrt(d0 * d0 + d1 * d1);
							    double d3 = target.getY(0.3333333333333333D) - arrow.getY() + d2 * (double)0.2F;
							    Vector3f vector3f = getProjectileShotVector(mobIn, new Vec3(d0, d3, d1), 0F);
							    arrow.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), 1.6F, (float)(14 - mobIn.level.getDifficulty().getId() * 4));
							    mobIn.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (mobIn.getLevel().getRandom().nextFloat() * 0.4F + 0.8F));
							    mobIn.getLevel().addFreshEntity(arrow);
							    
							    CrossbowItem.setCharged(crossbow, false);
							    mobIn.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
							    return true;
							}
							
							private Vector3f getProjectileShotVector(LivingEntity p_32333_, Vec3 p_32334_, float p_32335_)
							{
								Vec3 vec3 = p_32334_.normalize();
								Vec3 vec31 = vec3.cross(new Vec3(0.0D, 1.0D, 0.0D));
								if(vec31.lengthSqr() <= 1.0E-7D)
								   vec31 = vec3.cross(p_32333_.getUpVector(1.0F));
								
								Quaternion quaternion = new Quaternion(new Vector3f(vec31), 90.0F, true);
								Vector3f vector3f = new Vector3f(vec3);
								vector3f.transform(quaternion);
								Quaternion quaternion1 = new Quaternion(vector3f, p_32335_, true);
								Vector3f vector3f1 = new Vector3f(vec3);
								vector3f1.transform(quaternion1);
								return vector3f1;
							}
						}.setCustomName("shoot_crossbow")))).setCustomName("crossbow_attack");
	}
}
