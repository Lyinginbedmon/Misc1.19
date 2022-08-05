package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.ai.Node.Leaf;
import com.example.examplemod.entity.ai.Node.LeafSingle;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
	public static Node jump()
	{
		return new Leaf("jump")
		{
			private boolean hasJumped = false;
			
			public void run(Mob mobIn, Whiteboard<?> storage)
			{
				if(hasJumped)
				{
					if(mobIn.isOnGround())
						reset();
				}
				else
				{
					mobIn.getJumpControl().jump();
					hasJumped = true;
				}
			}
			
			private void reset()
			{
				hasJumped = false;
				resetNode();
			}
		};
	}
	
	/** Attempts to equip the item held in the mob's main hand */
	public static Node equipHeldItem()
	{
		return new LeafSingle("equip_held_item")
		{
			protected void doAction(Mob mobIn, Whiteboard<?> storage)
			{
				EquipmentSlot slot = Mob.getEquipmentSlotForItem(mobIn.getMainHandItem());
				if(mobIn.equipItemIfPossible(mobIn.getMainHandItem()))
				{
					mobIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					mobIn.setDropChance(slot, 1F);
				}
			}
		};
	}
	
	/** Swaps the items held in the mob's hands */
	public static Node swapItems()
	{
		return new LeafSingle("swap_held_items")
		{
			protected void doAction(Mob mobIn, Whiteboard<?> storage)
			{
				ItemStack stackA = mobIn.getMainHandItem().copy();
				ItemStack stackB = mobIn.getOffhandItem().copy();
				mobIn.setItemInHand(InteractionHand.MAIN_HAND, stackB);
				mobIn.setItemInHand(InteractionHand.OFF_HAND, stackA);
			}
		};
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
			super("make_sound");
			sounds = soundsIn;
		}
		
		public void doAction(Mob mobIn, Whiteboard<?> storage)
		{
			mobIn.playSound(sounds.length == 1 ? sounds[0] : sounds[mobIn.getRandom().nextInt(sounds.length)]);
		}
	}
	
	/**
	 * Looks at a given point.<br>
	 * Warning: This leaf never terminates, so should be used in a Parallel node with another action to terminate it.
	 */
	public static class LookAtConstant extends Leaf
	{
		private final String address;
		private final float xSpeed, ySpeed;
		
		public static LookAtConstant instant(String target) { return new LookAtConstant(target, 30F, 30F); }
		public static LookAtConstant normal(String target) { return new LookAtConstant(target, -1F, -1F); }
		public static LookAtConstant lazy(String target) { return new LookAtConstant(target, 1F, 1F); }
		
		protected LookAtConstant(String addressIn, float xSpeedIn, float ySpeedIn)
		{
			super("look_at_constant");
			address = addressIn;
			xSpeed = xSpeedIn;
			ySpeed = ySpeedIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage) { return storage.hasValue(address); }
		
		public void run(Mob mobIn, Whiteboard<?> storage)
		{
			Vec3 target = getLookAtVec(storage);
			if(target != null)
				if(xSpeed < 0F && ySpeed < 0F)
					mobIn.getLookControl().setLookAt(target);
				else
					mobIn.getLookControl().setLookAt(target.x, target.y, target.z, xSpeed, ySpeed);
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
	public static class Wait extends Leaf
	{
		private final int min, max;
		private int ticks = 0;
		
		public Wait(int durationIn)
		{
			this(durationIn, durationIn);
		}
		
		public Wait(int minTicks, int maxTicks)
		{
			super("wait");
			min = Math.min(minTicks, maxTicks);
			max = Math.max(minTicks, maxTicks);
		}
		
		public void start(Mob mobIn, Whiteboard<?> storage)
		{
			if(min != max)
				ticks = min + mobIn.getRandom().nextInt(max - min);
			else
				ticks = min;
		}
		
		public void run(Mob mobIn, Whiteboard<?> storage)
		{
			if(--ticks <= 0)
				reset();
		}
		
		public void stop(Mob mobIn, Whiteboard<?> storage) { reset(); }
		
		private void reset()
		{
			ticks = -1;
			resetNode();
		}
	}
	
	/**
	 * Sets the mob's navigator to move to the given position, then terminates when the navigator is empty.<br>
	 * Note that the navigator may be empty simply because no valid path exists.
	 */
	public static class MoveTo extends Leaf
	{
		private final String address;
		private final double speed;
		// FIXME Not precise enough for item collection, increase precision
		public MoveTo(String addressIn, double speedIn)
		{
			super("move_to");
			address = addressIn;
			speed = speedIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage) { return storage.hasValue(address); }
		
		public void start(Mob mobIn, Whiteboard<?> storage)
		{
			Vec3 dest = Whiteboard.getDest(storage, address);
			if(dest == null)
			{
				resetNode();
				return;
			}
			mobIn.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
		}
		
		public void run(Mob mobIn, Whiteboard<?> storage)
		{
			if(mobIn.getNavigation().isDone())
				resetNode();
		}
		
		public void stop(Mob mobIn, Whiteboard<?> storage)
		{
			mobIn.getNavigation().stop();
			resetNode();
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
			super("attack_melee");
			address = addressIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			Entity target = storage.getEntity(address);
			return mobIn.distanceToSqr(target) <= getAttackReachSqr(target, mobIn);
		}
		
		public void doAction(Mob mobIn, Whiteboard<?> storage)
		{
			if(storage.getEntity(address) == null)
				return;
			
			mobIn.swing(InteractionHand.MAIN_HAND);
			mobIn.doHurtTarget(storage.getEntity(address));
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
			super("pick_up_item");
			address = addressIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			ItemEntity target = (ItemEntity)storage.getEntity(address);
			if(target == null)
				return false;
			
			ItemStack heldItem = MobWhiteboard.getItemInSlot(storage, EquipmentSlot.MAINHAND); 
			if(heldItem.isEmpty() || canMergeStacks(heldItem, target.getItem()))
				return target.getBoundingBox().inflate(1).intersects(mobIn.getBoundingBox());
			
			return false;
		}
		
		public void doAction(Mob mobIn, Whiteboard<?> storage)
		{
			ItemEntity target = (ItemEntity)storage.getEntity(address);
			ItemStack stack = target.getItem();
			ItemStack heldItem = MobWhiteboard.getItemInSlot(storage, EquipmentSlot.MAINHAND); 
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
			super("unequip_"+slotIn.name().toLowerCase());
			slot = slotIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage) { return !mobIn.getItemBySlot(slot).isEmpty() && mobIn.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(); }
		
		protected void doAction(Mob mobIn, Whiteboard<?> storage)
		{
			mobIn.setItemInHand(InteractionHand.MAIN_HAND, mobIn.getItemBySlot(slot).copy());
			mobIn.setItemSlot(slot, ItemStack.EMPTY);
		}
	}
	
	/** Drops the mob's current main hand item */
	public static class DropItem extends LeafSingle
	{
		private final int amount;
		
		public DropItem(int amountIn)
		{
			super("drop_held_item");
			amount = amountIn;
		}
		
		public DropItem() { this(-1); }
		
		protected void doAction(Mob mobIn, Whiteboard<?> storage)
		{
			ItemStack dropped = mobIn.getMainHandItem();
			if(amount > 0)
				mobIn.spawnAtLocation(dropped.split(amount));
			else
			{
				mobIn.spawnAtLocation(dropped);
				mobIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
		}
	}
}
