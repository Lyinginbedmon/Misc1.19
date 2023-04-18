package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.VariableType;
import com.lying.misc19.magic.variable.Vec;

import net.minecraft.world.phys.Vec3;

public abstract class VectorGlyph extends OperationGlyph
{
	public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().size() < 2; }
	
	public VariableSet execute(VariableSet variablesIn)
	{
		if(inputs().size() < 2)
			return variablesIn;
		
		IVariable var1 = ((VariableGlyph)inputs().get(0)).get(variablesIn);
		IVariable var2 = ((VariableGlyph)inputs().get(1)).get(variablesIn);
		
		if(var1.type() == VariableType.VECTOR && var2.type() == VariableType.VECTOR)
			return setOutputs(variablesIn, getResult((Vec)var1, (Vec)var2));
		
		return setOutputs(variablesIn, VariableSet.DEFAULT);
	}
	
	protected abstract IVariable getResult(Vec var1, Vec var2);
	
	public static class Normalise extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().isEmpty(); }
		
		public VariableSet execute(VariableSet variablesIn)
		{
			if(inputs().size() != 1)
				return variablesIn;
			
			IVariable var1 = ((VariableGlyph)inputs().get(0)).get(variablesIn);
			if(var1.type() == VariableType.VECTOR)
				return setOutputs(variablesIn, ((Vec)var1).normalise());
			
			return setOutputs(variablesIn, VariableSet.DEFAULT);
		}
	}
	
	public static class Length extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().isEmpty(); }
		
		public VariableSet execute(VariableSet variablesIn)
		{
			if(inputs().size() != 1)
				return variablesIn;
			
			IVariable var1 = ((VariableGlyph)inputs().get(0)).get(variablesIn);
			IVariable result = VariableSet.DEFAULT;
			if(var1.type() == VariableType.VECTOR)
				result = ((Vec)var1).length();
			
			return setOutputs(variablesIn, result);
		}
	}
	
	public static class Compose extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().size() < 3; }
		
		public VariableSet execute(VariableSet variablesIn)
		{
			double x = 0D, y = 0D, z = 0D;
			switch(inputs().size())
			{
				case 0:
					break;
				case 1:
					x = getVariable(0, variablesIn).asDouble();
					break;
				case 2:
					x = getVariable(0, variablesIn).asDouble();
					z = getVariable(1, variablesIn).asDouble();
					break;
				case 3:
					x = getVariable(0, variablesIn).asDouble();
					y = getVariable(1, variablesIn).asDouble();
					z = getVariable(2, variablesIn).asDouble();
					break;
			}
			
			return setOutputs(variablesIn, new Vec(new Vec3(x, y, z)));
		}
	}
	
	public static class Dot extends VectorGlyph
	{
		protected IVariable getResult(Vec var1, Vec var2) { return var1.dot(var2); }
	}
	
	public static class Cross extends VectorGlyph
	{
		protected IVariable getResult(Vec var1, Vec var2) { return var1.cross(var2); }
	}
}
