package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;

public abstract class OperationGlyph extends ComponentBase
{
	public Type type() { return Type.GLYPH; }
	
	public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE; }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE; }
	
	public static class Set extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().isEmpty(); }
		
		public VariableSet execute(VariableSet variablesIn)
		{
			return setOutputs(variablesIn, getVariable(0, variablesIn));
		}
	}
	
	public static class Add extends OperationGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			IVariable value = VariableSet.DEFAULT;
			for(int i=0; i<inputs().size(); i++)
			{
				IVariable variable = getVariable(i, variablesIn);
				if(i == 0)
					value = variable;
				else
					value = value.add(variable);
			}
			
			return setOutputs(variablesIn, value);
		}
	}
	
	public static class Subtract extends OperationGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			IVariable value = VariableSet.DEFAULT;
			for(int i=0; i<inputs().size(); i++)
			{
				IVariable variable = getVariable(i, variablesIn);
				if(i == 0)
					value = variable;
				else
					value = value.subtract(variable);
			}
			
			return setOutputs(variablesIn, value);
		}
	}
	
	public static class Multiply extends OperationGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			IVariable value = VariableSet.DEFAULT;
			for(int i=0; i<inputs().size(); i++)
			{
				IVariable variable = getVariable(i, variablesIn);
				if(i == 0)
					value = variable;
				else
					value = value.multiply(variable);
			}
			
			return setOutputs(variablesIn, value);
		}
	}
	
	public static class Divide extends OperationGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			IVariable value = VariableSet.DEFAULT;
			for(int i=0; i<inputs().size(); i++)
			{
				IVariable variable = getVariable(i, variablesIn);
				if(i == 0)
					value = variable;
				else
					value = value.divide(variable);
			}
			
			return setOutputs(variablesIn, value);
		}
	}
}