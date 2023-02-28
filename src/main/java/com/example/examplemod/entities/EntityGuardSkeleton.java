package com.example.examplemod.entities;

import java.util.Optional;
import java.util.UUID;

import com.example.examplemod.entities.ai.EntityAIGuardFollowOwner;
import com.example.examplemod.entities.ai.EntityAIGuardOwnerHurtByTarget;
import com.example.examplemod.entities.ai.EntityAIGuardOwnerHurtTarget;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EntityGuardSkeleton extends Skeleton implements IGuardMob
{
	protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(EntityGuardSkeleton.class, EntityDataSerializers.OPTIONAL_UUID);
	public EntityGuardSkeleton(EntityType<? extends EntityGuardSkeleton> p_33570_, Level p_33571_)
	{
		super(p_33570_, p_33571_);
	}
	
	protected void defineSynchedData()
	{
		super.defineSynchedData();
		this.entityData.define(DATA_OWNERUUID_ID, Optional.empty());
	}
	
	protected void registerGoals()
	{
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0D, 1.2D));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		
		this.goalSelector.addGoal(6, new EntityAIGuardFollowOwner<>(this, 1.0D, 10.0F, 2.0F, false));
		this.targetSelector.addGoal(1, new EntityAIGuardOwnerHurtByTarget<>(this));
		this.targetSelector.addGoal(2, new EntityAIGuardOwnerHurtTarget<>(this));
	}
	
	public void addAdditionalSaveData(CompoundTag compound)
	{
		super.addAdditionalSaveData(compound);
		if(getOwnerUUID() != null)
			compound.putUUID("Owner", getOwnerUUID());
	}
	
	public void readAdditionalSaveData(CompoundTag compound)
	{
		super.readAdditionalSaveData(compound);
		if(compound.hasUUID("Owner"))
			setOwnerUUID(compound.getUUID("Owner"));
	}
	
	public boolean isSunBurnTick() { return false; }
	public boolean isPreventingPlayerRest(Player player) { return false; }
	
	public boolean canAttack(LivingEntity entityIn) { return IGuardMob.isOwnerOrFriend(this, entityIn) ? false : super.canAttack(entityIn); }
	
	public UUID getOwnerUUID() { return this.entityData.get(DATA_OWNERUUID_ID).orElse(null); }
	
	public void setOwnerUUID(UUID idIn) { this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(idIn)); }
}
