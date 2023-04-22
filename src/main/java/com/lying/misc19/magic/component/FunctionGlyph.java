package com.lying.misc19.magic.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.Misc19;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A glyph that performs an actual function based on its inputs and does not have any outputs */
public abstract class FunctionGlyph extends ComponentBase
{
	private final Param[] inputNeeds;
	private final int cost;
	
	protected FunctionGlyph(int costIn, Param... inputs)
	{
		this.cost = costIn;
		this.inputNeeds = inputs;
	}
	
	public Category category() { return Category.FUNCTION; }
	
	public Type type() { return Type.GLYPH; }
	
	public int castingCost() { return cost; }
	
	public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn); }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return false; }
	
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
	
	public VariableSet execute(VariableSet variablesIn)
	{
		if(inputsMet(variablesIn))
			run(variablesIn, collectParams(variablesIn));
		return variablesIn;
	}
	
	protected abstract void run(VariableSet variablesIn, Map<String, IVariable> params);
	
	/** Creates a map containing all variables needed by this function, based on its inputs */
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
		
		public boolean matches(IVariable variable) { return type == variable.type(); }
		
		public IVariable get(Map<String, IVariable> paramsIn) { return paramsIn.getOrDefault(name, VariableSet.DEFAULT); }
	}
	
	public static class Debug extends FunctionGlyph
	{
		public Debug() { super(0); }
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			if(variablesIn.isUsing(Slot.CASTER))
			{
				
			}
			else
			{
				Misc19.LOG.info("# Debug Glyph #");
				Misc19.LOG.info("# Glyphs executed: "+variablesIn.totalGlyphs()+" of "+VariableSet.EXECUTION_LIMIT);
				Misc19.LOG.info("# Casting cost: "+variablesIn.totalCastingCost());
				Misc19.LOG.info("# Register contents:");
				for(Slot slot : VariableSet.Slot.values())
					if(variablesIn.isUsing(slot))
						Misc19.LOG.info("# * "+slot.name()+": "+variablesIn.get(slot).toString());
				Misc19.LOG.info("# Debug End #");
			}
		}
	}
	
	public static class Teleport extends FunctionGlyph
	{
		private static final Param ENTITY = Param.of("entity", VariableType.ENTITY);
		private static final Param POS = Param.of("pos", VariableType.VECTOR);
		
		public Teleport() { super(15, ENTITY, POS); }
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			Entity entity = ENTITY.get(params).asEntity();
			Vec3 pos = POS.get(params).asVec();
			
			if(entity != null && entity.isAlive())
				entity.teleportTo(pos.x, pos.y, pos.z);
		}
	}
}
