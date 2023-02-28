package com.example.examplemod.entities;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public interface IGuardMob
{
	public default boolean hasOwner() { return getOwnerUUID() != null; }
	
	@Nullable
	public default LivingEntity getOwner() { return hasOwner() ? getLevel().getPlayerByUUID(getOwnerUUID()) : null; }
	
	public Level getLevel();
	
	@Nullable
	public UUID getOwnerUUID();
	
	public void setOwnerUUID(@Nullable UUID idIn);
	
	public static boolean isGuardingSamePlayer(IGuardMob mob1, IGuardMob mob2) { return mob1.getOwnerUUID().equals(mob2.getOwnerUUID()); }
	
	public static boolean isOwnerOrFriend(IGuardMob mob, LivingEntity entityIn)
	{
		return entityIn.getUUID().equals(mob.getOwnerUUID()) || (entityIn instanceof IGuardMob && isGuardingSamePlayer(mob, ((IGuardMob)entityIn)));
	}
	
	public default boolean wantsToAttack(LivingEntity entityIn, LivingEntity owner) { return !isOwnerOrFriend(this, entityIn); }
}
