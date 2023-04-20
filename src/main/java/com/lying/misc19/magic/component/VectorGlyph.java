package com.lying.misc19.magic.component;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.VariableType;
import com.lying.misc19.magic.variable.VarVec;

import net.minecraft.world.phys.Vec3;

public abstract class VectorGlyph extends OperationGlyph
{
	public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn) && inputs().size() < 2; }
	
	protected abstract IVariable applyTo(VarVec var1, VarVec var2);
	
	public IVariable getResult(VariableSet variablesIn)
	{
		if(inputs().size() < 2)
			return VariableSet.DEFAULT;
		
		IVariable var1 = getVariable(0, variablesIn);
		IVariable var2 = getVariable(1, variablesIn);
		
		if(var1.type() == VariableType.VECTOR && var2.type() == VariableType.VECTOR)
			return applyTo((VarVec)var1, (VarVec)var2);
		
		return VariableSet.DEFAULT;
	}
	
	public static class Normalise extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn) && inputs().isEmpty(); }
		
		public IVariable getResult(VariableSet variablesIn)
		{
			IVariable var1 = getVariable(0, variablesIn);
			if(var1.type() == VariableType.VECTOR)
				return ((VarVec)var1).normalise();
			
			return VariableSet.DEFAULT;
		}
	}
	
	public static class Length extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn) && inputs().isEmpty(); }
		
		public IVariable getResult(VariableSet variablesIn)
		{
			IVariable var1 = getVariable(0, variablesIn);
			if(var1.type() == VariableType.VECTOR)
				return ((VarVec)var1).length();
			
			return VariableSet.DEFAULT;
		}
	}
	
	public static class Compose extends OperationGlyph
	{
		public boolean isValidInput(ISpellComponent componentIn) { return componentIn.type() == Type.VARIABLE && inputs().size() < 3; }
		
		public IVariable getResult(VariableSet variablesIn)
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
			
			return new VarVec(new Vec3(x, y, z));
		}
	}
	
	public static class Dot extends VectorGlyph
	{
		protected IVariable applyTo(VarVec var1, VarVec var2) { return var1.dot(var2); }
	}
	
	public static class Cross extends VectorGlyph
	{
		protected IVariable applyTo(VarVec var1, VarVec var2) { return var1.cross(var2); }
	}
}
