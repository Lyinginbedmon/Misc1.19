package com.example.examplemod.entities.ai;

import java.util.EnumSet;

import com.example.examplemod.entities.IGuardMob;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class EntityAIGuardFollowOwner<T extends Mob & IGuardMob> extends Goal
{
	private final T guard;
	
	private LivingEntity owner;
	private final LevelReader level;
	private final double speedModifier;
	private final PathNavigation navigation;
	private int timeToRecalcPath;
	private final float stopDistance;
	private final float startDistance;
	private float oldWaterCost;
	private final boolean canFly;
	
	public EntityAIGuardFollowOwner(T guardIn, double speedMod, float startDist, float stopDist, boolean canFlyIn)
	{
		this.guard = guardIn;
		this.level = guardIn.getLevel();
		this.speedModifier = speedMod;
		this.navigation = guardIn.getNavigation();
		this.startDistance = startDist;
		this.stopDistance = stopDist;
		this.canFly = canFlyIn;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		if (!(guardIn.getNavigation() instanceof GroundPathNavigation) && !(guardIn.getNavigation() instanceof FlyingPathNavigation))
			throw new IllegalArgumentException("Unsupported mob type for GuardFollowOwnerGoal");
	}
	
	public boolean canUse()
	{
		LivingEntity owner = this.guard.getOwner();
		if (owner == null || owner.isSpectator())
			return false;
		else if (this.guard.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance))
			return false;
		else
		{
			this.owner = owner;
			return true;
		}
	}
	
	public boolean canContinueToUse()
	{
		if (this.navigation.isDone())
			return false;
		else
			return !(this.guard.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance));
	}

	public void start()
	{
		this.timeToRecalcPath = 0;
		this.oldWaterCost = this.guard.getPathfindingMalus(BlockPathTypes.WATER);
		this.guard.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
	}
	
	public void stop()
	{
		this.owner = null;
		this.navigation.stop();
		this.guard.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
	}
	
	public void tick()
	{
		this.guard.getLookControl().setLookAt(this.owner, 10.0F, (float)this.guard.getMaxHeadXRot());
		if (--this.timeToRecalcPath <= 0)
		{
			this.timeToRecalcPath = this.adjustedTickDelay(10);
			if (!this.guard.isLeashed() && !this.guard.isPassenger())
			{
				if (this.guard.distanceToSqr(this.owner) >= 144.0D)
					this.teleportToOwner();
				else
					this.navigation.moveTo(this.owner, this.speedModifier);
			}
		}
	}
	
	private void teleportToOwner()
	{
		BlockPos blockpos = this.owner.blockPosition();
		for(int i = 0; i < 10; ++i)
		{
			int j = this.randomIntInclusive(-3, 3);
			int k = this.randomIntInclusive(-1, 1);
			int l = this.randomIntInclusive(-3, 3);
			boolean flag = this.maybeTeleportTo(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
			if (flag)
			{
				guard.setTarget(null);
				return;
			}
		}
	}
	
	private boolean maybeTeleportTo(int p_25304_, int p_25305_, int p_25306_)
	{
		if (Math.abs((double)p_25304_ - this.owner.getX()) < 2.0D && Math.abs((double)p_25306_ - this.owner.getZ()) < 2.0D)
			return false;
		else if (!this.canTeleportTo(new BlockPos(p_25304_, p_25305_, p_25306_)))
			return false;
		else
		{
			this.guard.moveTo((double)p_25304_ + 0.5D, (double)p_25305_, (double)p_25306_ + 0.5D, this.guard.getYRot(), this.guard.getXRot());
			this.navigation.stop();
			return true;
		}
	}
	
	private boolean canTeleportTo(BlockPos p_25308_)
	{
		BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, p_25308_.mutable());
		if (blockpathtypes != BlockPathTypes.WALKABLE)
			return false;
		else
		{
			BlockState blockstate = this.level.getBlockState(p_25308_.below());
			if (!this.canFly && blockstate.getBlock() instanceof LeavesBlock)
				return false;
			else
				return this.level.noCollision(this.guard, this.guard.getBoundingBox().move(p_25308_.subtract(this.guard.blockPosition())));
		}
	}
	
	private int randomIntInclusive(int minimum, int maximum) { return this.guard.getRandom().nextInt(maximum - minimum + 1) + minimum; }
}
