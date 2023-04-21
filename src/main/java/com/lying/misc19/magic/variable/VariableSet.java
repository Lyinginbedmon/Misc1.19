package com.lying.misc19.magic.variable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.lying.misc19.init.Components;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.component.VariableGlyph;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VariableSet
{
	public static final IVariable DEFAULT = new VarBool(false);
	public static int EXECUTION_LIMIT = 128;
	
	private int glyphsExecuted = 0;
	private int accruedCost = 0;
	
	private Map<Slot, IVariable> values = new HashMap<>();
	
	public boolean isUsing(Slot name) { return values.containsKey(name); }
	
	public IVariable get(Slot name) { return values.getOrDefault(name, DEFAULT); }
	public VariableSet set(Slot name, @Nullable IVariable value)
	{
		this.values.put(name, value);
		return this;
	}
	
	public void setDouble(Slot name, double value) { values.put(name, new VarDouble(value)); }
	public double getDouble(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null  ? var.asDouble() : 0D;
	}
	
	public void setVector(Slot name, Vec3 vector) { values.put(name, new VarVec(vector)); }
	public Vec3 getVector(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null ? var.asVec() : Vec3.ZERO;
	}
	
	public void setEntity(Slot name, Entity entity) { values.put(name, new VarEntity(entity)); }
	public Entity getEntity(Slot name)
	{
		IVariable var = values.getOrDefault(name, null);
		return var != null ? var.asEntity() : null;	// FIXME Ensure a value to prevent null pointer crashes
	}
	
	public boolean executionLimited() { return this.glyphsExecuted > EXECUTION_LIMIT; }
	public VariableSet glyphExecuted(int costIn) { this.glyphsExecuted++; this.accruedCost += costIn; return this; }
	public int totalGlyphs() { return this.glyphsExecuted; }
	public void resetExecutions()
	{
		this.glyphsExecuted = 0;
		this.accruedCost = 0;
	}
	
	public int totalCastingCost() { return this.accruedCost; }
	
	public static enum VariableType
	{
		DOUBLE,
		VECTOR,
		ENTITY,
		STACK;
	}
	
	public static enum Slot
	{
		/** Index is a special variable used by circles, containing the execution index */
		INDEX(true),
		/** World is the level the spell is executing in and is always present */
		WORLD(true),	// TODO Implement World variables
		/** Caster contains the LivingEntity that originally cast the spell and is always present */
		CASTER(true),
		/** Position contains the location the spell is working from, usually the Caster's eye position */
		POSITION(true),
		/** Target contains the Entity the Caster was looking at, if any */
		TARGET(true),
		/** Look contains the vector from the Caster's eye position */
		LOOK(true),
		THOTH,
		BAST,
		ANUBIS,
		SUTEKH,
		OSIRIS,
		ISIS,
		HORUS,
		RA;
		
		private boolean readOnly;
		
		private Slot(boolean assignIn)
		{
			readOnly = assignIn;
		}
		private Slot() { this(false); }
		
		public ResourceLocation glyph() { return Components.make(name().toLowerCase()+"_glyph"); }
		
		public boolean isPlayerAssignable() { return !this.readOnly; }
		
		public static ISpellComponent makeGlyph(Slot slotIn) { return new VariableGlyph.Local(slotIn); }
	}
}
