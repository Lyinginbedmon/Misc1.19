package com.lying.misc19.magic;

import com.lying.misc19.magic.component.ComponentBase;
import com.lying.misc19.magic.variable.VariableSet;

public abstract class ComponentGlyph extends ComponentBase
{
	public Type type() { return Type.GLYPH; }
	
	public static class Dummy extends ComponentGlyph
	{
		public VariableSet execute(VariableSet variablesIn) { return variablesIn; }
		
		public int castingCost() { return 0; }
	}
}
