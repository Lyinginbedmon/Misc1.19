package com.lying.misc19.magic;

import com.lying.misc19.magic.component.ComponentBase;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;

import net.minecraft.nbt.CompoundTag;

public abstract class ComponentCircle extends ComponentBase
{
	public Category category() { return Category.CIRCLE; }
	
	public Type type() { return Type.CIRCLE; }
	
	public boolean isValidInput(ISpellComponent component) { return ISpellComponent.canBeInput(component) && this.inputGlyphs.isEmpty(); }
	
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
			if(!variablesIn.executionLimited())
				variablesIn = doRun(variablesIn.set(Slot.INDEX, new com.lying.misc19.magic.variable.VarDouble(i)));
		return variablesIn.set(Slot.INDEX, VariableSet.DEFAULT);
	}
	
	/** Sequentially executes all child glyphs per execution call */
	public static class Basic extends ComponentCircle
	{
		public VariableSet doRun(VariableSet variablesIn)
		{
			for(ISpellComponent child : this.outputs())
				variablesIn = child.execute(variablesIn).glyphExecuted(child.castingCost());
			return variablesIn;
		}
	}
	
	/** Executes only one glyph per call, useful for high-cost arrangements */
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
			if(variablesIn.executionLimited())
				return variablesIn;
			ISpellComponent current = outputs().get(index++ % outputs().size());
			return current.execute(variablesIn).glyphExecuted(current.castingCost());
		}
		
		public void serialiseNBT(CompoundTag nbt)
		{
			if(this.index % outputs().size() > 0)
				nbt.putInt("Index", this.index % outputs().size());
		}
		
		public void deserialiseNBT(CompoundTag nbt) { this.index = nbt.getInt("Index"); }
	}
}
