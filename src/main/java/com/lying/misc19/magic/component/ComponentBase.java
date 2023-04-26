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
import com.lying.misc19.utility.M19Utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public abstract class ComponentBase implements ISpellComponent
{
	private ISpellComponent parent = null;
	
	private final Param[] inputNeeds;
	protected final List<ISpellComponent> inputGlyphs = Lists.newArrayList();
	protected final List<ISpellComponent> outputGlyphs = Lists.newArrayList();
	
	private ResourceLocation registryName = SpellComponents.GLYPH_DUMMY;
	private float posX = 0F, posY = 0F;
	
	protected ComponentBase(Param... parameters) { this.inputNeeds = parameters; }
	
	public void setParent(ISpellComponent parentIn) { this.parent = parentIn; }
	
	public ISpellComponent parent() { return this.parent; }
	
	public void setPosition(float x, float y)
	{
		posX = x;
		posY = y;
	}
	
	public Vec2 position()
	{
		if(parent() == null)
			return new Vec2(posX, posY);
		else
			return new Vec2(posX, posY).add(parent().position());
	}
	
	public void setRegistryName(ResourceLocation location) { this.registryName = location; }
	
	public ResourceLocation getRegistryName() { return this.registryName; }
	
	public void organise()
	{
		float spin = 180F / inputGlyphs.size();
		Vec2 offset = M19Utils.rotate(up(), 90D).scale(-20F);
		Vec2 start = M19Utils.rotate(offset, spin / 2);
		for(ISpellComponent input : inputGlyphs)
		{
			input.setParent(this);
			input.setPosition(start.x, start.y);
			input.organise();
			start = M19Utils.rotate(start, spin);
		}
		
		spin = 180F / outputGlyphs.size();
		offset = M19Utils.rotate(up(), 90D).scale(20F);
		start = M19Utils.rotate(offset, spin / 2);
		for(ISpellComponent output : outputGlyphs)
		{
			output.setParent(this);
			output.setPosition(start.x, start.y);
			output.organise();
			start = M19Utils.rotate(start, spin);
		}
	}
	
	public void addInput(ISpellComponent component)
	{
		this.inputGlyphs.add(component);
		organise();
	}
	
	public List<ISpellComponent> inputs(){ return this.inputGlyphs; }
	
	public void addOutput(ISpellComponent component)
	{
		this.outputGlyphs.add(component);
		organise();
	}
	
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
