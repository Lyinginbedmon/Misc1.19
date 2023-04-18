package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VarBool;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;

public abstract class ComparisonGlyph extends ComponentBase
{
	public Type type() { return Type.GLYPH; }
	
	public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE; }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE; }
	
	/** Outputs 1 if all input values are of equal value */
	public static class Equals extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			if(inputs().size() > 1)
			{
				IVariable target = getVariable(0, variablesIn);
				for(int i=1; i<inputs().size(); i++)
				{
					IVariable value = getVariable(i, variablesIn);
					if(!value.equals(target))
						return setOutputs(variablesIn, VarBool.FALSE);
				}
			}
			return setOutputs(variablesIn, VarBool.TRUE);
		}
	}
	
	/** Outputs 1 if all double input values are greater than their preceding input value */
	public static class Greater extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			if(inputs().size() > 1)
			{
				IVariable target = getVariable(0, variablesIn);
				for(int i=1; i<inputs().size(); i++)
				{
					IVariable value = getVariable(i, variablesIn);
					if(!value.greater(target))
						return setOutputs(variablesIn, VarBool.FALSE);
					target = value;
				}
			}
			
			return setOutputs(variablesIn, VarBool.TRUE);
		}
	}
	
	/** Outputs 1 if all double input values are less than their preceding input value */
	public static class Less extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			if(inputs().size() > 1)
			{
				IVariable target = getVariable(0, variablesIn);
				for(int i=1; i<inputs().size(); i++)
				{
					IVariable value = getVariable(i, variablesIn);
					if(!value.less(target))
						return setOutputs(variablesIn, VarBool.FALSE);
					target = value;
				}
			}
			
			return setOutputs(variablesIn, VarBool.TRUE);
		}
	}
	
	/** Outputs 1 if all input values are true */
	public static class And extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			for(int i=0; i<inputs().size(); i++)
				if(!getVariable(i, variablesIn).asBoolean())
					return setOutputs(variablesIn, VarBool.FALSE);
			return setOutputs(variablesIn, VarBool.TRUE);
		}
	}
	
	/** Outputs 1 if all input values are false */
	public static class NAnd extends And
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			for(int i=0; i<inputs().size(); i++)
				if(getVariable(i, variablesIn).asBoolean())
					return setOutputs(variablesIn, VarBool.FALSE);
			return setOutputs(variablesIn, VarBool.TRUE);
		}
	}
	
	/** Outputs 1 if any input value is true */
	public static class Or extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			for(int i=0; i<inputs().size(); i++)
				if(getVariable(i, variablesIn).asBoolean())
					return setOutputs(variablesIn, VarBool.TRUE);
			return setOutputs(variablesIn, VarBool.FALSE);
		}
	}
	
	/** Outputs 1 if only one input value is true */
	public static class XOR extends ComparisonGlyph
	{
		public VariableSet execute(VariableSet variablesIn)
		{
			boolean foundTrue = false;
			for(int i=0; i<inputs().size(); i++)
			{
				IVariable value = getVariable(i, variablesIn);
				if(value.asBoolean())
					if(foundTrue)
						return setOutputs(variablesIn, VarBool.FALSE);
					else
						foundTrue = true;
			}
			return setOutputs(variablesIn, foundTrue ? VarBool.TRUE : VarBool.FALSE);
		}
	}
}
