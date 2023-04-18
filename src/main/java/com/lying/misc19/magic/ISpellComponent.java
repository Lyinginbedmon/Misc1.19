package com.lying.misc19.magic;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.magic.variable.VariableSet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public interface ISpellComponent
{
	public void setPosition(float x, float y);
	
	public Vec2 position();
	
	public void setRegistryName(ResourceLocation location);
	
	public ResourceLocation getRegistryName();
	
	public Type type();
	
	public default int castingCost() { return 1; }
	
	public VariableSet execute(VariableSet variablesIn);
	
	public default boolean isValidInput(ISpellComponent component) { return false; }
	
	public default ISpellComponent addInputs(ISpellComponent... components)
	{
		for(int i=0; i<components.length; i++)
			addInput(components[i]);
		
		return this;
	}
	
	public default void addInput(ISpellComponent component) { }
	
	public default List<ISpellComponent> inputs(){ return Lists.newArrayList(); }
	
	public default boolean isValidOutput(ISpellComponent component) { return false; }
	
	public default ISpellComponent addOutputs(ISpellComponent... components)
	{
		for(int i=0; i<components.length; i++)
			addOutput(components[i]);
		return this;
	}
	
	public default void addOutput(ISpellComponent component) { }
	
	public default List<ISpellComponent> outputs() { return Lists.newArrayList(); }
	
	public static CompoundTag saveToNBT(ISpellComponent component)
	{
		CompoundTag nbt = saveAtomically(component);
		
		if(component.position().length() > 0)
		{
			ListTag position = new ListTag();
			position.add(FloatTag.valueOf(component.position().x));
			position.add(FloatTag.valueOf(component.position().y));
			nbt.put("Position", position);
		}
		
		CompoundTag extra = new CompoundTag();
		component.serialiseNBT(extra);
		if(!extra.isEmpty())
			nbt.put("Data", extra);
		
		if(!component.inputs().isEmpty())
		{
			ListTag children = new ListTag();
			component.inputs().forEach((child) -> children.add(saveToNBT(child)));
			nbt.put("Input", children);
		}
		
		if(!component.outputs().isEmpty())
		{
			ListTag children = new ListTag();
			component.outputs().forEach((child) -> children.add(saveToNBT(child)));
			nbt.put("Output", children);
		}
		
		return nbt;
	}
	
	public static CompoundTag saveAtomically(ISpellComponent component)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putString("ID", component.getRegistryName().getPath());
		return nbt;
	}
	
	public default void serialiseNBT(CompoundTag nbt) { }
	
	public default void deserialiseNBT(CompoundTag nbt) { }
	
	public static enum Type
	{
		ROOT,
		VARIABLE,
		CIRCLE,
		GLYPH;
	}
}
