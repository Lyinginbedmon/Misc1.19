package com.example.examplemod.utility;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ai.group.GroupPlayer;
import com.example.examplemod.entity.ai.group.GroupType;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSyncGroups;
import com.example.examplemod.reference.Reference;
import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class GroupSaveData extends SavedData
{
    private static final String DATA_NAME = Reference.ModInfo.MOD_ID + ":group_data";
    private static final String TAG_GROUPS = "Groups";
    public static GroupSaveData clientStorageCopy = new GroupSaveData();
    
    private List<IMobGroup> allGroups = Lists.newArrayList();
    private boolean needsSync = false;
    
    public static GroupSaveData read(CompoundTag compound) 
    {
    	GroupSaveData groupManager = new GroupSaveData();
        ListTag tagList = compound.getList(TAG_GROUPS, Tag.TAG_COMPOUND);
        for(Tag tag : tagList)
        {
            CompoundTag data = (CompoundTag)tag;
            if(!data.contains("Type", Tag.TAG_STRING))
            	continue;
            
            ResourceLocation type = new ResourceLocation(data.getString("Type"));
            IMobGroup group = GroupType.createGroupFromName(type);
            if(group == null)
            	continue;
            
            CompoundTag values = data.getCompound("Values");
            group.loadFromNbt(values);
            groupManager.register(group);
        }
        return groupManager;
    }
    
    public CompoundTag save(CompoundTag compound) {
        ListTag groupList = new ListTag();
        for(IMobGroup group : allGroups)
        {
        	if(group.shouldNotPersist())
        		continue;
        	group.setDirty(false);
        	
        	CompoundTag data = new CompoundTag();
        	data.putString("Type", group.getKey().toString());
        	data.put("Values", group.saveToNbt(new CompoundTag()));
        	groupList.add(data);
        }
        compound.put(TAG_GROUPS, groupList);
        return compound;
    }
    
    public static GroupSaveData get(@Nullable MinecraftServer server)
    {
        if(server != null)
        {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            return Objects.requireNonNull(overworld).getDataStorage().computeIfAbsent(GroupSaveData::read, GroupSaveData::new, DATA_NAME);
        }
        
        return clientStorageCopy;
    }
	
    public int size() { return allGroups.size(); }
    
	@Nullable
	public IMobGroup register(@Nullable IMobGroup groupIn)
	{
		if(groupIn != null && !allGroups.contains(groupIn) && !groupIn.shouldNotPersist())
		{
			allGroups.add(groupIn);
			setDirty();
			return groupIn;
		}
		return null;
	}
	
	public List<IMobGroup> getAllGroups() { return this.allGroups; }
	
	public void remove(IMobGroup groupIn)
	{
		if(allGroups.contains(groupIn))
		{
			allGroups.remove(groupIn);
			setDirty();
		}
	}
	
	public void clear()
	{
		allGroups.clear();
		setDirty();
	}
	
	public void tick(MinecraftServer server)
	{
		allGroups.removeIf((group) -> group.shouldNotPersist());
		for(IMobGroup group : allGroups)
		{
			group.tick(server);
			if(group.isDirty())
				setDirty();
		}
	}
	
	public void setDirty()
	{
		super.setDirty();
		needsSync = true;
	}
	
	public boolean needsSync() { return this.needsSync; }
	
	public void syncToClients(MinecraftServer server)
	{
		server.getPlayerList().getPlayers().forEach((player) -> syncToClient(player));
		needsSync = false;
	}
	
	public void syncToClient(ServerPlayer player)
	{
		PacketHandler.sendTo(player, new PacketSyncGroups(this));
	}
	
	public boolean isInAnyGroup(LivingEntity memberIn)
	{
		for(IMobGroup group : allGroups)
			if(group.isMember(memberIn))
				return true;
		return false;
	}
	
	public boolean hasGroup(@Nullable LivingEntity memberIn) { return getGroup(memberIn) != null; }
	
	@Nullable
	public IMobGroup getGroup(@Nullable LivingEntity memberIn)
	{
		if(memberIn == null)
			return null;
		
		if(memberIn instanceof Player)
		{
			Player player = (Player)memberIn;
			List<IMobGroup> groups = GroupPlayer.getGroupsOfPlayer(player);
			if(groups.isEmpty())
				return register(new GroupPlayer((Player)memberIn));
			
			// Return group with closest member
			groups.sort((o1,o2) -> 
			{
				double closest1 = Double.MAX_VALUE;
				double closest2 = Double.MAX_VALUE;
				
				for(LivingEntity member : o1.members())
					if(member.distanceTo(player) < closest1)
						closest1 = member.distanceTo(player);
				
				for(LivingEntity member : o2.members())
					if(member.distanceTo(player) < closest2)
						closest2 = member.distanceTo(player);
				
				return closest1 < closest2 ? 1 : closest1 > closest2 ? -1 : 0;
			});
			return groups.get(0);
		}
		else
			for(IMobGroup group : allGroups)
				if(group.isMember(memberIn))
					return group;
		
		return null;
	}
	
	public IMobGroup getNearest(BlockPos position, double maxDist)
	{
		maxDist *= maxDist;
		double min = Double.MAX_VALUE;
		IMobGroup nearest = null;
		for(IMobGroup group : allGroups)
		{
			Vec3 pos = group.position();
			double dist = position.distToCenterSqr(pos.x, pos.y, pos.z);
			if(dist < maxDist && dist < min)
			{
				min = dist;
				nearest = group;
			}
		}
		return nearest;
	}
	
	public IMobGroup getNearestOfClass(Class<? extends IMobGroup> classIn, BlockPos position, double maxDist)
	{
		maxDist *= maxDist;
		double min = Double.MAX_VALUE;
		IMobGroup nearest = null;
		for(IMobGroup group : allGroups)
		{
			if(group.getClass() != classIn)
				continue;
			
			Vec3 pos = group.position();
			double dist = position.distToCenterSqr(pos.x, pos.y, pos.z);
			if(dist < maxDist && dist < min)
			{
				min = dist;
				nearest = group;
			}
		}
		return nearest;
	}
	
	public boolean exists(Predicate<IMobGroup> predicateIn) { return !getGroups(predicateIn).isEmpty(); }
	
	public List<IMobGroup> getGroups(Predicate<IMobGroup> predicateIn)
	{
		List<IMobGroup> groups = Lists.newArrayList();
		allGroups.forEach((group) -> 
		{
			if(predicateIn.test(group))
				groups.add(group);
		});
		return groups;
	}
}
