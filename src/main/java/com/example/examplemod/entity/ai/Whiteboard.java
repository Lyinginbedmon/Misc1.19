package com.example.examplemod.entity.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.tree.Branches;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public abstract class Whiteboard<T>
{
	private final Map<String, Object> values = new HashMap<>();
	private Whiteboard<?> parent;
	
	private final Map<String, Function<T,Object>> expansions = new HashMap<>();
	private final Map<ResourceLocation, Integer> timers = new HashMap<>();
	private final Map<ResourceLocation, Counter> counters = new HashMap<>();
	
	private CommandStack commands = null;
	
	@Nullable
	public static Whiteboard<?> tryGetWhiteboard(LivingEntity objIn)
	{
		if(objIn instanceof ITreeEntity && objIn instanceof PathfinderMob)
			return ((ITreeEntity)objIn).getWhiteboard((PathfinderMob)objIn);
		return null;
	}
	
	public final void addExpansion(String address, Function<T,Object> expansionIn) { expansions.put(address, expansionIn); }
	
	public final boolean hasValue(String input) { return values.containsKey(input); }
	
	public final void setValue(String input, Object value)
	{
		if(value != null)
			values.put(input, value);
		else if(hasValue(input))
			clearValue(input);
	}
	
	public final void clearValue(String input) { values.remove(input); }
	
	public final Object get(String input)
	{
		/**
		 * If value does not exist in this whiteboard, check the parent (if any) instead.<br>
		 * This means that descendant whiteboards always overrule parents.
		 */
		if(!hasValue(input) && parent != null)
			return parent.get(input);
		return values.get(input);
	}
	
	public final void setParent(Whiteboard<?> board) { this.parent = board; }
	
	public final boolean hasParent() { return this.parent != null; }
	public final Whiteboard<?> getParent(){ return this.parent; }
	
	public boolean getBoolean(String input) { return (boolean)get(input); }
	
	public double getDouble(String input) { return (double)get(input); }
	
	public float getFloat(String input) { return (float)get(input); }
	
	public BlockPos getBlockPos(String input) { return (BlockPos)get(input); }
	
	public Vec3 getVec3(String input) { return (Vec3)get(input); }
	
	public Entity getEntity(String input) { return (Entity)get(input); }
	
	public ItemStack getItemStack(String input) { return (ItemStack)get(input); }
	
	public int getInt(String input) { return (int)get(input); }
	
	public final void tick(T obj)
	{
		/** Update all registered expansions */
		for(String address : expansions.keySet())
			setValue(address, expansions.get(address).apply(obj));
		
		/** Timer handling, decrement towards 0 then erase */
		List<ResourceLocation> finished = Lists.newArrayList();
		for(ResourceLocation key : this.timers.keySet())
		{
			int val = getTimer(key);
			if(val != 0)
				setTimer(key, val - (int)Math.signum(val));
			else
				finished.add(key);
		}
		finished.forEach((key) -> this.timers.remove(key));
		
		this.counters.forEach((key, counter) -> counter.tick(obj));
		
		/** Perform any class-specific operations */
		specialDataOperations(obj);
	}
	
	/** Gets the current value of an internal timer, or zero if it doesn't exist */
	public final int getTimer(ResourceLocation name) { return this.timers.getOrDefault(name, 0); }
	/** Sets the value of an internal timer, which is constantly ticked towards zero */
	public final void setTimer(ResourceLocation name, int val) { this.timers.put(name, val); }
	
	/** Gets the current value of an internal counter, or zero if it doesn't exist */
	public final int getCounter(ResourceLocation name) { return this.counters.containsKey(name) ? this.counters.get(name).getValue() : 0; }
	/** Registers an internal counter, which ticks up as long as its qualifier is true, resetting to 0 when it first becomes true */
	public final void addCounter(ResourceLocation name, Predicate<T> inc) { addCounter(name, inc, Predicates.alwaysFalse()); }
	public final void addCounter(ResourceLocation name, Predicate<T> inc, Predicate<T> reset) { this.counters.put(name, new Counter(inc, reset)); }
	
	public final boolean hasCommands() { return this.commands != null && !this.commands.isEmpty(); }
	public final void setCommands(CommandStack stackIn) { this.commands = stackIn; }
	public final CommandStack getCommands() { return this.commands; }
	public final MobCommand currentCommand() { return hasCommands() ? getCommands().current() : null; }
	
	public void specialDataOperations(T obj) { }
	
	/** Attempts to retrieve a position at the given address, whether it is stored as a vector, block position, or entity */
	public static Vec3 getDest(Whiteboard<?> storage, String address)
	{
		Vec3 dest = null;
		try
		{
			dest = storage.getVec3(address);
		}
		catch(Exception e) { }
		
		if(dest == null)
			try
			{
				BlockPos pos = storage.getBlockPos(address);
				dest = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			}
			catch(Exception e) { }
		
		if(dest == null)
			try
			{
				Entity ent = storage.getEntity(address);
				dest = ent.position();
			}
			catch(Exception e) { }
		
		return dest;
	}
	
	private class Counter
	{
		private final Predicate<T> incQualifier;
		private final Predicate<T> resetQualifier;
		
		private int value = 0;
		private boolean prevQuality = false;
		
		public Counter(Predicate<T> incIn, Predicate<T> resetIn)
		{
			this.incQualifier = incIn;
			this.resetQualifier = resetIn;
		}
		
		public void tick(T mob)
		{
			if(resetQualifier.test(mob))
			{
				this.value = 0;
				return;
			}
			
			boolean quality = incQualifier.test(mob);
			if(quality)
				if(!prevQuality)
					value = 0;
				else
					value++;
			prevQuality = quality;
		}
		
		public int getValue() { return this.value; }
	}
	
	public static class Expansions
	{
		public static final String BEST_SWORD = "best_sword_entity";
		public static final String BEST_HEAD = "best_helmet_entity";
		public static final String BEST_CHEST = "best_chestplate_entity";
		public static final String BEST_LEGS = "best_leggings_entity";
		public static final String BEST_FEET = "best_boots_entity";
		
		public static final <T extends Entity> Object getBestSword(T input)
		{
			ItemEntity object = null;
			double best = Double.MIN_VALUE;
			for(ItemEntity entity : input.getLevel().getEntitiesOfClass(ItemEntity.class, input.getBoundingBox().inflate(15D)))
			{
				double damage = Branches.getDamageBonus(entity.getItem(), input instanceof Mob ? (Mob)input : null);
				if(damage > 0 && damage > best)
				{
					best = damage;
					object = entity;
				}
			}
			return object;
		}
		
		public static final <T extends Entity> Object getBestHead(T input) { return getBestArmor(input, EquipmentSlot.HEAD); }
		public static final <T extends Entity> Object getBestChest(T input) { return getBestArmor(input, EquipmentSlot.CHEST); }
		public static final <T extends Entity> Object getBestLegs(T input) { return getBestArmor(input, EquipmentSlot.LEGS); }
		public static final <T extends Entity> Object getBestFeet(T input) { return getBestArmor(input, EquipmentSlot.FEET); }
		
		protected static final <T extends Entity> Object getBestArmor(T input, EquipmentSlot slot)
		{
			ItemEntity object = null;
			double best = Double.MIN_VALUE;
			for(ItemEntity entity : input.getLevel().getEntitiesOfClass(ItemEntity.class, input.getBoundingBox().inflate(15D), new Predicate<ItemEntity>()
			{
				public boolean apply(ItemEntity input) { return input.getItem().canEquip(slot, null); }
			}))
			{
				double armor = Branches.getArmorBonus(entity.getItem(), slot);
				if(armor > 0 && armor > best)
				{
					best = armor;
					object = entity;
				}
			}
			return object;
		}
	}
	
	public static class GroupWhiteboard<T extends ITreeEntity> extends Whiteboard<T>
	{
		
	}
	
	public static class MobWhiteboard<T extends Mob> extends Whiteboard<T>
	{
		private final Mob theMob;
		
		public static final String MOB_ENTITY = "mob";
		public static final String MOB_HEALTH = "mob_health";
		public static final String MOB_MAX_HEALTH = "mob_max_health";
		public static final String MOB_DAMAGE = "mob_attack_damage";
		public static final String MOB_ARMOR = "mob_armour";
		public static final String MOB_SPEED = "mob_move_speed";
		
		private static final Map<EquipmentSlot, String> ITEM_ADDRESSES = new HashMap<>();
		
		public static final ResourceLocation MOB_MELEE_COOLDOWN = new ResourceLocation("mob_melee_cooldown");
		public static final String MOB_ITEM_USING = "mob_using_item";
		public static final String MOB_TICKS_USING = "mob_using_ticks";
		
		public static final String MOB_POS_VEC = "mob_position";
		public static final String MOB_POS_BLOCK = "mob_blockpos";
		
		public static final String ATTACK_TARGET = "attack_target";
		public static final String AI_TARGET = "ai_target";
		public static final String MOB_TARGET = "mob_attack_target";
		public static final String MOB_TARGET_VISIBLE = "mob_can_see_target";
		/** Increments whilst mob has attack target it cannot see, resets while no attack target */
		public static final ResourceLocation MOB_TARGET_NOT_VISIBLE = new ResourceLocation("mob_cannot_see_target");
		public static final String MOB_MOUNT = "mob_mount";
		
		public static final String MOB_LEASHED = "mob_is_leashed";
		public static final String MOB_LEASHER = "mob_leash_holder";
		public static final String MOB_POS_LEASH = "mob_leash_pos";
		
		public static final String MOB_HOME_EXISTS = "mob_restricted";
		public static final String MOB_HOME_POS = "mob_restriction_pos";
		public static final String MOB_HOME_RADIUS = "mob_restriction_radius";
		
		public MobWhiteboard(Mob mobIn)
		{
			theMob = mobIn;
			addExpansion(MOB_ENTITY, this::getMob);
			addExpansion(MOB_HEALTH, Mob::getHealth);
			addExpansion(MOB_MAX_HEALTH, Mob::getMaxHealth);
			
			addExpansion(MOB_DAMAGE, this::getMobAttackDamage);
			addExpansion(MOB_ARMOR, this::getMobArmor);
			addExpansion(MOB_SPEED, this::getMobSpeed);
			
			for(EquipmentSlot slot : EquipmentSlot.values())
				addExpansion(getSlotAddress(slot), new Function<T,Object>()
				{
					public Object apply(T input) { return input.getItemBySlot(slot); }
				});
			
			addExpansion(MOB_POS_VEC, Mob::position);
			addExpansion(MOB_POS_BLOCK, Mob::blockPosition);
			
			addExpansion(MOB_ITEM_USING, Mob::getUseItem);
			addExpansion(MOB_TICKS_USING, Mob::getTicksUsingItem);
			
			addExpansion(MOB_HOME_EXISTS, Mob::hasRestriction);
			addExpansion(MOB_HOME_RADIUS, Mob::getRestrictRadius);
			addExpansion(MOB_HOME_POS, Mob::getRestrictCenter);
			
			addExpansion(ATTACK_TARGET, this::getCurrentTarget);
			addExpansion(MOB_TARGET, Mob::getTarget);
			addExpansion(MOB_TARGET_VISIBLE, this::canSeeTarget);
			addCounter(MOB_TARGET_NOT_VISIBLE, this::cannotSeeTarget, this::canSeeTarget);
			addExpansion(MOB_MOUNT, Mob::getVehicle);
			
			addExpansion(MOB_LEASHER, Mob::isLeashed);
			addExpansion(MOB_POS_LEASH, this::getLeashKnotPosition);
		}
		
		public final Mob getMob(Mob mobIn) { return mobIn; }
		
		private double getMobAttackDamage(Mob mobIn) { return mobIn.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? mobIn.getAttributeValue(Attributes.ATTACK_DAMAGE) : -1D; }
		private double getMobArmor(Mob mobIn) { return mobIn.getAttributes().hasAttribute(Attributes.ARMOR) ? mobIn.getAttributeValue(Attributes.ARMOR) : -1D; }
		private double getMobSpeed(Mob mobIn) { return mobIn.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED) ? mobIn.getAttributeValue(Attributes.MOVEMENT_SPEED) : -1D; }
		private boolean hasTarget(Mob mobIn) { return getCurrentTarget(mobIn) != null; }
		private boolean canSeeTarget(Mob mobIn) { return hasTarget(mobIn) && mobIn.getSensing().hasLineOfSight(getCurrentTarget(mobIn)); }
		private boolean cannotSeeTarget(Mob mobIn) { return hasTarget(mobIn) && !mobIn.getSensing().hasLineOfSight(getCurrentTarget(mobIn)); }
		
		private static final Predicate<Entity> IS_VALID_TARGET = (entity) -> { return entity != null && entity.isAlive() && entity.isAddedToWorld(); }; 
		
		@Nullable
		public Entity getCurrentTarget(Mob mobIn)
		{
			/* Prioritise assigned whiteboard target over mob target */
			Entity target = null;
			
			if(hasValue(AI_TARGET))
			{
				target = getEntity(AI_TARGET);
				if(!IS_VALID_TARGET.apply(target))
					target = null;
			}
			
			if(target == null)
			{
				target = getEntity(MOB_TARGET);
				if(!IS_VALID_TARGET.apply(target))
					target = null;
			}
			
			return target;
		}
		
		private BlockPos getLeashKnotPosition(Mob mobIn)
		{
			Entity holder = theMob.getLeashHolder();
			return (holder != null && holder.getType() == EntityType.LEASH_KNOT) ? holder.blockPosition() : null;
		}
		
		public static String getSlotAddress(EquipmentSlot slot)
		{
			return ITEM_ADDRESSES.get(slot);
		}
		
		public static ItemStack getItemInSlot(Whiteboard<?> storage, EquipmentSlot slot)
		{
			String address = getSlotAddress(slot);
			return storage.hasValue(address) ? storage.getItemStack(address) : ItemStack.EMPTY;
		}
		
		static
		{
			for(EquipmentSlot slot : EquipmentSlot.values())
				ITEM_ADDRESSES.put(slot, "mob_equip_"+slot.name().toLowerCase());
		}
	}
	
	public static class TamableWhiteboard extends MobWhiteboard<TamableAnimal>
	{
		public static final String TAMABLE_TAMED = "tamable_is_tamed";
		public static final String TAMABLE_OWNER = "tamable_owner";
		public static final String TAMABLE_SITTING = "tamable_is_sitting";
		
		public TamableWhiteboard(TamableAnimal mobIn)
		{
			super(mobIn);
			addExpansion(TAMABLE_TAMED, TamableAnimal::isTame);
			addExpansion(TAMABLE_OWNER, TamableAnimal::getOwner);
			addExpansion(TAMABLE_SITTING, TamableAnimal::isOrderedToSit);
		}
	}
}
