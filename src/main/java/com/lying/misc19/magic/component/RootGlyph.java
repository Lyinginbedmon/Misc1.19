package com.lying.misc19.magic.component;

import javax.annotation.Nonnull;

import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VarVec;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public abstract class RootGlyph extends ComponentBase
{
	public Category category() { return Category.ROOT; }
	
	public Type type() { return Type.ROOT; }
	
	public boolean isValidInput(ISpellComponent component) { return false; }
	
	public boolean isValidOutput(ISpellComponent component) { return outputs().isEmpty() && component.type() == Type.CIRCLE; }
	
	protected ISpellComponent circle() { return outputs().isEmpty() ? null : outputs().get(0); }
	
	public void performExecution(@Nonnull Level world, @Nonnull LivingEntity caster, @Nonnull VariableSet variablesIn)
	{
		variablesIn.resetExecutions();
		variablesIn.recacheBeforeExecution(world);
		
		populateVariables(world, caster, variablesIn);
		execute(variablesIn);
		payManaCost(caster, variablesIn.totalCastingCost());
	}
	
	public static void payManaCost(@Nonnull LivingEntity caster, int cost)
	{
		// TODO Subtract variables mana from caster and damage if necessary
	}
	
	public VariableSet execute(VariableSet variablesIn)
	{
		return !variablesIn.executionLimited() && circle() != null ? circle().execute(variablesIn).glyphExecuted(circle().castingCost()) : variablesIn;
	}
	
	public abstract VariableSet populateVariables(Level world, LivingEntity caster, VariableSet variablesIn);

	/** Populates the variable set with CASTER and WORLD variables only */
	public static class Dummy extends RootGlyph
	{
		public VariableSet populateVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			return variablesIn;
		}
		
		public int castingCost() { return 0; }
	}
	
	/** Populates the variable set with POSITION, LOOK, and TARGET variable */
	public static class Self extends RootGlyph
	{
		public VariableSet populateVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			variablesIn.set(Slot.POSITION, new VarVec(caster.getEyePosition()));
			variablesIn.set(Slot.LOOK, new VarVec(caster.getLookAngle()));
			return variablesIn;
		}
	}
	
	/** Populates the variable set with the POSITION variable */
	public static class Position extends RootGlyph
	{
		public VariableSet populateVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			variablesIn.set(Slot.POSITION, new VarVec(caster.position()));
			return variablesIn;
		}
	}
	
	/** Populates the variable set with POSITION and TARGET variables */
	public static class Target extends RootGlyph
	{
		public VariableSet populateVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			variablesIn.set(Slot.POSITION, new VarVec(caster.getEyePosition()));
			
			// TODO Trace caster's current look target, either an entity or a position
			
			return variablesIn;
		}
	}
}
