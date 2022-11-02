package com.example.examplemod.entity.ai.group.action;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class ActionUtils
{
	public static float assessDurability(LivingEntity target)
	{
		float health = 0.5F * (target.getHealth() / 20F);
		float armour = 0.5F * (float)(target.getAttributeValue(Attributes.ARMOR) / 20D);
		return health + armour;
	}
	
	/** Returns the utility value associated with a given plotted graph */
	public static float getInterpolatedUtility(double value, Map<Double,Float> plot)
	{
		Pair<Double,Double> bounds = getPlotBounds(value, plot.keySet());
		double keyUnder = bounds.getLeft();
		double keyOver = bounds.getRight();
		if(keyUnder == keyOver)
			return plot.get(keyUnder);
		
		double keySep = keyOver - keyUnder;
		double prog = (value - keyUnder) / keySep;
		
		float lastVal = plot.get(keyUnder);
		float nextVal = plot.get(keyOver);
		float valSep = nextVal - lastVal;
		
		return Mth.clamp(lastVal + (float)(valSep * prog), 0F, 1F);
	}
	
	private static Pair<Double,Double> getPlotBounds(double value, Set<Double> values) throws NullPointerException
	{
		if(values.isEmpty())
			throw new NullPointerException();
		
		double keyUnder = 0D, keyOver = Double.MIN_VALUE;
		for(double val : values)
			if(val > keyOver)
				keyOver = val;
		
		for(double val : values)
		{
			if(val > keyUnder && val <= value)
				keyUnder = val;
			
			if(val < keyOver && val >= value)
				keyOver = val;
		}
		
		return Pair.of(keyUnder, keyOver);
	}
	
	public static double getAttackReachSqr(Entity ent, Entity member)
	{
		if(ent.getType() == EntityType.PLAYER)
			return ((Player)ent).getAttackRange() + member.getBbWidth();
		else
			return (double)(ent.getBbWidth() * 2F * ent.getBbWidth() + member.getBbWidth());
	}
	
	@Nullable
	public static LivingEntity tryFindEntityNearby(@Nullable UUID uuidIn, List<LivingEntity> membersIn)
	{
		if(uuidIn != null && !membersIn.isEmpty())
			for(LivingEntity member : membersIn)
				for(LivingEntity entity : member.getLevel().getEntitiesOfClass(LivingEntity.class, member.getBoundingBox().inflate(16D)))
					if(entity.isAddedToWorld() && entity.getUUID().equals(uuidIn))
						return entity;
		return null;
	}
	
	@Nullable
	public static LivingEntity tryFindEntityNearby(@Nullable MemberData dataIn, List<LivingEntity> membersIn)
	{
		if(dataIn != null && !membersIn.isEmpty())
			for(LivingEntity member : membersIn)
				for(LivingEntity entity : member.getLevel().getEntitiesOfClass(LivingEntity.class, member.getBoundingBox().inflate(16D), dataIn::matches))
					return entity;
		return null;
	}
	
	/** Cachable entity variable that can be stored in NBT data */
	public static class MemberData
	{
		private LivingEntity entity;
		private UUID entityID = null;
		
		public MemberData(@Nullable LivingEntity memberIn)
		{
			entity = memberIn;
			if(memberIn != null)
				entityID = memberIn.getUUID();
		}
		
		public MemberData(UUID idIn)
		{
			entityID = idIn;
		}
		
		public boolean matches(@Nullable LivingEntity entityIn)
		{
			if(entityIn != null && entityIn.isAddedToWorld() && (get() == entityIn || entityIn.getUUID().equals(uuid())))
			{
				entity = entityIn;
				return true;
			}
			return false;
		}
		
		/** Returns true if the specified entity has been matched since instantiation */
		public boolean cached() { return this.entity != null; }
		
		public UUID uuid() { return this.entityID; }
		public LivingEntity get() { return this.entity; }
		
		public CompoundTag saveToNbt(CompoundTag tag)
		{
			tag.putUUID("UUID", this.entityID);
			return tag;
		}
		
		public static MemberData fromNbt(CompoundTag tag)
		{
			return new MemberData(tag.getUUID("UUID"));
		}
	}
}
