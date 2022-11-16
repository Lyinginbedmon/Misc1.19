package com.example.examplemod.entity.ai.group.action;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ai.group.action.GroupAction.Status;
import com.example.examplemod.reference.Reference;
import com.google.common.base.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class ActionType
{
    private static final ConcurrentHashMap<ResourceLocation, Supplier<GroupAction>> ACTION_TYPES = new ConcurrentHashMap<>();
    
    public static final ResourceLocation AGGRO_FLANK	= register("aggro_flank", () -> { return new GroupAction.ActionAggroFlank(); });
    public static final ResourceLocation BRAWL			= register("brawl", () -> { return new GroupAction.ActionBrawl(); });
    public static final ResourceLocation FLANK			= register("flank", () -> { return new GroupAction.ActionFlank(); });
    public static final ResourceLocation FOLLOW			= register("follow", () -> { return new GroupAction.ActionFollow(null, 3D, 10D); });
    public static final ResourceLocation GRID			= register("grid", () -> { return new GroupAction.ActionGrid(BlockPos.ZERO, 2D); });
    public static final ResourceLocation GUARD_POS		= register("guard_pos", () -> { return new GroupAction.ActionGuardPos(BlockPos.ZERO, 3D, 10D); });
    public static final ResourceLocation GUARD_MOB		= register("guard_mob", () -> { return new GroupAction.ActionGuardMob(null, 3D, 10D); });
    public static final ResourceLocation QUARRY			= register("quarry", () -> { return new GroupAction.ActionQuarry(BlockPos.ZERO, BlockPos.ZERO, Direction.NORTH); });
    public static final ResourceLocation PICK_UP		= register("pick_up", () -> { return new GroupAction.ActionPickUp(BlockPos.ZERO, BlockPos.ZERO); });
    public static final ResourceLocation PICK_UP_NON_SEEDS	= register("pick_up_non_seeds", () -> { return new GroupAction.ActionPickUpNonSeeds(BlockPos.ZERO, BlockPos.ZERO); });
    public static final ResourceLocation GENERIC		= register("generic", () -> new GroupAction.ActionGeneric());
    public static final ResourceLocation FARM			= register("farm", () -> new GroupAction.ActionFarm(BlockPos.ZERO, BlockPos.ZERO));
    
    public static ResourceLocation register(String nameIn, Supplier<GroupAction> factory)
    {
    	ResourceLocation registryName = new ResourceLocation(Reference.ModInfo.MOD_ID, nameIn);
    	ACTION_TYPES.put(registryName, factory);
    	return registryName;
    }
    
    public static void init() { }
    
    @Nullable
    public static GroupAction createActionFromNbt(@Nullable ResourceLocation key, CompoundTag compound)
    {
    	if(key != null && ACTION_TYPES.containsKey(key))
    	{
    		Supplier<GroupAction> supplier = ACTION_TYPES.get(key);
    		GroupAction action = supplier.get();
    		action.setComplement(compound.getInt("Complement"));
    		action.setStatus(Status.fromName(compound.getString("Status")));
    		ListTag childData = compound.getList("Children", Tag.TAG_COMPOUND);
    		for(int i=0; i<childData.size(); i++)
    		{
    			CompoundTag comp = childData.getCompound(i);
    			GroupAction child = createActionFromNbt(new ResourceLocation(comp.getString("Type")), comp);
    			if(child != null)
    				action.addChild(child, false);
    		};
    		action.setMaxChildren(compound.getInt("ChildLimit"));
    		
    		action.loadFromNbt(compound.getCompound("Data"));
    		return action;
    	}
    	
    	return null;
    }
}
