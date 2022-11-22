package com.example.examplemod.entity.ai.tree;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.entity.ai.group.action.GroupAction;
import com.example.examplemod.entity.ai.tree.Actions.WaitUntil;
import com.example.examplemod.entity.ai.tree.TreeNode.Condition;
import com.example.examplemod.entity.ai.tree.TreeNode.Decorator;
import com.example.examplemod.entity.ai.tree.TreeNode.LeafRunning;
import com.example.examplemod.entity.ai.tree.TreeNode.LeafSingle;
import com.example.examplemod.entity.ai.tree.TreeNode.NodePredicate;
import com.example.examplemod.entity.ai.tree.TreeNode.Selector;
import com.example.examplemod.entity.ai.tree.TreeNode.Sequence;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public class Branches
{
	/** Moves to random positions near self */
	public static final TreeNode wanderBasic()
	{
		return Sequence.sequential(
					new Condition(NodePredicates.hasValue(MobWhiteboard.MOB_POS_BLOCK)),
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
		return wanderAround(MobWhiteboard.MOB_POS_BLOCK, Reference.Values.TICKS_PER_SECOND * 3, Reference.Values.TICKS_PER_SECOND * 15).setCustomName("wander").setDiscrete();
	}
	
	public static final TreeNode wanderAround(String address, int minWait, int maxWait)
	{
		return Sequence.sequential(
				new Condition(NodePredicates.hasValue(address)),
				new LeafSingle()
				{
					public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
					{
						BlockPos mobPos = storage.getBlockPos(address);
						RandomSource random = mobIn.getRandom();
						
						BlockPos target = addRandom(mobPos, random, 10);
						PathNavigation navigation = mobIn.getNavigation();
						int tries = 50;
						while(navigation.createPath(target, 64) == null && --tries > 0)
							target = addRandom(mobPos, random, 10);
						
						storage.setValue("wander_target", target);
						return true;
					}
					
					private BlockPos addRandom(BlockPos origin, RandomSource random, double range)
					{
						double offX = (random.nextDouble() - 0.5D) * range;
						double offY = (random.nextDouble() - 0.5D) * range * 0.4D;
						double offZ = (random.nextDouble() - 0.5D) * range;
						return origin.offset(offX, offY, offZ);
					}
				}.setCustomName("set_destination"),
				new Actions.MoveTo("wander_target", 0.35D),
				new Actions.Wait(minWait, maxWait));
	}
	
	/** Moves towards attack target and periodically uses melee attack when close enough */
	public static final TreeNode attackMelee()
	{
		return Sequence.reactive(
					Actions.LookAtConstant.instant(MobWhiteboard.ATTACK_TARGET),
					Selector.sequential(
						TreeNode.conditional((mob, storage) -> { return storage.getTimer(MobWhiteboard.MOB_MELEE_COOLDOWN) > 0; }, Selector.sequential(
								Sequence.sequential(
										new Condition((mob, storage) -> { return storage.getItemStack(MobWhiteboard.getSlotAddress(EquipmentSlot.MAINHAND)).is(Items.SHIELD) || storage.getItemStack(MobWhiteboard.getSlotAddress(EquipmentSlot.OFFHAND)).is(Items.SHIELD); }).setCustomName("has_shield"),
										new Condition((mob, storage) -> { return !storage.getItemStack(MobWhiteboard.MOB_ITEM_USING).is(Items.SHIELD); }).setCustomName("not_using_shield"),
										swapHandsIfInvalid((stack) -> stack.is(Items.SHIELD)),
										Actions.startUsingItem()).setCustomName("use_shield").setDiscrete(),
								Sequence.sequential(
										// Set destination, respecting group strategy
										new LeafSingle()
										{
											public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
											{
												storage.clearValue("fallback_pos");
												
												Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
												List<BlockPos> positions = Lists.newArrayList();
												// Generate list of viable destinations away from target
												
												// Search range
												double minRange = Actions.AttackMelee.getAttackReachSqr(mob, target);
												double maxRange = minRange * 2;
												
												// Direction away from target
												Vec3i offset = mob.blockPosition().subtract(target.blockPosition());
												PathNavigation navigator = mob.getNavigation();
												
												for(int x=(offset.getX() == 0 ? -(int)minRange : 0); x<maxRange; x++)
													for(int z=(offset.getZ() == 0 ? -(int)minRange : 0); z<maxRange; z++)
														for(int y=-3; y<3; y++)
														{
															BlockPos testPos = target.blockPosition().offset(x*offset.getX(), y, z*offset.getZ());
															// Exclude any positions that are too close or that we can't move to
															if(testPos.distSqr(target.blockPosition()) < minRange || navigator.createPath(testPos, (int)maxRange + 1) == null)
																continue;
															
															positions.add(testPos);
														}
												
												if(!positions.isEmpty())
													storage.setValue("fallback_pos", positions.get(0));
												
												return storage.hasValue("fallback_pos");
											}
										},
										new Actions.MoveTo("fallback_pos", 0.5D)
										).setCustomName("fall_back")
							)).setCustomName("waiting_to_attack"),
						TreeNode.conditional((mob, storage) ->
						{
							Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
							return target != null && mob.distanceToSqr(target) > Actions.AttackMelee.getAttackReachSqr(target, mob);
						}, new Actions.MoveTo(MobWhiteboard.ATTACK_TARGET, 0.5D)).setCustomName("move_closer").setDiscrete(),
						Sequence.sequential(
							new Actions.AttackMelee(MobWhiteboard.ATTACK_TARGET),
							Actions.setWhiteboardTimer(MobWhiteboard.MOB_MELEE_COOLDOWN, Reference.Values.TICKS_PER_SECOND)).setCustomName("melee_attack").setDiscrete()
					)).setCustomName("melee_attack").setDiscrete();
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
		return Selector.sequential(
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
		return Sequence.sequential(
				Decorator.forceSuccess(
					Sequence.reactive(
						new Condition((mob, storage) -> storage.hasValue(targetAddressIn) && storage.getEntity(targetAddressIn) != null && !storage.getEntity(targetAddressIn).isRemoved()).setCustomName("entity_valid"),
						new Condition((mob, storage) -> !mob.getBoundingBox().inflate(1,0,1).intersects(storage.getEntity(targetAddressIn).getBoundingBox())).setCustomName("too_far"),
						new Actions.MoveTo(targetAddressIn, speed))).setCustomName("approach_entity"),
				Actions.equipFromEntity(targetAddressIn).setCustomName("equip_item")).setCustomName("equip_from_entity");
	}
	
	/** Moves towards the given item entity and attempts to pick it up */
	public static final TreeNode moveToPickUp(String targetAddressIn)
	{
		return Sequence.reactive(
				Actions.LookAtConstant.instant(targetAddressIn),
				Selector.sequential(
					Branches.tryPickUp(targetAddressIn),
					TreeNode.conditional((mob, storage) ->
					{
						Entity target = storage.getEntity(targetAddressIn);
						return target != null && target.isAlive() && !mob.getBoundingBox().inflate(1,0,1).intersects(target.getBoundingBox());
					}, new Actions.MoveTo(targetAddressIn, 0.5D))));
	}
	
	/** Attempts to pick up the given item entity, manipulating hand inventory if necessary */
	public static final TreeNode tryPickUp(String targetAddress)
	{
		return Selector.sequential(
				new Actions.PickUpItem(targetAddress),
				Sequence.reactive(
					new Condition((mob, storage) ->
					{
						ItemStack heldStack = MobWhiteboard.getItemInSlot(storage, EquipmentSlot.MAINHAND);
						ItemStack targetStack = ((ItemEntity)storage.getEntity(targetAddress)).getItem();
						return !(heldStack.isEmpty() || Actions.PickUpItem.canMergeStacks(heldStack, targetStack));
					}).setCustomName("need_make_space"), 
					Selector.sequential(
						TreeNode.conditional((mob,storage) -> 
						{
							ItemStack heldStack = MobWhiteboard.getItemInSlot(storage, EquipmentSlot.OFFHAND);
							ItemStack targetStack = ((ItemEntity)storage.getEntity(targetAddress)).getItem();
							return Actions.PickUpItem.canMergeStacks(heldStack, targetStack);
						}, Actions.swapItems()),
						new Actions.DropItem())).setCustomName("ensure_pickup")).setCustomName("try_pick_up");
	}
	
	public static final TreeNode lookRandom(int min, int max)
	{
		return Sequence.sequential(
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
					Actions.LookAtConstant.normal("look_target"))).setCustomName("random_look").setDiscrete();
	}
	
	public static final TreeNode rangeAttackMotion()
	{
		NodePredicate targetClose = (mob, storage) ->
		{
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.ATTACK_TARGET);
			return target != null && target.distanceTo(mob) <= Actions.AttackMelee.getAttackReachSqr(mob, target) + 1D;
		};
		
		NodePredicate targetFar = (mob, storage) ->
		{
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.ATTACK_TARGET);
			return target != null && target.distanceTo(mob) > Actions.AttackMelee.getAttackReachSqr(mob, target) + 5D;
		};
		
		return Selector.sequential(
				TreeNode.conditional(targetClose, new Actions.MoveAwayFrom(MobWhiteboard.ATTACK_TARGET, 0.5D, 8D)).setCustomName("move_farther").setDiscrete(),
				TreeNode.conditional(targetFar, new Actions.MoveTowards(MobWhiteboard.ATTACK_TARGET, 0.5D, 12D)).setCustomName("move_closer").setDiscrete(),
				Condition.alwaysTrue().setCustomName("good_position")).setCustomName("manage_distance");
	}
	
	public static final TreeNode attackRangeBow()
	{
		/** Returns true if ticks using is at least 1 second and target is too close OR ticks using is at least 3 seconds and target is far away */
		NodePredicate shouldShoot = (mob, storage) ->
		{
			int ticksUsing = storage.getInt(MobWhiteboard.MOB_TICKS_USING);
			LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.ATTACK_TARGET);
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
		
		return Sequence.reactive(
			new Condition((mob, storage) ->
			{
				return mob.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BowItem || mob.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof BowItem;
			}).setCustomName("has_bow"),
			Selector.sequential(
				swapHandsIfInvalid((item)->{ return item.is(Items.BOW); }),
				Sequence.sequential(
					Actions.startUsingItem(),
					new WaitUntil(shouldShoot),
					new LeafSingle()
					{
						public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
						{
							LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.ATTACK_TARGET);
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
					}.setCustomName("shoot_bow")))).setCustomName("bow_attack").setDiscrete();
	}
	
	public static final TreeNode attackRangeCrossbow()
	{
		return TreeNode.conditional((mob, storage) -> { return mob.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof net.minecraft.world.item.CrossbowItem; },
				Selector.sequential(
					TreeNode.conditional((mob, storage) ->
					{
						return !CrossbowItem.isCharged(mob.getItemInHand(InteractionHand.MAIN_HAND));
					}, Sequence.sequential(
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
						return CrossbowItem.isCharged(mob.getItemInHand(InteractionHand.MAIN_HAND)) && storage.hasValue(MobWhiteboard.ATTACK_TARGET) && mob.getSensing().hasLineOfSight(storage.getEntity(MobWhiteboard.ATTACK_TARGET));
					}, new LeafSingle()
						{
							public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
							{
								LivingEntity target = (LivingEntity)storage.getEntity(MobWhiteboard.ATTACK_TARGET);
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
						}.setCustomName("shoot_crossbow")))).setCustomName("crossbow_attack").setDiscrete();
	}
	
	@SuppressWarnings("deprecation")
	public static TreeNode attackSplashPotion()
	{
		ResourceLocation timerName = Registry.ITEM.getKey(Items.SPLASH_POTION);
		return Sequence.reactive(
				new Condition((mob, storage) ->
				{
					if(mob.getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.SPLASH_POTION)
					{
						boolean noPositives = true;
						for(MobEffectInstance effect : PotionUtils.getMobEffects(mob.getItemInHand(InteractionHand.MAIN_HAND)))
							if(effect.getEffect().isBeneficial())
							{
								noPositives = false;
								break;
							};
						if(noPositives)
							return true;
					}
					if(mob.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.SPLASH_POTION)
					{
						boolean noPositives = true;
						for(MobEffectInstance effect : PotionUtils.getMobEffects(mob.getItemInHand(InteractionHand.OFF_HAND)))
							if(effect.getEffect().isBeneficial())
							{
								noPositives = false;
								break;
							};
						if(noPositives)
							return true;
					}
					
					return false;
				}).setCustomName("has_splash_potion"),
				new Condition(NodePredicates.CAN_SEE_TARGET).setCustomName("can_see_target"),
				new Condition(NodePredicates.isTimerZero(timerName)),
				Selector.sequential(
					swapHandsIfInvalid((item)->{ return item.getItem() == Items.SPLASH_POTION; }),
					Sequence.sequential(
						new LeafSingle()
						{
							public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
							{
								ItemStack stack = storage.getItemStack(MobWhiteboard.getSlotAddress(EquipmentSlot.MAINHAND));
								ThrownPotion potion = new ThrownPotion(mob.level, mob);
								potion.setItem(stack);
								potion.setXRot(potion.getXRot() - -20.0F);
								
								Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
								Vec3 vec3 = target.getDeltaMovement();
								double d0 = target.getX() + vec3.x - mob.getX();
								double d1 = target.getEyeY() - (double)1.1F - mob.getY();
								double d2 = target.getZ() + vec3.z - mob.getZ();
								double d3 = Math.sqrt(d0 * d0 + d2 * d2);
								potion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
								
								if(!mob.isSilent())
									mob.level.playSound((Player)null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WITCH_THROW, mob.getSoundSource(), 1.0F, 0.8F + mob.getRandom().nextFloat() * 0.4F);
								mob.level.addFreshEntity(potion);
								return true;
							}
						}.setCustomName("throw_potion"),
						Actions.setWhiteboardTimer(timerName, Reference.Values.TICKS_PER_SECOND * 3)))
				);
	}
	
	private static String LAST_SIGHTING = "target_last_sighting";
	private static String SEARCH_RADIUS = "target_search_radius";
	
	/** Stores the target entity's block position in the whiteboard as long as it is visible */
	public static TreeNode markTargetSighting()
	{
		return new LeafSingle()
			{
				public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
				{
					Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
					if(!storage.hasValue(MobWhiteboard.ATTACK_TARGET) || target == null)
					{
						storage.clearValue(LAST_SIGHTING);
						return false;
					}
					
					if(mob.getSensing().hasLineOfSight(target))
						storage.setValue(LAST_SIGHTING, target.blockPosition());
					return true;
				}
			}.setCustomName("mark_last_sighting");
	}
	
	/** Moves to the last known position of the target entity, then gradually spreads out from it */
	public static TreeNode searchAroundPosition(int repeats) { return searchAroundPosition(LAST_SIGHTING, repeats); }
	
	public static TreeNode searchAroundPosition(String addressIn, int repeats)
	{
		return Sequence.sequential(
				new Actions.MoveTo(addressIn, 0.5D),
				new Actions.Wait(Reference.Values.TICKS_PER_SECOND, Reference.Values.TICKS_PER_SECOND*3),
				Actions.setWhiteboardValue(SEARCH_RADIUS, 1),
				Decorator.repeat(repeats, 
					Sequence.sequential(
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									int radius = storage.getInt(SEARCH_RADIUS);
									storage.setValue(SEARCH_RADIUS, radius + 1);
									
									BlockPos mobPos = storage.getBlockPos(addressIn);
									RandomSource random = mob.getRandom();
									
									BlockPos target = addRandom(mobPos, radius, random);
									PathNavigation navigation = mob.getNavigation();
									int tries = 50;
									while(navigation.createPath(target, radius) == null && --tries > 0)
										target = addRandom(mobPos, radius, random);
									
									if(navigation.createPath(target, radius) != null)
									{
										storage.setValue("search_target", target);
										return true;
									}
									return false;
								}
								
								private BlockPos addRandom(BlockPos origin, int radius, RandomSource random)
								{
									double offX = random.nextDouble() - 0.5D;
									double offY = random.nextDouble() - 0.5D;
									double offZ = random.nextDouble() - 0.5D;
									Vec3 offset = new Vec3(offX, offY, offZ).normalize().multiply(radius, radius / 2, radius);
									return origin.offset(offset.x, offset.y, offset.z);
								}
							},
							new Actions.MoveTo("search_target", 0.5D),
							new Actions.Wait(Reference.Values.TICKS_PER_SECOND, Reference.Values.TICKS_PER_SECOND*3)
						)).setCustomName("search_loop"),
				new LeafSingle()
				{
					public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
					{
						storage.clearValue(SEARCH_RADIUS);
						mob.setTarget(null);
						return true;
					}
				}.setCustomName("finish_search")).setCustomName("search_area").setDiscrete();
	}
	
	@SuppressWarnings("deprecation")
	public static TreeNode throwEnderPearl()
	{
		ResourceLocation timerName = Registry.ITEM.getKey(Items.ENDER_PEARL);
		return Sequence.reactive(
				new Condition((mob, storage) -> { return !storage.hasValue("thrown_ender_pearl") || storage.getEntity("thrown_ender_pearl").isRemoved(); }),
				new Condition(NodePredicates.CAN_SEE_TARGET),
				new Condition(NodePredicates.isTimerZero(timerName)),
				Selector.sequential(
					swapHandsIfInvalid((item)->{ return item.is(Items.ENDER_PEARL); }),
					Sequence.sequential(
						new LeafSingle()
						{
							public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
							{
								ThrownEnderpearl pearl = new ThrownEnderpearl(mob.level, mob);
								pearl.setItem(storage.getItemStack(MobWhiteboard.getSlotAddress(EquipmentSlot.MAINHAND)));
								
								Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
								Vec3 vec3 = target.getDeltaMovement();
								double d0 = target.getX() + vec3.x - mob.getX();
								double d1 = target.getEyeY() - (double)1.1F - mob.getY();
								double d2 = target.getZ() + vec3.z - mob.getZ();
								double d3 = Math.sqrt(d0 * d0 + d2 * d2);
								pearl.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
								storage.setValue("thrown_ender_pearl", pearl);
								mob.level.playSound((Player)null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
								mob.level.addFreshEntity(pearl);
								return true;
							}
						}.setCustomName("throw_pearl"),
						Actions.setWhiteboardTimer(timerName, Reference.Values.TICKS_PER_SECOND))
					));
	}
	
	@SuppressWarnings("deprecation")
	public static TreeNode throwTrident()
	{
		ResourceLocation timerName = Registry.ITEM.getKey(Items.TRIDENT);
		return Sequence.reactive(
				new Condition(NodePredicates.CAN_SEE_TARGET),
				Selector.sequential(
					swapHandsIfInvalid((item)->{ return item.is(Items.TRIDENT); }),
					Selector.sequential(
						Sequence.sequential(
							new Condition((mob, storage) -> { return mob.distanceTo(storage.getEntity(MobWhiteboard.ATTACK_TARGET)) > mob.getBbWidth() + 4D; }),
							new Condition(NodePredicates.isTimerZero(timerName)),
							Actions.startUsingItem(),
							new Actions.WaitUntil((mob, storage) -> { return storage.getInt(MobWhiteboard.MOB_TICKS_USING) >= Reference.Values.TICKS_PER_SECOND; }),
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									ThrownTrident trident = new ThrownTrident(mob.level, mob, storage.getItemStack(MobWhiteboard.getSlotAddress(EquipmentSlot.MAINHAND)));
									
									Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
									double d0 = target.getX() - mob.getX();
								    double d1 = target.getY(0.3333333333333333D) - trident.getY();
								    double d2 = target.getZ() - mob.getZ();
								    double d3 = Math.sqrt(d0 * d0 + d2 * d2);
								    trident.shoot(d0, d1 + d3 * (double)0.2F, d2, 1.6F, (float)(14 - mob.level.getDifficulty().getId() * 4));
								    
									mob.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
									mob.level.addFreshEntity(trident);
									return true;
								}
							}.setCustomName("throw_trident"),
							Actions.stopUsingItem(),
							Actions.setWhiteboardTimer(timerName, Reference.Values.TICKS_PER_SECOND * 2)).setCustomName("throw_trident").setDiscrete(),
						Branches.attackMelee())));
	}
	
	public static TreeNode swapHandsIfInvalid(Predicate<ItemStack> predicate)
	{
		return Sequence.reactive(
				Decorator.inverter(new Condition(NodePredicates.isItemValid(predicate, MobWhiteboard.getSlotAddress(EquipmentSlot.MAINHAND)))),
				new Condition(NodePredicates.isItemValid(predicate, MobWhiteboard.getSlotAddress(EquipmentSlot.OFFHAND))),
				Actions.swapItems()).setCustomName("swap_held_items").setDiscrete();
	}
	
	public static TreeNode moveToAndMineBlock(String addressIn)
	{
		return Sequence.reactive(
				new Condition(NodePredicates.hasValue(addressIn)).setCustomName("value_exists"),
				new Condition(NodePredicates.isBlockMinable(addressIn)).setCustomName("block_is_minable"),
				new LeafRunning()
				{
					protected Status run(PathfinderMob mob, Whiteboard<?> storage)
					{
						BlockPos minePos = storage.getBlockPos(addressIn);
						mob.getLookControl().setLookAt(minePos.getX(), minePos.getY(), minePos.getZ(), 10, 10);
						return Status.RUNNING;
					}
				}.setCustomName("look_at_pos"),
				Selector.sequential(
					TreeNode.conditional((mob, storage) ->
					{
						BlockPos pos = (BlockPos)storage.getBlockPos(addressIn);
						
						// Check blocks immediately adjacent to ourselves
						if(mob.getBoundingBox().inflate(1D).contains(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D))
							return true;
						
						// Check distance to our head
						Vec3 minePos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
						Vec3 eyePos = mob.getEyePosition();
						return minePos.distanceTo(eyePos) < 2D;
					}, mineBlock(addressIn)).setCustomName("do_mining"),
					new Actions.MoveTo(addressIn, 0.5D).setCustomName("move_to_block"))
				).setCustomName("move_and_mine");
	}
	
	public static TreeNode mineBlock(String addressIn)
	{
		String PROGRESS = "mining_progress";
		return Sequence.reactive(
				new Condition(NodePredicates.hasValue(addressIn)).setCustomName("value_exists"),
				new Condition(NodePredicates.isBlockMinable(addressIn)).setCustomName("block_is_minable"),
				Sequence.sequential(
					Actions.setWhiteboardValue(PROGRESS, 0F).setCustomName("start_mining"),
					Decorator.doWhile(
						(mob, storage) -> { return storage.getFloat(PROGRESS) < 1F; },
						Sequence.sequential(
							Actions.swingArm(InteractionHand.MAIN_HAND),
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									float progress = storage.getFloat(PROGRESS);
									float inc = 0.8F;	// TODO Change to value based on blockstate and held item etc.
									progress += inc;
									
									storage.setValue(PROGRESS, progress);
									mob.getLevel().destroyBlockProgress(mob.getId(), storage.getBlockPos(addressIn), 10 - (int)Mth.clamp(progress, 0F, 10F));
									return true;
								}
							}.setCustomName("inc_progress"))).setCustomName("mining_loop").setDiscrete(),
					TreeNode.conditional((mob,storage) -> { return storage.getFloat(PROGRESS) >= 1F; }, Actions.breakBlock(addressIn)).setCustomName("break_if_complete").setDiscrete())).setCustomName("mine_block");
	}
	
	/**
	 * Returns a sequential node with node1 nested inside of a reactive sequence with the condition set to force success, then node2<br>
	 * Useful for performing the actions of node2 regardless the success or validity of node1
	 * @param condition
	 * @param node1
	 * @param node2
	 * @return
	 */
	public static TreeNode doUntilThen(NodePredicate condition, TreeNode node1, TreeNode node2)
	{
		return doOnceThen(
				Sequence.reactive(
					new Condition(condition),
					node1),
				node2).setCustomName("do_until_then");
	}
	
	public static TreeNode doOnceThen(TreeNode node1, TreeNode node2)
	{
		return Sequence.sequential(
				Decorator.forceSuccess(node1),
				node2).setCustomName("do_once_then");
	}
	
	@SuppressWarnings("deprecation")
	public static TreeNode executeGroupCommand()
	{
		return Sequence.reactive(
				new Condition((mob, storage) -> storage.hasCommands()).setCustomName("has_commands"),
				Selector.sequential(
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.GUARD_POS),
							new Condition((mob, storage) -> ((BlockPos)storage.currentCommand().variable("Pos")).distSqr(mob.blockPosition()) > 1D),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										storage.clearValue("guard_move");
										BlockPos guardPos = (BlockPos)storage.currentCommand().variable("Pos");
										
										BlockPos currentPos = mob.blockPosition();
										currentPos = new BlockPos(currentPos.getX(), guardPos.getY(), guardPos.getZ());
										if(currentPos == guardPos)
											return false;
										
										int range = 3;
										if(mob.getNavigation().createPath(guardPos, range * range) != null)
										{
											/*
											 * If our current position can path directly to the guard position,
											 * within range^2 nodes, try to move directly to it
											 */
											storage.setValue("guard_move", guardPos);
										}
										else
										{
											float currentUtility = getUtility(mob.blockPosition(), guardPos, mob.getNavigation());
											double lowest = currentUtility;
											BlockPos target = mob.blockPosition();
											for(int x=-range; x<range; x++)
												for(int z=-range; z<range; z++)
												{
													BlockPos offset = currentPos.offset(x, 0, z);
													float utility = getUtility(offset, guardPos, mob.getNavigation());
													if(utility < lowest)
													{
														lowest = utility;
														target = offset;
													}
												}
											
											if(target != currentPos)
												storage.setValue("guard_move", target);
										}
										
										return storage.hasValue("guard_move");
									}
									
									public final float getUtility(BlockPos pos, BlockPos guard, PathNavigation navi)
									{
										if(pos == guard)
											return Float.MIN_VALUE;
										
										Path path = navi.createPath(Stream.of(pos, guard), 64);
										return path == null ? Float.MAX_VALUE : path.getNodeCount();
									}
								}.setCustomName("identify_position"),
								new Actions.MoveTo("guard_move", 0.7D))).setCustomName("guard_pos").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.MINE),
						doUntilThen(
							(mob, storage) -> !mob.getLevel().isEmptyBlock((BlockPos)storage.currentCommand().variable("Pos")),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										storage.setValue("mine_position", (BlockPos)storage.currentCommand().variable("Pos"));
										return true;
									}
								}.setCustomName("set_mine_position"),
								moveToAndMineBlock("mine_position")),
							Actions.completeCurrentTask())).setCustomName("mine_block").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.QUARRY),
						Sequence.sequential(
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									storage.clearValue("mine_position");
									MobCommand current = storage.currentCommand();
									BlockPos minPos;
									BlockPos maxPos;
									if(current.variables() > 2)
									{
										minPos = (BlockPos)current.variable("Pos");
										maxPos = (BlockPos)current.variable("Pos2");
									}
									else
									{
										BlockPos core = (BlockPos)current.variable("Pos");
										minPos = core.offset(-5, 0, -5);
										maxPos = core.offset(5, 3, 5);
									}
									
									Direction orientation = (Direction)current.variable("Facing");
									List<BlockPos> consignment = GroupAction.ActionQuarry.makeConsignment(minPos, maxPos, orientation, mob.getLevel(), Lists.newArrayList(), 1);
									if(!consignment.isEmpty())
										storage.setValue("mine_position", consignment.get(0));
									return true;
								}
							}.setCustomName("set_mine_position"),
							Decorator.forceSuccess(TreeNode.conditional((mob,storage) -> !storage.hasValue("mine_position"), Actions.completeCurrentTask())).setCustomName("complete_if_no_blocks").setDiscrete(),
							moveToAndMineBlock("mine_position").setCustomName("mine_current_block").setDiscrete()).setCustomName("quarry_loop")
						).setCustomName("quarry_region").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.BONEMEAL),
						new Condition((mob, storage) -> storage.getTimer(Registry.ITEM.getKey(Items.BONE_MEAL)) == 0).setCustomName("ready_to_bonemeal"),
						doOnceThen(
							Sequence.reactive(
								new Condition((mob, storage) ->
								{
									BlockPos pos = (BlockPos)storage.currentCommand().variable("Pos");
									BlockState state = mob.getLevel().getBlockState(pos);
									return state.getBlock() instanceof BonemealableBlock && ((BonemealableBlock)state.getBlock()).isValidBonemealTarget(mob.getLevel(), pos, state, mob.getLevel().isClientSide);
								}).setCustomName("target_is_bonemealable"),
								Sequence.sequential(
									new Condition((mob, storage) -> mob.getMainHandItem().is(Items.BONE_MEAL) || mob.getOffhandItem().is(Items.BONE_MEAL)).setCustomName("has_bonemeal"),
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											storage.setValue("bonemeal_target", (BlockPos)storage.currentCommand().variable("Pos"));
											return true;
										}
									}.setCustomName("set_target"),
									Decorator.forceSuccess(new Actions.MoveTo("bonemeal_target", 0.5D)),
									new Condition((mob, storage) -> 
									{
										BlockPos target = storage.getBlockPos("bonemeal_target");
										return mob.getBoundingBox().inflate(1D).contains(new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D));
									}).setCustomName("in_range"),
									Decorator.forceSuccess(swapHandsIfInvalid((stack) -> stack.is(Items.BONE_MEAL))),
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											// FIXME Action fires multiple times instead of just once
											BlockPos target = storage.getBlockPos("bonemeal_target");
											Level level = mob.getLevel();
											BlockState state = level.getBlockState(target);
											((BonemealableBlock)state.getBlock()).performBonemeal((ServerLevel)level, mob.getRandom(), target, state);
											mob.getMainHandItem().shrink(1);
											storage.setTimer(Registry.ITEM.getKey(Items.BONE_MEAL), 5);
											return true;
										}
									}.setCustomName("apply_bonemeal"))),
							Actions.completeCurrentTask())).setCustomName("bonemeal").setDiscrete(),
					Sequence.reactive(	// FIXME Ensure handling for no specified block
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.PLACE_BLOCK),
						doOnceThen(
							Sequence.reactive(
								new Condition((mob, storage) ->
								{
									MobCommand command = storage.currentCommand();
									BlockPos target = (BlockPos)command.variable("Pos");
									return command.variables() > 2 ? (Block)command.variable("Block") != mob.getLevel().getBlockState(target).getBlock() : mob.getLevel().isEmptyBlock(target);
								}).setCustomName("block_not_at_pos"),
								new Condition((mob, storage) -> 
								{
									MobCommand command = storage.currentCommand();
									if(command.variables() > 2)
									{
										Block specified = (Block)storage.currentCommand().variable("Block");
										return
												mob.getMainHandItem().getItem() instanceof BlockItem && ((BlockItem)mob.getMainHandItem().getItem()).getBlock() == specified ||
												mob.getOffhandItem().getItem() instanceof BlockItem && ((BlockItem)mob.getOffhandItem().getItem()).getBlock() == specified;
									}
									else
										return mob.getMainHandItem().getItem() instanceof BlockItem || mob.getOffhandItem().getItem() instanceof BlockItem;
								}).setCustomName("has_block_item"),
								Sequence.sequential(
									Decorator.forceSuccess(
										Sequence.reactive(
										new Condition((mob, storage) -> 
										{
											BlockPos pos = (BlockPos)storage.currentCommand().variable("Pos");
											return !mob.getBoundingBox().inflate(1.5D).contains(pos.getX(), pos.getY(), pos.getZ());
										}),
										Sequence.sequential(
											new LeafSingle()
											{
												public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
												{
													storage.setValue("place_target_pos", (BlockPos)storage.currentCommand().variable("Pos"));
													return true;
												}
											}.setCustomName("set_destination"),
											new Actions.MoveTo("place_target_pos", 0.5D)))).setCustomName("move_to_pos").setDiscrete(),
									Decorator.forceSuccess(
										Sequence.reactive(
											new Condition((mob, storage) -> 
											{
												MobCommand command = storage.currentCommand();
												if(command.variables() > 2)
												{
													Block specified = (Block)storage.currentCommand().variable("Block");
													return !(mob.getMainHandItem().getItem() instanceof BlockItem && ((BlockItem)mob.getMainHandItem().getItem()).getBlock() == specified);
												}
												else
													return !(mob.getMainHandItem().getItem() instanceof BlockItem);
											}).setCustomName("not_in_mainhand"),
											Actions.swapItems())).setCustomName("put_block_in_mainhand").setDiscrete(),
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											Level world = mob.getLevel();
											ItemStack heldStack = mob.getMainHandItem();
											MobCommand command = storage.currentCommand();
											BlockPos target = (BlockPos)command.variable("Pos");
											Direction direction = (Direction)command.variable("Facing");
											BlockPos targetBelow = target.relative(direction);
											
											Block blockToPlace = ((BlockItem)heldStack.getItem()).getBlock();
											BlockState targetState = blockToPlace.defaultBlockState();
											if(targetState != null)
											{
												targetState = Block.updateFromNeighbourShapes(targetState, world, target);
												if(!ForgeEventFactory.onBlockPlace(mob, BlockSnapshot.create(world.dimension(), world, targetBelow), direction.getOpposite()))
												{
													world.setBlock(target, targetState, 3);
													world.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(mob, targetState));
													heldStack.shrink(1);
													return true;
												}
											}
											return false;
										}
									}.setCustomName("place_block"))
								),
							Actions.completeCurrentTask())).setCustomName("place_block").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.ATTACK),
						Sequence.sequential(
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									Entity target = (Entity) storage.currentCommand().variable("Entity");
									storage.setValue(MobWhiteboard.AI_TARGET, target);
									return true;
								}
							}.setCustomName("set_attack_target"),
							Actions.completeCurrentTask())).setCustomName("attack_target").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.CEASEFIRE),
						Sequence.sequential(
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									storage.clearValue(MobWhiteboard.AI_TARGET);
									mob.setTarget(null);
									return true;
								}
							}.setCustomName("clear_attack_target"),
						Actions.completeCurrentTask())).setCustomName("ceasefire").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.CEASEFIRE_MOB),
							doUntilThen(
								(mob, storage) -> storage.getEntity(MobWhiteboard.AI_TARGET) == storage.currentCommand().variable("Entity"),
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										storage.clearValue(MobWhiteboard.AI_TARGET);
										mob.setTarget(null);
										return true;
									}
								}.setCustomName("clear_attack_target"),
								Actions.completeCurrentTask())).setCustomName("ceasefire_mob").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.MOUNT),
							doUntilThen(
								(mob, storage) -> 
								{
									if(mob.isPassenger())
										return false;
									
									MobCommand command = storage.currentCommand();
									if(command.hasVariable("Entity"))
									{
										Entity target = (Entity)command.variable("Entity");
										return target != mob && target.isAddedToWorld() && target.isAlive() && !target.isVehicle();
									}
									return false;
								},
								doUntilThen(
									(mob, storage) ->
									{
										Entity target = (Entity)storage.currentCommand().variable("Entity");
										return !target.getBoundingBox().inflate(1D).intersects(mob.getBoundingBox());
									},
									Sequence.sequential(
											new LeafSingle()
											{
												public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
												{
													storage.setValue("mount_target", (Entity)storage.currentCommand().variable("Entity"));
													return true;
												}
											}.setCustomName("set_target"),
											new Actions.MoveTo("mount_target", 0.5D)),
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											return mob.startRiding((LivingEntity)storage.currentCommand().variable("Entity"));
										}
									}),
								Actions.completeCurrentTask())).setCustomName("mount").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.DISMOUNT),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										mob.stopRiding();
										return true;
									}
								}.setCustomName("dismount_vehicle"),
								Actions.completeCurrentTask())).setCustomName("dismount").setDiscrete(),
					Sequence.reactive(
						new Condition((mob, storage) -> storage.currentCommand().type() == Mark.GOTO_POS),
						Sequence.sequential(
							new LeafSingle()
							{
								public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
								{
									storage.setValue("command_dest", (BlockPos)storage.currentCommand().variable("Pos"));
									return true;
								}
							}.setCustomName("set_dest"),
							new Actions.MoveTo("command_dest", 0.5D),
							Actions.completeCurrentTask())).setCustomName("goto_pos").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.GOTO_MOB),
							new Condition((mob, storage) -> storage.currentCommand().variable("Entity") != null),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										storage.setValue("command_dest", ((Entity)storage.currentCommand().variable("Entity")));
										return true;
									}
								}.setCustomName("set_dest"),
								new Actions.MoveTo("command_dest", 0.5D),
								Actions.completeCurrentTask())).setCustomName("goto_mob").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.STOP_MOVING),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										mob.getNavigation().stop();
										return true;
									}
								}.setCustomName("stop_moving"),
								Actions.completeCurrentTask())).setCustomName("stop_moving").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.PICK_UP),
							doUntilThen(
								(mob, storage) ->
								{
									Entity target = (Entity)storage.currentCommand().variable("Entity");
									return target != null && target.isAlive();
								},
								Sequence.sequential(
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											storage.setValue("pickup_target", storage.currentCommand().variable("Entity"));
											return true;
										}
									}.setCustomName("set_pickup_target"),
									Branches.moveToPickUp("pickup_target")),
								Actions.completeCurrentTask())).setCustomName("pick_up").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.EQUIP),
							doUntilThen(
								(mob, storage) ->
								{
									Entity target = (Entity)storage.currentCommand().variable("Entity");
									return target != null && target.isAlive();
								},
								Sequence.sequential(
									new LeafSingle()
									{
										public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
										{
											storage.setValue("equip_target", storage.currentCommand().variable("Entity"));
											return true;
										}
									}.setCustomName("set_equip_target"),
									Branches.equipFromEntity("equip_target", 0.5D)),
								Actions.completeCurrentTask())).setCustomName("equip").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.JOIN_GROUP),
							doUntilThen(
								(mob, storage) -> GroupSaveData.get(mob.getServer()).hasGroup((LivingEntity)storage.currentCommand().variable("Entity")),
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										MobCommand current = storage.currentCommand();
										LivingEntity target = (LivingEntity)current.variable("Entity");
										IMobGroup targetGroup = GroupSaveData.get(mob.getServer()).getGroup(target);
										IMobGroup myGroup = GroupSaveData.get(mob.getServer()).getGroup(mob);
										if(targetGroup == myGroup)
											return true;
										
										if(myGroup != null)
											myGroup.remove(mob);
										
										targetGroup.add(mob);
										return true;
									}
								}.setCustomName("join_target_group"),
								Actions.completeCurrentTask())).setCustomName("join_group").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.START_GROUP),
							new Condition((mob, storage) -> GroupSaveData.get(mob.getServer()).hasGroup(mob)).setCustomName("has_group"),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										IMobGroup group = GroupSaveData.get(mob.getServer()).getGroup(mob);
										group.split(mob);
										return true;
									}
								}.setCustomName("split_group"),
								Actions.completeCurrentTask())).setCustomName("start_group").setDiscrete(),
					Sequence.reactive(
							new Condition((mob, storage) -> storage.currentCommand().type() == Mark.WAIT),
							Sequence.sequential(
								new LeafSingle()
								{
									public boolean doAction(PathfinderMob mob, Whiteboard<?> storage)
									{
										MobCommand command = storage.currentCommand();
										int duration = command.variables() > 0 && command.hasVariable("Duration") ? Math.abs((int)command.variable("Duration")) : Reference.Values.TICKS_PER_SECOND;
										storage.setTimer(new ResourceLocation(Reference.ModInfo.MOD_ID, "wait_order"), duration);
										return true;
									}
								},
								doUntilThen(
									(mob, storage) -> storage.getTimer(new ResourceLocation(Reference.ModInfo.MOD_ID, "wait_order")) > 0,
									Actions.doNothing(),
									Actions.completeCurrentTask()))).setCustomName("wait").setDiscrete()
				).setCustomName("group_command"));
	}
}
