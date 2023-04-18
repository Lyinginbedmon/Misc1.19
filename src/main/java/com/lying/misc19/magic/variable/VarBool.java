package com.lying.misc19.magic.variable;

public class VarBool extends VarDouble
{
	public static final IVariable TRUE = new VarBool(true);
	public static final IVariable FALSE = new VarBool(false);
	
	public VarBool(boolean var) { super(var ? 1D : 0D); }
	
	public String toString() { return "Bool["+asBoolean()+"]"; }
}