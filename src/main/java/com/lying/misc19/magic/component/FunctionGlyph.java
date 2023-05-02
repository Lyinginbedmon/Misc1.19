package com.lying.misc19.magic.component;

import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.Misc19;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A glyph that performs an actual function based on its inputs and does not have any outputs */
public abstract class FunctionGlyph extends ComponentBase
{
	private final int cost;
	
	protected FunctionGlyph(int costIn, Param... inputs)
	{
		super(inputs);
		this.cost = costIn;
	}
	
	public Category category() { return Category.FUNCTION; }
	
	public Type type() { return Type.GLYPH; }
	
	public int castingCost() { return cost; }
	
	public boolean isValidInput(ISpellComponent componentIn) { return ISpellComponent.canBeInput(componentIn); }
	
	public boolean isValidOutput(ISpellComponent componentIn) { return false; }
	
	public VariableSet execute(VariableSet variablesIn)
	{
		if(inputsMet(variablesIn))
			run(variablesIn, collectParams(variablesIn));
		return variablesIn;
	}
	
	protected abstract void run(VariableSet variablesIn, Map<String, IVariable> params);
	
	public static class Debug extends FunctionGlyph
	{
		public Debug() { super(0); }
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			List<Component> messages = Lists.newArrayList();
			messages.add(Component.literal("# Debug Glyph #"));
			messages.add(Component.literal("# Glyphs executed: "+variablesIn.totalGlyphs()+" of "+VariableSet.EXECUTION_LIMIT));
			messages.add(Component.literal("# Casting cost: "+variablesIn.totalCastingCost()));
			messages.add(Component.literal("# Register contents:"));
			for(Slot slot : VariableSet.Slot.values())
				if(variablesIn.isUsing(slot))
					messages.add(Component.literal("# * "+slot.name()+": ").append(variablesIn.get(slot).translate()));
			messages.add(Component.literal("# Debug End #"));
			
			if(variablesIn.isUsing(Slot.CASTER))
			{
//				Entity caster = variablesIn.get(Slot.CASTER).asEntity();
//				if(caster.getType() == EntityType.PLAYER)
//				{
//					Player player = (Player)caster;
//					messages.forEach((line) -> player.displayClientMessage(line, false));
//				}
			}
			else
				messages.forEach((line) -> Misc19.LOG.info(line.getString()));
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
