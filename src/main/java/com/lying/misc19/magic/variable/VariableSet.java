package com.lying.misc19.magic.variable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.lying.misc19.init.Components;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VariableSet
{
	public static final IVariable DEFAULT = new Bool(false);
	
	private Map<Slot, IVariable> values = new HashMap<>();
	
	public IVariable get(Slot name) { return values.getOrDefault(name, DEFAULT); }
	public VariableSet set(Slot name, @Nullable IVariable value)
	{
		this.values.put(name, value);
		return this;
	}
	
	public void setDouble(Slot name, double value) { values.put(name, new Double(value)); }
	public double getDouble(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null  ? var.asDouble() : 0D;
	}
	
	public void setVector(Slot name, Vec3 vector) { values.put(name, new Vec(vector)); }
	public Vec3 getVector(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null ? var.asVec() : Vec3.ZERO;
	}
	
	public void setEntity(Slot name, Entity entity) { values.put(name, new Ent(entity)); }
	public Entity getEntity(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null ? var.asEntity() : null;	// FIXME Ensure a value to prevent null pointer crashes
	}
	
	public static enum VariableType
	{
		DOUBLE,
		VECTOR,
		ENTITY,
		STACK;
	}
	
	public static enum Slot
	{
		INDEX(false),
		THOTH,
		BAST,
		ANUBIS,
		SUTEKH,
		OSIRIS,
		ISIS,
		HORUS,
		RA;
		
		private boolean assignable;
		
		private Slot(boolean assignIn)
		{
			assignable = assignIn;
		}
		private Slot() { this(true); }
		
		public ResourceLocation glyph() { return Components.make(name().toLowerCase()+"_glyph"); }
		
		public boolean isPlayerAssignable() { return this.assignable; }
	}
}
