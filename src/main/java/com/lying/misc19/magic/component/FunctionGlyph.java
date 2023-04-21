package com.lying.misc19.magic.component;

import com.lying.misc19.Misc19;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;

/** A glyph that performs an actual function based on its inputs and does not have any outputs */
public abstract class FunctionGlyph extends ComponentBase
{
	public Type type() { return Type.GLYPH; }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return false; }
	
	public static class Debug extends FunctionGlyph
	{
		public int castingCost() { return 0; }
		
		public VariableSet execute(VariableSet variablesIn)
		{
			Misc19.LOG.info("# Debug Glyph #");
			Misc19.LOG.info("# Glyphs executed: "+variablesIn.totalGlyphs()+" of "+VariableSet.EXECUTION_LIMIT);
			Misc19.LOG.info("# Casting cost: "+variablesIn.totalCastingCost());
			Misc19.LOG.info("# Register contents:");
			for(Slot slot : VariableSet.Slot.values())
				if(variablesIn.isUsing(slot))
					Misc19.LOG.info("# * "+slot.name()+": "+variablesIn.get(slot).toString());
			Misc19.LOG.info("# Debug End #");
			return variablesIn;
		}
	}
}
