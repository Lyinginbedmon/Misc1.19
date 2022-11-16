package com.example.examplemod.entity.ai;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.utility.MobCommanding.Mark;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class MobCommand
{
	public final Mark type;
	private Object[] variables;
	
	public MobCommand(Mark typeIn, Object... variablesIn)
	{
		this.type = typeIn;
		this.variables = variablesIn;
	}
	
	public Mark type() { return this.type; }
	
	public Object variable(int index) { return this.variables[index]; }
	public int variables() { return this.variables.length; }
	
	public CompoundTag saveToNBT(CompoundTag compound)
	{
		compound.putString("Type", type.getSerializedName());
		
		ListTag variableData = new ListTag();
		for(Object obj : variables)
		{
			if(obj instanceof Entity)
				variableData.add(Utils.storeVariable((Entity)obj));
			else if(obj instanceof BlockPos)
				variableData.add(Utils.storeVariable((BlockPos)obj));
			else if(obj instanceof Direction)
				variableData.add(Utils.storeVariable((Direction)obj));
		}
		compound.put("Variables", variableData);
		
		return compound;
	}
	
	@Nullable
	public static MobCommand loadFromNBT(CompoundTag compound, double searchRange, Level world)
	{
		Mark type = Mark.fromName(compound.getString("Type"));
		
		ListTag variableData = compound.getList("Variables", Tag.TAG_COMPOUND);
		Object[] variables = new Object[variableData.size()];
		for(int i=0; i<variableData.size(); i++)
			variables[i] = Utils.loadVariable(variableData.getCompound(i), searchRange, world);
		
		return type.makeCommand(variables);
	}
	
	public static class Utils
	{
		private static CompoundTag storage(Types type)
		{
			CompoundTag data = new CompoundTag();
			data.putString("Type", type.getSerializedName());
			return data;
		}
		
		public static CompoundTag storeVariable(BlockPos pos)
		{
			CompoundTag data = storage(Types.POS);
			data.put("Pos", NbtUtils.writeBlockPos(pos));
			return data;
		}
		
		public static CompoundTag storeVariable(Entity ent)
		{
			CompoundTag data = storage(Types.ENT);
			data.putUUID("UUID", ent.getUUID());
			data.put("Pos", NbtUtils.writeBlockPos(ent.blockPosition()));
			return data;
		}
		
		public static CompoundTag storeVariable(Direction num)
		{
			CompoundTag data = storage(Types.DIR);
			data.putInt("Index", num.ordinal());
			return data;
		}
		
		public static Object loadVariable(CompoundTag data, double searchRange, Level world)
		{
			switch(Types.fromName(data.getString("Type")))
			{
				case DIR:
					return Direction.values()[data.getInt("Index")];
				case POS:
					return NbtUtils.readBlockPos(data.getCompound("Pos"));
				case ENT:
					return findEntityOfUUID(data.getUUID("UUID"), NbtUtils.readBlockPos(data.getCompound("Pos")), searchRange, world);
				default:
					break;
			}
			return null;
		}
		
		public static Entity findEntityOfUUID(UUID entityID, BlockPos position, double range, Level world)
		{
			AABB bounds = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).move(position).inflate(range);
			List<Entity> prospects = world.getEntities((Entity)null, bounds, (entity) -> { return !entity.isRemoved() && entity.getUUID().equals(entityID); });
			if(prospects.size() > 1)
				ExampleMod.LOG.warn("Multiple targets found for mob command, this shouldn't be possible...");
			return prospects.isEmpty() ? null : prospects.get(0);
		}
		
		private static enum Types implements StringRepresentable
		{
			POS,
			ENT,
			DIR;
			
			public String getSerializedName(){ return name().toLowerCase(); }
			
			public static Types fromName(String nameIn)
			{
				for(Types type : values())
					if(type.getSerializedName().equals(nameIn))
						return type;
				return null;
			}
		}
	}
}