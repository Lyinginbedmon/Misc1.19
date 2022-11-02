package com.example.examplemod.entity.ai.group.action;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.group.action.ActionUtils.MemberData;
import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public abstract class ActionFormation extends GroupAction
{
	protected double minDist;
	protected double maxDist;
	
	// List of block positions either occupied or attempting to be occupied by members of this group
	private Map<MemberData, BlockPos> guardFormation = new HashMap<>();
	
	protected ActionFormation(ResourceLocation nameIn, int complementIn)
	{
		super(nameIn, complementIn);
	}
	
	public CompoundTag saveToNbt(CompoundTag compound)
	{
		compound.putDouble("Min", minDist);
		compound.putDouble("Max", maxDist);
		
		ListTag formationData = new ListTag();
		guardFormation.forEach((data,pos) -> 
		{
			CompoundTag tag = new CompoundTag();
			tag.put("Data", data.saveToNbt(new CompoundTag()));
			tag.put("Pos", NbtUtils.writeBlockPos(pos));
			formationData.add(tag);
		});
		compound.put("Formation", formationData);
		return compound;
	}
	
	public void loadFromNbt(CompoundTag compound)
	{
		minDist = compound.getDouble("Min");
		maxDist = compound.getDouble("Max");
		
		guardFormation.clear();
		ListTag formationData = compound.getList("Formation", Tag.TAG_COMPOUND);
		for(int i=0; i<formationData.size(); i++)
		{
			CompoundTag tag = formationData.getCompound(i);
			guardFormation.put(MemberData.fromNbt(tag.getCompound("Data")), NbtUtils.readBlockPos(tag.getCompound("Pos")));
		}
	}
	
	public Collection<BlockPos> formationPoints() { return guardFormation.values(); }
	
	protected void tick(List<LivingEntity> membersIn, List<LivingEntity> targetsIn, Level world)
	{
		// Recache tracked members post-boot
		for(LivingEntity member : membersIn)
			getTrackedPos(member);
		
		if(guardFormation.size() > membersIn.size())
			clearFormation();
	}
	
	protected void clearFormation() { guardFormation.clear(); }
	
	protected void addTrackedPos(LivingEntity entity, BlockPos pos)
	{
		guardFormation.put(new MemberData(entity), pos);
	}
	
	protected void removeTrackedEntity(@Nullable LivingEntity entity)
	{
		if(entity == null)
			return;
		
		MemberData entry = null;
		for(MemberData key : guardFormation.keySet())
			if(key.matches(entity))
			{
				entry = key;
				break;
			}
		if(entry != null)
			guardFormation.remove(entry);
	}
	
	protected BlockPos getTrackedPos(@Nullable LivingEntity entity)
	{
		if(entity != null)
			for(MemberData data : guardFormation.keySet())
				if(data.matches(entity))
					return guardFormation.get(data);
		return BlockPos.ZERO;
	}
	
	protected List<LivingEntity> trackedMembers()
	{
		List<LivingEntity> tracked = Lists.newArrayList();
		guardFormation.keySet().forEach((data) -> { if(data.cached()) tracked.add(data.get()); });
		return tracked;
	}
}