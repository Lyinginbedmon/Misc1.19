package com.lying.misc19.magic.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public abstract class ComponentBase implements ISpellComponent
{
	private final Param[] inputNeeds;
	protected final List<ISpellComponent> inputGlyphs = Lists.newArrayList();
	protected final List<ISpellComponent> outputGlyphs = Lists.newArrayList();
	
	private ResourceLocation registryName = SpellComponents.GLYPH_DUMMY;
	private Vec2 pos = Vec2.ZERO;
	
	protected ComponentBase(Param... parameters) { this.inputNeeds = parameters; }
	
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
		
		return getVariable(this.inputGlyphs.get(index), variablesIn);
	}
	
	protected IVariable getVariable(ISpellComponent input, VariableSet variablesIn)
	{
		if(input.type() == Type.VARIABLE)
			return ((VariableGlyph)input).get(variablesIn);
		else if(input instanceof OperationGlyph)
			return ((OperationGlyph)input).getResult(variablesIn);
		else
			return VariableSet.DEFAULT;
	}
	
	public Param[] getInputNeeds() { return inputNeeds; }
	
	public boolean inputsMet(VariableSet variablesIn)
	{
		if(inputNeeds.length == 0)
			return true;
		
		List<VariableType> needs = Lists.newArrayList();
		for(int i=0; i<inputNeeds.length; i++)
			needs.add(inputNeeds[i].type);
		
		for(int i=0; i<inputs().size(); i++)
			needs.remove(getVariable(i, variablesIn).type());
		
		return needs.isEmpty();
	}
	
	/** Sets all output variable glyphs to the given variable */
	protected VariableSet setOutputs(VariableSet variablesIn, IVariable value)
	{
		for(ISpellComponent glyph : outputs())
			if(glyph.type() == Type.VARIABLE)
				variablesIn = ((VariableGlyph)glyph).set(variablesIn, value);
		return variablesIn;
	}
	
	/** Creates a map containing all variables needed by this glyph, based on its inputs */
	protected Map<String, IVariable> collectParams(VariableSet variablesIn)
	{
		Map<String, IVariable> params = new HashMap<>();
		List<ISpellComponent> inputs = Lists.newArrayList();
		inputs.addAll(inputs());
		
		for(Param param : inputNeeds)
		{
			ISpellComponent paramInput = null;
			for(ISpellComponent input : inputs)
			{
				IVariable var = getVariable(input, variablesIn);
				if(param.matches(var))
				{
					params.put(param.name, var);
					paramInput = input;
					break;
				}
			}
			if(paramInput != null)
				inputs.remove(paramInput);
		}
		
		return params;
	}
	
	protected static class Param
	{
		private final VariableType type;
		private final String name;
		
		private Param(String nameIn, VariableType typeIn)
		{
			name = nameIn;
			type = typeIn;
		}
		
		public static Param of(String nameIn, VariableType typeIn) { return new Param(nameIn, typeIn); }
		
		// XXX Allow for variable polymorphism instead of rigid type-matching?
		public boolean matches(IVariable variable) { return type == variable.type(); }
		
		public IVariable get(Map<String, IVariable> paramsIn) { return paramsIn.getOrDefault(name, VariableSet.DEFAULT); }
	}
}
