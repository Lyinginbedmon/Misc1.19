package com.example.examplemod.entity.ai.group;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.example.examplemod.reference.Reference;
import com.google.common.base.Supplier;

import net.minecraft.resources.ResourceLocation;

public class GroupType
{
    private static final ConcurrentHashMap<ResourceLocation, Supplier<IGroupFactory>> GROUP_TYPES = new ConcurrentHashMap<>();
    
    public static final ResourceLocation PLAYER = register("player", GroupPlayer.Factory::new);
    public static final ResourceLocation GENERIC = register("generic", GroupGeneric.Factory::new);
    public static final ResourceLocation STEVE = register("steve", GroupSteve.Factory::new);
    
    public static ResourceLocation register(String nameIn, Supplier<IGroupFactory> factory)
    {
    	ResourceLocation registryName = new ResourceLocation(Reference.ModInfo.MOD_ID, nameIn);
    	GROUP_TYPES.put(registryName, factory);
    	return registryName;
    }
    
    public static void init() { }
    
    @Nullable
    public static IMobGroup createGroupFromName(ResourceLocation key)
    {
    	if(key != null)
	    	for(Entry<ResourceLocation, Supplier<IGroupFactory>> entry : GROUP_TYPES.entrySet())
	    		if(entry.getKey().equals(key))
	    			return entry.getValue().get().create();
    	
    	return null;
    }
}
