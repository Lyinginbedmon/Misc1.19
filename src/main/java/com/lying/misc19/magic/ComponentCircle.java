package com.lying.misc19.magic;

import com.lying.misc19.magic.component.ComponentBase;
import com.lying.misc19.magic.variable.VarDouble;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.utility.M19Utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;

public abstract class ComponentCircle extends ComponentBase
{
	public Category category() { return Category.CIRCLE; }
	
	public Type type() { return Type.CIRCLE; }
	
	public boolean isValidInput(ISpellComponent component) { return ISpellComponent.canBeInput(component) && this.inputGlyphs.isEmpty(); }
	
	public boolean isValidOutput(ISpellComponent component) { return component.type() == Type.GLYPH && this.outputGlyphs.size() < 8; }
	
	public void organise()
	{
		float spin = 180F / inputGlyphs.size();
		Vec2 start = M19Utils.rotate(new Vec2(-20, 0), spin / 2);
		for(ISpellComponent input : inputGlyphs)
		{
			input.setParent(this);
			input.setPosition(start.x, start.y);
			input.organise();
			
			start = M19Utils.rotate(start, spin);
		}
		
		spin = 360F / outputGlyphs.size();
		start = new Vec2(0, -100);
		for(ISpellComponent output : outputGlyphs)
		{
			output.setParent(this);
			output.setPosition(start.x, start.y);
			output.organise();
			
			start = M19Utils.rotate(start, spin);
		}
	}
	
	/** Returns how many times this circle should cycle in this execution */
	public int calculateRuns(VariableSet variablesIn)
	{
		if(!inputs().isEmpty())
			return (int)Math.max(getVariable(0, variablesIn).asDouble(), 0);
		
		return 1;
	}
	
	/** Performs circle execution logic once */
	protected abstract VariableSet doRun(VariableSet variablesIn);
	
	public VariableSet execute(VariableSet variablesIn)
	{
		for(int i=0; i<calculateRuns(variablesIn); i++)
			if(!variablesIn.executionLimited())
				variablesIn = doRun(variablesIn.set(Slot.INDEX, new VarDouble(i)));
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
