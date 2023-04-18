package com.lying.misc19.magic;

import com.lying.misc19.magic.component.ComponentBase;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;

import net.minecraft.nbt.CompoundTag;

public abstract class ComponentCircle extends ComponentBase
{
	public Type type() { return Type.CIRCLE; }
	
	public boolean isValidInput(ISpellComponent component) { return component.type() == Type.VARIABLE && this.inputGlyphs.isEmpty(); }
	
	public boolean isValidOutput(ISpellComponent component) { return component.type() == Type.GLYPH && this.outputGlyphs.size() < 8; }
	
	public int calculateRuns(VariableSet variablesIn)
	{
		if(!inputs().isEmpty())
			return (int)Math.max(getVariable(0, variablesIn).asDouble(), 0);
		
		return 1;
	}
	
	protected abstract VariableSet doRun(VariableSet variablesIn);
	
	public VariableSet execute(VariableSet variablesIn)
	{
		for(int i=0; i<calculateRuns(variablesIn); i++)
			variablesIn = doRun(variablesIn.set(Slot.INDEX, new com.lying.misc19.magic.variable.Double(i)));
		return variablesIn.set(Slot.INDEX, VariableSet.DEFAULT);
	}
	
	public static class Basic extends ComponentCircle
	{
		public VariableSet doRun(VariableSet variablesIn)
		{
			for(ISpellComponent child : this.outputs())
				variablesIn = child.execute(variablesIn);
			return variablesIn;
		}
	}
	
	public static class Step extends ComponentCircle
	{
		private int index = 0;
		
		public VariableSet execute(VariableSet variablesIn)
		{
			if(calculateRuns(variablesIn) == 0)
			{
				this.index = 0;
				return variablesIn;
			}
			else
				return super.execute(variablesIn);
		}
		
		public VariableSet doRun(VariableSet variablesIn)
		{
			return outputs().get(index++ % outputs().size()).execute(variablesIn);
		}
		
		public void serialiseNBT(CompoundTag nbt)
		{
			if(this.index % outputs().size() > 0)
				nbt.putInt("Index", this.index % outputs().size());
		}
		
		public void deserialiseNBT(CompoundTag nbt) { this.index = nbt.getInt("Index"); }
	}
}
