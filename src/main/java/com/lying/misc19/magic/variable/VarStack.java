package com.lying.misc19.magic.variable;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.magic.variable.VariableSet.VariableType;

public class VarStack implements IVariable
{
	private List<IVariable> variables = Lists.newArrayList();
	
	public VarStack(IVariable... variablesIn)
	{
		for(int i=0; i<variablesIn.length; i++)
			this.variables.add((variablesIn[i]));
	}
	
	public VariableType type() { return VariableType.STACK; }
	
	public VarStack asStack() { return this; }
	
	public boolean asBoolean() { return variables.isEmpty() ? false : variables.get(0).asBoolean(); }
	
	public double asDouble() { return variables.size(); }
	
	public boolean greater(IVariable var2)
	{
		return var2.type() == VariableType.STACK ? variables.size() > ((VarStack)var2).variables.size() :  false;
	}
	
	public IVariable add(IVariable var2) { return this; }
	
	public IVariable multiply(IVariable var2) { return this; }
	
	public IVariable divide(IVariable var2) { return this; }
	
	public IVariable addToStack(IVariable var2)
	{
		if(asDouble() == 0D)
			return new VarStack(var2);
		
		VarStack stack = new VarStack();
		stack.variables.addAll(this.variables);
		if(getFromStack(0).type() != var2.type())
			return stack;
		else
			stack.variables.add(var2);
		
		return stack;
	}
	
	public IVariable getFromStack(int index)
	{
		return variables.get(index % variables.size());
	}
	
	public IVariable removeFromStack(int index)
	{
		variables.remove(index % variables.size());
		return this;
	}
}
