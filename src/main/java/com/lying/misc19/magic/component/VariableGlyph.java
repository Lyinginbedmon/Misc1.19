package com.lying.misc19.magic.component;

import javax.annotation.Nullable;

import com.lying.misc19.magic.ComponentGlyph;
import com.lying.misc19.magic.variable.Double;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.Vec;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public abstract class VariableGlyph extends ComponentGlyph
{
	public Type type() { return Type.VARIABLE; }
	
	public abstract IVariable get(VariableSet variablesIn);
	
	public abstract VariableSet set(VariableSet variablesIn, @Nullable IVariable value);
	
	public VariableSet execute(VariableSet variablesIn) { return variablesIn; }
	
	/** Constant variables store useful values and cannot be assigned */
	public static class Constant extends VariableGlyph
	{
		private final IVariable value;
		
		public Constant(IVariable varIn) { this.value = varIn; }
		
		public IVariable get(VariableSet variablesIn) { return this.value; }
		
		public VariableSet set(VariableSet variablesIn, @Nullable IVariable value) { return variablesIn; }
		
		public static Constant doubleConst(double varIn) { return new Constant(new Double(varIn)); }
		public static Constant dirConst(Direction varIn) { return new Constant(new Vec(new Vec3(varIn.getNormal().getX(), varIn.getNormal().getY(), varIn.getNormal().getZ()))); }
		public static Constant vecConst(Vec3 varIn) { return new Constant(new Vec(varIn)); }
	}
	
	/** Local variables can be set and retrieved by arrangements */
	public static class Local extends VariableGlyph
	{
		private final VariableSet.Slot slot;
		
		public Local(VariableSet.Slot slotIn) { this.slot = slotIn; }
		
		public IVariable get(VariableSet variablesIn) { return variablesIn.get(this.slot); }
		
		public VariableSet set(VariableSet variablesIn, @Nullable IVariable value)
		{
			variablesIn.set(this.slot, value);
			return variablesIn;
		}
	}
	
	/** Index is a special variable used by circles that cannot be assigned by arrangements */
	public static class Index extends Local
	{
		public Index() { super(VariableSet.Slot.INDEX); }
		
		public VariableSet set(VariableSet variablesIn, @Nullable IVariable value) { return variablesIn; }
	}
}
