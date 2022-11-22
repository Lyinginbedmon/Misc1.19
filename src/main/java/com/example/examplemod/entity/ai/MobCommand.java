package com.example.examplemod.entity.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.utility.MobCommanding.Mark;
import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MobCommand
{
	private Level world = null;
	private double searchRange = 0D;
	
	public final Mark type;
	
	private Map<String, CachableVariable> variableMap = new HashMap<>();
	
	public MobCommand(Mark typeIn, Map<String, Object> variablesIn)
	{
		this.type = typeIn;
		
		if(variablesIn != null && !variablesIn.isEmpty())
			for(String name : variablesIn.keySet())
				variableMap.put(name, new CachableVariable(variablesIn.get(name)));
	}
	
	public MobCommand(Mark typeIn, Player playerIn)
	{
		this(typeIn, (Map<String, Object>)null);
		variableMap.put("Entity", new CachableVariable(playerIn));
	}
	
	protected MobCommand bindToWorld(Level worldIn, double rangeIn)
	{
		this.world = worldIn;
		this.searchRange = rangeIn;
		return this;
	}
	
	public Mark type() { return this.type; }
	
	@Nullable
	public Object variable(String nameIn) { return hasVariable(nameIn) ? variableMap.get(nameIn).get(world, searchRange) : null; }
	public boolean hasVariable(String nameIn) { return variableMap.containsKey(nameIn); }
	public int variables() { return this.variableMap.size(); }
	
	public CompoundTag saveToNBT(CompoundTag compound)
	{
		compound.putString("Type", type.getSerializedName());
		
		ListTag variableData = new ListTag();
		for(String name : variableMap.keySet())
		{
			CompoundTag data = new CompoundTag();
			data.putString("Name", name);
			data.put("Data", variableMap.get(name).store());
			variableData.add(data);
		}
		compound.put("Variables", variableData);
		
		return compound;
	}
	
	@Nullable
	public static MobCommand loadFromNBT(CompoundTag compound, double searchRange, Level world)
	{
		Mark type = Mark.fromName(compound.getString("Type"));
		
		Map<String, CachableVariable> map = new HashMap<>();
		ListTag variableData = compound.getList("Variables", Tag.TAG_COMPOUND);
		for(int i=0; i<variableData.size(); i++)
		{
			CompoundTag data = variableData.getCompound(i);
			String varName = data.getString("Name");
			CompoundTag varData = data.getCompound("Data");
			Types varType = Types.fromName(varData.getString("Type"));
			if(varType == null)
				continue;
			map.put(varName, new CachableVariable(varType, varData));
		}
		
		MobCommand command = type.makeCommand(new HashMap<String,Object>()).bindToWorld(world, searchRange);
		command.variableMap = map;
		return command;
	}
	
	public static Entity findEntityOfUUID(UUID entityID, BlockPos position, double range, Level world)
	{
		AABB bounds = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).move(position).inflate(range);
		List<Entity> prospects = world.getEntities((Entity)null, bounds, (entity) -> { return !entity.isRemoved() && entity.getUUID().equals(entityID); });
		if(prospects.size() > 1)
			ExampleMod.LOG.warn("Multiple targets found for entity variable, this shouldn't be possible...");
		return prospects.isEmpty() ? null : prospects.get(0);
	}
	
	public static enum Types implements StringRepresentable
	{
		POSITION(
				(obj) -> obj instanceof BlockPos,
				(obj, data) -> data.put("Pos", NbtUtils.writeBlockPos((BlockPos)obj)),
				(data, range, world) -> NbtUtils.readBlockPos(data.getCompound("Pos"))),
		ENTITY(
				(obj) -> obj instanceof Entity,
				(obj, data) ->
				{
					Entity entity = (Entity)obj;
					data.putUUID("UUID", entity.getUUID());
					data.put("Pos", NbtUtils.writeBlockPos(entity.blockPosition()));
				},
				(data, range, world) -> findEntityOfUUID(data.getUUID("UUID"), NbtUtils.readBlockPos(data.getCompound("Pos")), range, world)),
		DIRECTION(
				(obj) -> obj instanceof Direction,
				(obj, data) -> data.putString("Facing", ((Direction)obj).getSerializedName()),
				(data, range, world) -> Direction.byName(data.getString("Facing"))),
		VECTOR(
				(obj) -> obj instanceof Vec3,
				(obj, data) ->
				{
					Vec3 vec = (Vec3)obj;
					data.putDouble("X", vec.x);
					data.putDouble("Y", vec.y);
					data.putDouble("Z", vec.z);
				},
				(data, range, world) -> new Vec3(data.getDouble("X"), data.getDouble("Y"), data.getDouble("Z"))),
		@SuppressWarnings("deprecation")
		BLOCK(
				(obj) -> obj instanceof Block,
				(obj, data) -> data.putString("Block", ((Block)obj).toString()),
				(data, range, world) -> Registry.BLOCK.get(new ResourceLocation(data.getString("Block"))));
		
		private final Predicate<Object> predicate;
		private final VariableStore encrypter;
		private final VariableLoad decrypter;
		
		private Types(Predicate<Object> predIn, VariableStore encryptIn, VariableLoad decryptIn)
		{
			this.predicate = predIn;
			this.encrypter = encryptIn;
			this.decrypter = decryptIn;
		}
		
		public String getSerializedName(){ return name().toLowerCase(); }
		
		public static Types fromName(String nameIn)
		{
			for(Types type : values())
				if(type.getSerializedName().equals(nameIn))
					return type;
			return null;
		}
		
		public static Types fromObj(Object obj)
		{
			for(Types type : values())
				if(type.matches(obj))
					return type;
			return null;
		}
		
		public boolean matches(Object obj) { return predicate.apply(obj); }
		
		public CompoundTag store(Object obj)
		{
			CompoundTag data = new CompoundTag();
			data.putString("Type", getSerializedName());
			this.encrypter.store(obj, data);
			return data;
		}
		
		@FunctionalInterface
		private interface VariableStore
		{
			public void store(Object obj, CompoundTag data);
		}
		
		@FunctionalInterface
		private interface VariableLoad
		{
			public Object load(CompoundTag data, double searchRange, Level world);
		}
	}
	
	private static class CachableVariable
	{
		private final Types type;
		private final CompoundTag data;
		
		private Object cached = null;
		
		public CachableVariable(Types typeIn, CompoundTag dataIn)
		{
			this.type = typeIn;
			this.data = dataIn;
		}
		
		public CachableVariable(Object objIn)
		{
			this.cached = objIn;
			
			this.type = Types.fromObj(objIn);
			this.data = new CompoundTag();
			this.type.encrypter.store(cached, this.data);
		}
		
		public boolean cached() { return this.cached != null; }
		
		public Object get(Level world, double searchRange)
		{
			if(cached == null)
			{
				try
				{
					this.cached = this.type.decrypter.load(data, searchRange, world);
				}
				catch(Exception e) { }
			}
			return cached;
		}
		
		public Tag store()
		{
			if(cached())
				return this.type.store(cached);
			else
			{
				this.data.putString("Type", this.type.getSerializedName());
				return this.data;
			}
			
		}
	}
}