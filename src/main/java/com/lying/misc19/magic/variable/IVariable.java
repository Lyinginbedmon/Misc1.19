package com.lying.misc19.magic.variable;

import javax.annotation.Nonnull;

import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface IVariable
{
	public VariableType type();
	public String toString();
	
	public default double asDouble() { return 0D; }
	public boolean asBoolean();
	public default Vec3 asVec() { return Vec3.ZERO; }
	public default Entity asEntity() { return null; }
	public default Stack asStack() { return new Stack(this); }
	
	public default boolean equals(@Nonnull IVariable var2) { return this.equals(var2); }
	public boolean greater(@Nonnull IVariable var2);
	public default boolean less(@Nonnull IVariable var2) { return !(equals(var2) || greater(var2)); }
	public IVariable add(@Nonnull IVariable var2);
	public default IVariable subtract(@Nonnull IVariable var2) { return add(var2.multiply(new Double(-1D))); }
	public IVariable multiply(@Nonnull IVariable var2);
	public IVariable divide(@Nonnull IVariable var2);
}