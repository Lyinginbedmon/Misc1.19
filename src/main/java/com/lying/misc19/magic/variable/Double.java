package com.lying.misc19.magic.variable;

import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.world.phys.Vec3;

public class Double implements IVariable
{
	protected double value;
	
	public Double(double valueIn) { this.value = valueIn; }
	
	public VariableType type() { return VariableType.DOUBLE; }
	
	public String toString() { return "Double["+value+"]"; }
	
	public double asDouble() { return this.value; }
	
	public boolean asBoolean() { return this.value > 0D; }
	
	public boolean greater(IVariable var2)
	{
		switch(var2.type())
		{
			case DOUBLE:
				return this.value < var2.asDouble();
			case VECTOR:
			case ENTITY:
			default:
				return false;
		}
	}
	
	public IVariable add(IVariable var2)
	{
		switch(var2.type())
		{
			case DOUBLE:
				return new Double(this.value + var2.asDouble());
			case VECTOR:
				return new Vec(var2.asVec().add(value, value, value));
			case ENTITY:
			default:
				return new Double(this.value);
		}
	}
	
	public IVariable multiply(IVariable var2)
	{
		switch(var2.type())
		{
			case DOUBLE:
				return new Double(this.value * var2.asDouble());
			case VECTOR:
				return new Vec(var2.asVec().scale(value));
			case ENTITY:
			default:
				return new Double(this.value);
		}
	}
	
	public IVariable divide(IVariable var2)
	{
		switch(var2.type())
		{
			case DOUBLE:
				return multiply(new Double(1 / var2.asDouble()));
			case VECTOR:
				Vec3 vecVal = var2.asVec();
				return multiply(new Vec(new Vec3(1 / vecVal.x, 1 / vecVal.y, 1 / vecVal.z)));
			case ENTITY:
			default:
				return new Double(this.value);
		}
	}
}