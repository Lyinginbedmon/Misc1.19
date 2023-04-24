package com.lying.misc19.magic.variable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.init.SpellVariables;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.component.VariableGlyph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

public class VariableSet
{
	public static final IVariable DEFAULT = new VarBool(false);
	public static int EXECUTION_LIMIT = 128;
	
	private int glyphsExecuted = 0;
	private int accruedCost = 0;
	
	private Map<Slot, IVariable> values = new HashMap<>();
	
	public VariableSet()
	{
		values.put(Slot.AGE, new VarDouble(0D));
	}
	
	public boolean isUsing(Slot name) { return values.containsKey(name) && !values.get(name).equals(DEFAULT); }
	
	public IVariable get(Slot name) { return values.getOrDefault(name, DEFAULT); }
	public VariableSet set(Slot name, @Nullable IVariable value)
	{
		this.values.put(name, value);
		return this;
	}
	
	public void recacheBeforeExecution(Level worldIn)
	{
		for(IVariable var : values.values())
			var.recache(worldIn);
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
	
	public CompoundTag writeToNBT(CompoundTag compound)
	{
		ListTag vars = new ListTag();
		for(Slot slot : Slot.values())
			if(isUsing(slot))
			{
				CompoundTag data = new CompoundTag();
				data.putString("Slot", slot.getSerializedName());
				vars.add(IVariable.saveToNBT(get(slot), data));
			}
		if(!vars.isEmpty())
			compound.put("Variables", vars);
		return compound;
	}
	
	public static VariableSet readFromNBT(CompoundTag compound)
	{
		VariableSet variables = new VariableSet();
		
		if(compound.contains("Variables", Tag.TAG_LIST))
		{
			ListTag vars = compound.getList("Variables", Tag.TAG_COMPOUND);
			for(int i=0; i<vars.size(); i++)
			{
				CompoundTag data = vars.getCompound(i);
				Slot slot = Slot.byName(data.getString("Slot"));
				if(slot == null)
					continue;
				
				variables.values.put(slot, SpellVariables.readFromNbt(data));
			}
		}
		
		return variables;
	}
	
	public static enum VariableType
	{
		DOUBLE,
		VECTOR,
		ENTITY,
		STACK,
		WORLD;
	}
	
	public static enum Slot implements StringRepresentable
	{
		/** Age represents the number of times a given spell has executed thus far.<br>Always present */
		AGE(true),
		/** World is the level the spell is executing in.<br>Always present */
		WORLD(true),
		/** Caster contains the LivingEntity that originally cast the spell.<br>Always present */
		CASTER(true),
		/** Position contains the location the spell is working from, usually the Caster's eye position */
		POSITION(true),
		/** Target contains the Entity the Caster was looking at, if any */
		TARGET(true),
		/** Look contains the vector from the Caster's eye position */
		LOOK(true),
		/** Index is a special variable used by circles, containing the execution index */
		INDEX(true),
		/**
		 * TRUE if this spell should continue after the current execution.<br>
		 * By defaulting this to FALSE, we treat all spells as single-run by default.
		 */
		CONTINUE,
		ANUBIS,
		APEP,
		BAST,
		BES,
		AMUN,
		HATHOR,
		HORUS,
		ISIS,
		NEPTHYS,
		OSIRIS,
		PTAH,
		RA,
		SOBEK,
		SUTEKH,
		TAWARET,
		THOTH;
		
		private boolean readOnly;
		
		private Slot(boolean assignIn)
		{
			readOnly = assignIn;
		}
		private Slot() { this(false); }
		
		public ResourceLocation glyph() { return SpellComponents.make(name().toLowerCase()+"_glyph"); }
		
		public boolean isPlayerAssignable() { return !this.readOnly; }
		
		public static ISpellComponent makeGlyph(Slot slotIn) { return new VariableGlyph.Local(slotIn); }
		
		public String getSerializedName() { return name().toLowerCase(); }
		
		@Nullable
		public static Slot byName(String nameIn)
		{
			for(Slot slot : values())
				if(slot.getSerializedName().equalsIgnoreCase(nameIn))
					return slot;
			return null;
		}
	}
}
