package com.example.examplemod.entities.ai;

import java.util.EnumSet;

import com.example.examplemod.entities.IGuardMob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class EntityAIGuardOwnerHurtTarget<T extends Mob & IGuardMob> extends TargetGoal
{
	private final T guard;
	private LivingEntity ownerLastHurt;
	private int timestamp;
	
	public EntityAIGuardOwnerHurtTarget(T guardIn)
	{
		super(guardIn, false);
		this.guard = guardIn;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}
	
	public boolean canUse()
	{
		if (this.guard.hasOwner())
		{
			LivingEntity owner = this.guard.getOwner();
			if (owner == null)
				return false;
			else
			{
				this.ownerLastHurt = owner.getLastHurtMob();
				int i = owner.getLastHurtMobTimestamp();
				return i != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && this.guard.wantsToAttack(this.ownerLastHurt, owner);
			}
		}
		else
			return false;
	}
	
	public void start()
	{
		this.mob.setTarget(this.ownerLastHurt);
		LivingEntity owner = this.guard.getOwner();
		if (owner != null)
			this.timestamp = owner.getLastHurtMobTimestamp();
		
		super.start();
	}
}
