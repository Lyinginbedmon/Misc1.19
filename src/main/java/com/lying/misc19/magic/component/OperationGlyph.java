package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;

public abstract class OperationGlyph extends ComponentBase
{
	public Category category() { return Category.OPERATION; }
	
	public Type type() { return Type.GLYPH; }
	
	public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn); }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE; }
	
	/** Returns the product of this operation, without setting outputs */
	public abstract IVariable getResult(VariableSet variablesIn);
	
	public VariableSet execute(VariableSet variablesIn)
	{
		return setOutputs(variablesIn, getResult(variablesIn));
	}
	
	public static class Set extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return super.isValidInput(componentIn) && inputs().isEmpty(); }
		
		public IVariable getResult(VariableSet variablesIn) { return getVariable(0, variablesIn); }
	}
	
	public static class Add extends OperationGlyph
	{
		public IVariable getResult(VariableSet variablesIn)
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
			return value;
		}
	}
	
	public static class Subtract extends OperationGlyph
	{
		public IVariable getResult(VariableSet variablesIn)
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
			
			return value;
		}
	}
	
	public static class Multiply extends OperationGlyph
	{
		public IVariable getResult(VariableSet variablesIn)
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
			
			return value;
		}
	}
	
	public static class Divide extends OperationGlyph
	{
		public IVariable getResult(VariableSet variablesIn)
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
			
			return value;
		}
	}
}