package com.lying.misc19.magic;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.magic.component.OperationGlyph;
import com.lying.misc19.magic.variable.VariableSet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public interface ISpellComponent
{
	public void setRegistryName(ResourceLocation location);
	
	public ResourceLocation getRegistryName();
	
	public void setParent(ISpellComponent parentIn);
	
	/** Component this component is associated with */
	public ISpellComponent parent();
	
	/** Sets relative position to parent (if any)*/
	public void setPosition(float x, float y);
	
	/** Global position in arrangement, including offset from parent */
	public Vec2 position();
	
	/** Position that descendants treat as the target for the purposes of their up() function */
	public default Vec2 core() { return position(); }
	
	/** Update the positions of all child components */
	public void organise();
	
	/** Relative up direction for this glyph */
	public default Vec2 up()
	{
		if(parent() == null)
			return new Vec2(0, 1);
		
		Vec2 pos = position();
		Vec2 par = parent().core();
		
		return new Vec2(par.x - pos.x, par.y - pos.y).normalized();
	}
	
	public Category category();
	
	public Type type();
	
	public default int castingCost() { return 1; }
	
	public VariableSet execute(VariableSet variablesIn);
	
	public default boolean isValidInput(ISpellComponent component) { return false; }
	
	public static boolean canBeInput(ISpellComponent component) { return component.type() == Type.VARIABLE || component instanceof OperationGlyph; }
	
	public default ISpellComponent addInputs(ISpellComponent... components)
	{
		for(int i=0; i<components.length; i++)
		{
			components[i].setParent(this);
			addInput(components[i]);
		}
		
		return this;
	}
	
	public default void addInput(ISpellComponent component) { }
	
	public default List<ISpellComponent> inputs(){ return Lists.newArrayList(); }
	
	public default boolean isValidOutput(ISpellComponent component) { return false; }
	
	public default ISpellComponent addOutputs(ISpellComponent... components)
	{
		for(int i=0; i<components.length; i++)
		{
			components[i].setParent(this);
			addOutput(components[i]);
		}
		return this;
	}
	
	public default void addOutput(ISpellComponent component) { }
	
	public default List<ISpellComponent> outputs() { return Lists.newArrayList(); }
	
	public static CompoundTag saveToNBT(ISpellComponent component)
	{
		CompoundTag nbt = saveAtomically(component);
		
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
	
	public static enum Category
	{
		CONSTANT,
		VARIABLE,
		OPERATION,
		FUNCTION,
		CIRCLE,
		ROOT;
	}
	
	public static enum Type
	{
		ROOT,
		VARIABLE,
		CIRCLE,
		GLYPH;
	}
}
