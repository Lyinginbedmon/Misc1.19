package com.lying.misc19.magic.variable;

import javax.annotation.Nonnull;

import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.world.entity.Entity;

public class VarEntity implements IVariable
{
	private Entity value;
	
	public VarEntity(@Nonnull Entity vecIn) { this.value = vecIn; }
	
	public VariableType type() { return VariableType.ENTITY; }
	
	public String toString() { return "Entity["+value.getDisplayName().getString()+"]"; }
	
	public Entity asEntity() { return this.value; }
	
	public boolean asBoolean() { return this.value != null && this.value.isAlive(); }
	
	public boolean greater(IVariable var2) { return false; }
	
	public IVariable add(@Nonnull IVariable var2) { return new VarEntity(value); }
	
	public IVariable subtract(@Nonnull IVariable var2) { return new VarEntity(value); }
	
	public IVariable multiply(@Nonnull IVariable var2) { return new VarEntity(value); }
	
	public IVariable divide(@Nonnull IVariable var2) { return new VarEntity(value); }
}