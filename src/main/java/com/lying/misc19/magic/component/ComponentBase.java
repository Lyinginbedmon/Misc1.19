package com.lying.misc19.magic.component;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.init.Components;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public abstract class ComponentBase implements ISpellComponent
{
	protected final List<ISpellComponent> inputGlyphs = Lists.newArrayList();
	protected final List<ISpellComponent> outputGlyphs = Lists.newArrayList();
	
	private ResourceLocation registryName = Components.GLYPH_DUMMY;
	private Vec2 pos = Vec2.ZERO;
	
	public void setPosition(float x, float y) { this.pos = new Vec2(x, y); }
	
	public Vec2 position() { return this.pos;}
	
	public void setRegistryName(ResourceLocation location) { this.registryName = location; }
	
	public ResourceLocation getRegistryName() { return this.registryName; }
	
	public void addInput(ISpellComponent component){ this.inputGlyphs.add(component); }
	
	public List<ISpellComponent> inputs(){ return this.inputGlyphs; }
	
	public void addOutput(ISpellComponent component) { this.outputGlyphs.add(component); }
	
	public List<ISpellComponent> outputs() { return this.outputGlyphs; }
	
	/** Retrieves the variable attached to the given input index, if it is a variable glyph */
	protected IVariable getVariable(int index, VariableSet variablesIn)
	{
		if(inputGlyphs.isEmpty() || index >= inputGlyphs.size())
			return VariableSet.DEFAULT;
		
		ISpellComponent input = this.inputGlyphs.get(index);
		return input.type() == Type.VARIABLE ? ((VariableGlyph)inputs().get(index)).get(variablesIn) : VariableSet.DEFAULT;
	}
	
	/** Sets all output variable glyphs to the given variable */
	protected VariableSet setOutputs(VariableSet variablesIn, IVariable value)
	{
		for(ISpellComponent glyph : outputs())
			if(glyph.type() == Type.VARIABLE)
				variablesIn = ((VariableGlyph)glyph).set(variablesIn, value);
		return variablesIn;
	}
}
