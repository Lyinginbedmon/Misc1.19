package com.example.examplemod.entity.ai.group;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

public class GroupPlayer extends GroupGeneric
{
	protected Player owner = null;
	private UUID ownerUUID;
	
	public GroupPlayer(@Nonnull Player playerIn, PathfinderMob... membersIn)
	{
		this(playerIn.getUUID(), membersIn);
		this.owner = playerIn;
	}
	
	public GroupPlayer(@Nonnull UUID idIn, PathfinderMob... membersIn)
	{
		super(membersIn);
		this.ownerUUID = idIn;
	}
	
	public ResourceLocation getKey(){ return GroupType.PLAYER; }
	
	public CompoundTag saveToNbt(CompoundTag compound)
	{
		saveMemberIds(compound);
		compound.put("Owner", NbtUtils.createUUID(ownerUUID));
		return compound;
	}
	
	public void loadFromNbt(CompoundTag compound)
	{
		this.ownerUUID = NbtUtils.loadUUID(compound.get("Owner"));
		loadMemberIds(compound);
	}
	
	public boolean shouldListenTo(LivingEntity entity) { return entity.getType() == EntityType.PLAYER && isOwner((Player)entity); }
	
	@Nullable
	public IMobGroup split(LivingEntity... membersIn)
	{
		if(membersIn.length == 0)
			return null;
		IMobGroup group = new GroupPlayer(this.ownerUUID);
		for(LivingEntity entity : membersIn)
			if(isMember(entity))
			{
				remove(entity);
				group.add(entity);
			}
		return GroupSaveData.get(membersIn[0].getServer()).register(group);
	}
	
	public boolean isOwner(Player playerIn)
	{
		if(playerIn.getUUID().equals(this.ownerUUID))
		{
			this.owner = playerIn;
			return true;
		}
		return false;
	}
	
	public static List<IMobGroup> getGroupsOfPlayer(Player playerIn)
	{
		return GroupSaveData.get(playerIn.getServer()).getGroups((group) -> { return group instanceof GroupPlayer && ((GroupPlayer)group).isOwner(playerIn); });
	}
	
	public boolean shouldNotPersist() { return ownerUUID == null; }
	
	public boolean isEmpty() { return shouldNotPersist(); }
	
	public static class Factory implements IGroupFactory
	{
		public IMobGroup create() { return new GroupPlayer(Reference.Values.DUMMY_ID); }
	}
}
