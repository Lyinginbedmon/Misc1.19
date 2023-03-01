package com.example.examplemod.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.HearthLightPathfinder;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityHearthLight extends LivingEntity
{
	protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(EntityHearthLight.class, EntityDataSerializers.OPTIONAL_UUID);
	private final Map<IndicatorState, AnimationState> stateToAnimation = new HashMap<>();
	
	private IndicatorState currentState = null;
	public final AnimationState indicatorHandIdle = new AnimationState();
	public final AnimationState indicatorHandWave = new AnimationState();
	public final AnimationState indicatorHandBeckoning = new AnimationState();
	public final AnimationState lanternHandIdle = new AnimationState();
	private int ticksUnseen = 0;
	
	private static final int PATH_RATE = 1;
	private static final int RECALC_TIME = Reference.Values.TICKS_PER_SECOND * 15;
	private int repathTimer = RECALC_TIME;
	private HearthLightPathfinder pathfinder;
	
	public EntityHearthLight(EntityType<? extends EntityHearthLight> p_19870_, Level p_19871_)
	{
		super(p_19870_, p_19871_);
		initAnimations();
	}
	
	protected void defineSynchedData()
	{
		super.defineSynchedData();
		this.entityData.define(DATA_OWNERUUID_ID, Optional.empty());
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
	
	public boolean hasOwner() { return getOwnerUUID() != null; }
	public UUID getOwnerUUID() { return this.entityData.get(DATA_OWNERUUID_ID).orElse(null); }
	public void setOwnerUUID(UUID idIn) { this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(idIn)); }
	
	@Nullable
	public Player getOwner()
	{
//		return hasOwner() ? getLevel().getPlayerByUUID(getOwnerUUID()) : null;
		return getLevel().getNearestPlayer(this, 64D);
	}
	
	public Iterable<ItemStack> getArmorSlots() { return NonNullList.withSize(4, ItemStack.EMPTY); }
	public ItemStack getItemBySlot(EquipmentSlot p_21127_) { return ItemStack.EMPTY; }
	public void setItemSlot(EquipmentSlot p_21036_, ItemStack p_21037_) { }
	public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
	
	private void initAnimations()
	{
		stateToAnimation.put(IndicatorState.IDLE, indicatorHandIdle);
		stateToAnimation.put(IndicatorState.WAVING, indicatorHandWave);
		stateToAnimation.put(IndicatorState.BECKONING, indicatorHandBeckoning);
	}
	
	public void tick()
	{
		if(this.getLevel().isClientSide())
		{
			this.lanternHandIdle.startIfStopped(this.tickCount);
			if(getOwner() == null)
				return;
			
			/**
			 * TODO Implement additional animations for indicator hand
			 * IF hand is at breadcrumb -> Beckoning animation
			 * IF hand is moving to breadcrumb -> Pointing animation
			 * IF player isn't following path -> Tutting animation
			 */
			if(hasLineOfSight(getOwner()))
				this.ticksUnseen -= Math.signum(this.ticksUnseen);
			else if(this.ticksUnseen < Reference.Values.TICKS_PER_SECOND * 3)
			{
				if(this.ticksUnseen == 0)
					this.ticksUnseen = Reference.Values.TICKS_PER_SECOND;
				else
					this.ticksUnseen++;
			}
			
			IndicatorState nextState = getState();
			if(nextState != currentState)
			{
				stateToAnimation.forEach((state,anim) -> anim.stop());
				
				currentState = nextState;
				if(currentState != null)
					stateToAnimation.get(currentState).start(this.tickCount);
			}
			
			Player owner = getOwner();
			if(owner != null)
			{
				if(pathfinder == null)
					this.pathfinder = new HearthLightPathfinder(owner);
				
				if(pathfinder.searchCompleted() && --repathTimer <= 0)
				{
					repathTimer = RECALC_TIME;
//					pathfinder.generatePath();
					pathfinder.start();
				}
				else if(this.tickCount%PATH_RATE == 0)
					pathfinder.tickPath(getLevel());
			}
		}
		super.tick();
	}
	
	public IndicatorState currentIndication() { return this.currentState == null ? IndicatorState.IDLE : this.currentState; }
	
	public IndicatorState getState()
	{
		if(this.ticksUnseen > 0)
			return IndicatorState.WAVING;
		else
		{
			if(getOwner().distanceTo(this) > 8D)
				return IndicatorState.BECKONING;
		}
		return IndicatorState.IDLE;
	}
	
	public boolean isVisibleFor(Player player) { return !hasOwner() || player.isCreative() || player.getUUID().equals(getOwnerUUID()); }
	
	public boolean hasPathToShow() { return this.pathfinder != null && this.pathfinder.hasPath(); }
	
	public HearthLightPathfinder getPathfinder() { return this.pathfinder; }
	
	public static enum IndicatorState
	{
		IDLE,
		WAVING,
		POINTING,
		BECKONING,
		TUTTING;
	}
}
