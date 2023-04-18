package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VariableSet;

public class ConditionGlyph extends ComponentBase
{
	public Type type() { return Type.GLYPH; }
	
	public boolean isValidInput(ISpellComponent component) { return component.type() == Type.VARIABLE && this.inputGlyphs.isEmpty(); }
	
	public boolean isValidOutput(ISpellComponent component) { return component.type() != Type.VARIABLE && this.outputGlyphs.isEmpty(); }
	
	public VariableSet execute(VariableSet variablesIn)
	{
		if(inputGlyphs.isEmpty() || outputGlyphs.isEmpty() || !getVariable(0, variablesIn).asBoolean())
			return variablesIn;
		return outputGlyphs.get(0).execute(variablesIn);
	}
}
