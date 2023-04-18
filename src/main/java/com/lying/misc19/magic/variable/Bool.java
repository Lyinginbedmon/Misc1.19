package com.lying.misc19.magic.variable;

public class Bool extends Double
{
	public static final IVariable TRUE = new Bool(true);
	public static final IVariable FALSE = new Bool(false);
	
	public Bool(boolean var) { super(var ? 1D : 0D); }
	
	public String toString() { return "Bool["+asBoolean()+"]"; }
}