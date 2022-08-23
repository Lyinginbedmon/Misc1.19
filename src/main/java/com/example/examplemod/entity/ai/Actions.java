package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.ai.TreeNode.LeafRunning;
import com.example.examplemod.entity.ai.TreeNode.LeafSingle;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Pre-written Leaf node classes describing common isolated actions taken by mobs.
 * @author Lying
 */
public class Actions
{
	/** Makes the mob do a little jump. */
	public static TreeNode jump()
	{
		return new LeafRunning()
		{
			public boolean start(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				mobIn.getJumpControl().jump();
				return true;
			}
			
			public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				if(mobIn.isOnGround())
					return Status.SUCCESS;
				return Status.RUNNING;
			}
		};
	}
	
	/** Attempts to equip the item held in the mob's main hand */
	public static TreeNode equipHeldItem()
	{
		return new LeafSingle()
		{
			public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				EquipmentSlot slot = Mob.getEquipmentSlotForItem(mobIn.getMainHandItem());
				if(mobIn.equipItemIfPossible(mobIn.getMainHandItem()))
				{
					mobIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					mobIn.setDropChance(slot, 1F);
					return true;
				}
				return false;
			}
		};
	}
	
	public static TreeNode equipFromEntity(String targetAddress)
	{
		return new LeafSingle()
		{
			public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				if(!storage.hasValue(targetAddress))
					return false;
				ItemEntity entity = (ItemEntity)storage.getEntity(targetAddress);
				if(entity == null || entity.isRemoved() || !entity.getBoundingBox().intersects(mobIn.getBoundingBox().inflate(1,0,1)))
					return false;
				
				ItemStack stack = entity.getItem();
				if(mobIn.equipItemIfPossible(stack))
				{
					mobIn.onItemPickup(entity);
					mobIn.take(entity, stack.getCount());
					entity.discard();
					return true;
				}
				
				return false;
			}
		}.setCustomName("equip_entity_direct");
	}
	
	/** Swaps the items held in the mob's hands */
	public static TreeNode swapItems()
	{
		return new LeafSingle()
		{
			public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				ItemStack stackA = mobIn.getMainHandItem().copy();
				ItemStack stackB = mobIn.getOffhandItem().copy();
				mobIn.setItemInHand(InteractionHand.MAIN_HAND, stackB);
				mobIn.setItemInHand(InteractionHand.OFF_HAND, stackA);
				return true;
			}
		}.setCustomName("swap_items");
	}
	
	/**
	 * Makes the mob emit a given sound.<br>
	 * If multiple sounds are given, selects randomly from among them.
	 */
	public static class MakeSound extends LeafSingle
	{
		private final SoundEvent[] sounds;
		
		public MakeSound(SoundEvent... soundsIn)
		{
			sounds = soundsIn;
		}
		
		public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			mobIn.playSound(sounds.length == 1 ? sounds[0] : sounds[mobIn.getRandom().nextInt(sounds.length)]);
			return true;
		}
	}
	
	public static final TreeNode setWhiteboardValue(String address, Object value)
	{
		return new LeafSingle()
			{
				public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
				{
					storage.setValue(address, value);
					return true;
				}
			};
	}
	
	public static final TreeNode setWhiteboardTimer(ResourceLocation address, int value)
	{
		return new LeafSingle()
			{
				public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
				{
					storage.setTimer(address, value);
					return true;
				}
			};
	}
	
	/**
	 * Looks at a given point.<br>
	 * Warning: This leaf never terminates, so should be used in a Parallel node with another action to terminate it.
	 */
	public static class LookAtConstant extends LeafRunning
	{
		private final String address;
		private final float xSpeed, ySpeed;
		
		public static LookAtConstant instant(String target) { return new LookAtConstant(target, 30F, 30F); }
		public static LookAtConstant normal(String target) { return new LookAtConstant(target, -1F, -1F); }
		public static LookAtConstant lazy(String target) { return new LookAtConstant(target, 1F, 1F); }
		
		protected LookAtConstant(String addressIn, float xSpeedIn, float ySpeedIn)
		{
			address = addressIn;
			xSpeed = xSpeedIn;
			ySpeed = ySpeedIn;
			setCustomName("look_at_constant");
		}
		
		public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(!storage.hasValue(address))
				return Status.FAILURE;
			
			Vec3 target = getLookAtVec(storage);
			if(target != null)
			{
				if(xSpeed < 0F && ySpeed < 0F)
					mobIn.getLookControl().setLookAt(target);
				else
					mobIn.getLookControl().setLookAt(target.x, target.y, target.z, xSpeed, ySpeed);
				return Status.RUNNING;
			}
			return Status.FAILURE;
		}
		
		private Vec3 getLookAtVec(Whiteboard<?> storage)
		{
			// Vector variable
			Vec3 target = null;
			try
			{
				target = storage.getVec3(address);
			}
			catch(Exception e) { }
			if(target != null)
				return target;
			
			// Block position variable
			BlockPos pos = null;
			try
			{
				pos = storage.getBlockPos(address);
			}
			catch(Exception e) { }
			if(pos != null)
				return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			
			// Entity variable
			Entity ent = null;
			try
			{
				ent = storage.getEntity(address);
			}
			catch(Exception e) { }
			if(ent != null)
			{
				double x = ent.getX();
				double y = ent instanceof LivingEntity ? ent.getEyeY() : (ent.getBoundingBox().minY + ent.getBoundingBox().maxY) * 0.5D;
				double z = ent.getZ();
				return new Vec3(x, y, z);
			}
			return null;
		}
	}
	
	/** Waits the given number of game ticks. */
	public static class Wait extends LeafRunning
	{
		private final int min, max;
		private int ticks = 0;
		
		public Wait(int durationIn)
		{
			this(durationIn, durationIn);
			setCustomName("wait_"+durationIn);
		}
		
		public Wait(int minTicks, int maxTicks)
		{
			min = Math.min(minTicks, maxTicks);
			max = Math.max(minTicks, maxTicks);
			setCustomName("wait_"+minTicks+"_to_"+maxTicks);
		}
		
		public boolean start(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(min != max)
				ticks = min + mobIn.getRandom().nextInt(max - min);
			else
				ticks = min;
			return true;
		}
		
		public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			return --ticks <= 0 ? Status.SUCCESS : Status.RUNNING;
		}
		
		public void stop(PathfinderMob mobIn, Whiteboard<?> storage) { ticks = -1; }
	}
	
	/** Waits in 1 second intervals until a condition is met */
	public static class WaitUntil extends LeafRunning
	{
		private final NodePredicate predicate;
		
		public WaitUntil(NodePredicate predicateIn)
		{
			predicate = predicateIn;
			setCustomName("wait_until_condition");
		}
		
		public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			return predicate.test(mobIn, storage) ? Status.SUCCESS : Status.RUNNING;
		}
	}
	
	public static class Counter extends LeafRunning
	{
		private final String address;
		private final int initialValue;
		private final int finalValue;
		
		public Counter(String addressIn) { this(addressIn, 0); }
		public Counter(String addressIn, int startValue) { this(addressIn, startValue, (int)(Integer.MAX_VALUE * Math.signum(startValue))); }
		public Counter(String addressIn, int startValue, int maxValue)
		{
			this.address = addressIn;
			this.initialValue = startValue;
			this.finalValue = maxValue;
		}
		
		protected boolean start(PathfinderMob mob, Whiteboard<?> storage)
		{
			storage.setValue(address, initialValue);
			return true;
		}
		
		protected Status run(PathfinderMob mob, Whiteboard<?> storage)
		{
			int value = storage.hasValue(address) ? storage.getInt(address) : initialValue;
			if(value != finalValue)
				value += Math.signum(finalValue - value);
			storage.setValue(address, value);
			return Status.RUNNING;
		}
		
		protected void stop(PathfinderMob mob, Whiteboard<?> storage)
		{
			storage.clearValue(address);
		}
	}
	
	/**
	 * Sets the mob's navigator to move to the given position, then terminates when the navigator is empty.<br>
	 * Note that the navigator may be empty simply because no valid path exists.
	 */
	public static class MoveTo extends LeafRunning
	{
		private final String address;
		private final double speed;
		// FIXME Not precise enough for item collection, increase precision
		public MoveTo(String addressIn, double speedIn)
		{
			address = addressIn;
			speed = speedIn;
			setCustomName("move_to");
		}
		
		public boolean start(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(!storage.hasValue(address))
				return false;
			
			Vec3 dest = Whiteboard.getDest(storage, address);
			return mobIn.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
		}
		
		public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			return mobIn.getNavigation().isDone() ? Status.SUCCESS : Status.RUNNING;
		}
		
		public void stop(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			mobIn.getNavigation().stop();
		}
	}
	
	public static class MoveTowards extends MoveAwayFrom
	{
		public MoveTowards(String addressIn, double speedIn, double minDistIn)
		{
			super(addressIn, speedIn, minDistIn);
			setCustomName("move_closer_to");
		}
		
		protected Vec3 getDest(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			return Whiteboard.getDest(storage, address);
		}
		
		protected boolean validTargetPoint(PathfinderMob mobIn, Vec3 dest, Vec3 point)
		{
			Vec3 position = mobIn.position();
			Vec3 direction = point.subtract(mobIn.position()).normalize();
			return position.distanceTo(dest) < position.add(direction).distanceTo(dest);
		}
		
		protected boolean isWithinRangeOf(PathfinderMob mobIn, Vec3 dest)
		{
			return dest == null || mobIn.distanceToSqr(dest) <= (minDist * minDist);
		}
	}
	
	public static class MoveAwayFrom extends LeafRunning
	{
		private final double speed;
		protected final String address;
		protected final double minDist;
		
		public MoveAwayFrom(String addressIn, double speedIn, double minDistIn)
		{
			address = addressIn;
			speed = speedIn;
			minDist = minDistIn;
			setCustomName("move_away_from");
		}
		
		public boolean start(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(!storage.hasValue(address))
				return false;
			
			Vec3 point = getDest(mobIn, storage);
			if(point != null)
				return mobIn.getNavigation().moveTo(point.x, point.y, point.z, speed);
			return false;
		}
		
		public Status run(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(mobIn.getNavigation().isDone())
				return Status.SUCCESS;
			
			Vec3 dest = Whiteboard.getDest(storage, address);
			if(dest == null || isWithinRangeOf(mobIn, dest))
				return Status.SUCCESS;
			
			return Status.RUNNING;
		}
		
		public void stop(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			mobIn.getNavigation().stop();
		}
		
		protected Vec3 getDest(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			Vec3 dest = Whiteboard.getDest(storage, address);
			Vec3 point = null;
			int attempts = 30;
			while(point == null && attempts-- > 0)
			{
				point = DefaultRandomPos.getPosAway(mobIn, 16, 7, dest);
				
				if(point != null && !validTargetPoint(mobIn, dest, point))
					point = null;
			}
			return point;
		}
		
		protected boolean validTargetPoint(PathfinderMob mobIn, Vec3 dest, Vec3 point)
		{
			Vec3 position = mobIn.position();
			Vec3 direction = point.subtract(mobIn.position()).normalize();
			return position.distanceTo(dest) >= position.add(direction).distanceTo(dest);
		}
		
		protected boolean isWithinRangeOf(PathfinderMob mobIn, Vec3 dest)
		{
			return mobIn.distanceToSqr(dest) >= (minDist * minDist);
		}
	}
	
	/**
	 * Performs a standard melee attack with the mob's main hand.<br>
	 * Note that this does not have any cooldown by itself.
	 */
	public static class AttackMelee extends LeafSingle
	{
		private final String address;
		
		public AttackMelee(String addressIn)
		{
			address = addressIn;
			setCustomName("attack_melee");
		}
		
		public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			Entity target = storage.getEntity(address);
			if(target == null || !target.isAlive() || mobIn.distanceToSqr(target) > getAttackReachSqr(target, mobIn))
				return false;
			
			if(storage.getEntity(address) == null)
				return false;
			
			mobIn.swing(InteractionHand.MAIN_HAND);
			mobIn.doHurtTarget(storage.getEntity(address));
			return true;
		}
		
		public static double getAttackReachSqr(Entity targetIn, LivingEntity mobIn)
		{
			return (double)(mobIn.getBbWidth() * 2F * mobIn.getBbWidth() * 2F + targetIn.getBbWidth());
		}
	}
	
	/** Adds the given item entity to the mob's main hand. */
	public static class PickUpItem extends LeafSingle
	{
		private final String address;
		
		public PickUpItem(String addressIn)
		{
			address = addressIn;
		}
		
		public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			ItemEntity target = (ItemEntity)storage.getEntity(address);
			if(target == null)
				return false;
			
			ItemStack heldItem = MobWhiteboard.getItemInSlot(storage, EquipmentSlot.MAINHAND); 
			if(heldItem.isEmpty() || canMergeStacks(heldItem, target.getItem()))
				if(!target.getBoundingBox().inflate(1).intersects(mobIn.getBoundingBox()))
					return false;
			
			ItemStack stack = target.getItem();
			if(heldItem.isEmpty())
			{
				mobIn.setItemInHand(InteractionHand.MAIN_HAND, stack);
				target.discard();
			}
			else
			{
				int dif = Math.min(heldItem.getMaxStackSize() - heldItem.getCount(), stack.getCount());
				heldItem.grow(dif);
				stack.shrink(dif);
				
				if(stack.getCount() <= 0)
					target.discard();
			}
			mobIn.setDropChance(EquipmentSlot.MAINHAND, 1F);
			return true;
		}
		
		private static boolean canMergeStacks(ItemStack stackA, ItemStack stackB)
		{
			if(
				!stackA.is(stackB.getItem()) ||
				stackA.getDamageValue() != stackB.getDamageValue() ||
				stackA.getCount() > stackA.getMaxStackSize())
				return false;
			return ItemStack.tagMatches(stackA, stackB);
				
		}
	}
	
	/** Unequips the given item and places it in the mob's main hand */
	public static class UnequipWornItem extends LeafSingle
	{
		private final EquipmentSlot slot;
		
		public UnequipWornItem(EquipmentSlot slotIn)
		{
			slot = slotIn;
		}
		
		public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			if(mobIn.getItemBySlot(slot).isEmpty() || !mobIn.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty())
				return false;
			
			mobIn.setItemInHand(InteractionHand.MAIN_HAND, mobIn.getItemBySlot(slot).copy());
			mobIn.setItemSlot(slot, ItemStack.EMPTY);
			return true;
		}
	}
	
	/** Drops the mob's current main hand item */
	public static class DropItem extends LeafSingle
	{
		private final int amount;
		
		public DropItem(int amountIn)
		{
			amount = amountIn;
			setCustomName("drop_held_item");
		}
		
		public DropItem() { this(-1); }
		
		public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
		{
			ItemStack dropped = mobIn.getMainHandItem();
			if(amount > 0)
				mobIn.spawnAtLocation(dropped.split(amount));
			else
			{
				mobIn.spawnAtLocation(dropped);
				mobIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
			return true;
		}
	}
	
	public static TreeNode startUsingItem()
	{
		return new LeafSingle()
		{
			public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				mobIn.startUsingItem(InteractionHand.MAIN_HAND);
				return true;
			}
		}.setCustomName("use_held_item");
	}
	
	public static TreeNode stopUsingItem()
	{
		return new LeafSingle()
		{
			public boolean doAction(PathfinderMob mobIn, Whiteboard<?> storage)
			{
				mobIn.releaseUsingItem();
				return true;
			}
		}.setCustomName("stop_held_item");
	}
}
