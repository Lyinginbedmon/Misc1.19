package com.lying.misc19.magic.component;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import com.lying.misc19.magic.ComponentCircle;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VarEntity;
import com.lying.misc19.magic.variable.VarVec;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class RootGlyph extends ComponentCircle.Basic
{
	public Category category() { return Category.ROOT; }
	
	public Type type() { return Type.ROOT; }
	
	public void setParent(ISpellComponent parent) { }
	
	public int calculateRuns(VariableSet variablesIn) { return 1; }
	
	public boolean isValidInput(ISpellComponent component) { return component.type() == Type.HERTZ; }
	
	public Vec2 core() { return position(); }
	
	public abstract void positionAndOrientSpell(Entity spellEntity, LivingEntity caster);
	
	public int tickRate()
	{
		if(inputs().isEmpty())
			return 1;
		
		return inputs().isEmpty() ? 1 : ((HertzGlyph)inputs().get(0)).getTickRate();
	}
	
	protected Pair<Float, Float> separations() { return Pair.of(30F, 80F); }
	
	public void performExecution(@Nonnull Level world, @Nonnull LivingEntity caster, @Nonnull VariableSet variablesIn)
	{
		variablesIn.resetExecutions();
		variablesIn.recacheBeforeExecution(world);
		
		updateCoreVariables(world, caster, variablesIn);
		execute(variablesIn);
		payManaCost(caster, variablesIn.totalCastingCost());
	}
	
	public static void payManaCost(@Nonnull LivingEntity caster, int cost)
	{
		// TODO Subtract variables mana from caster and damage if necessary
	}
	
	public abstract VariableSet populateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn);
	
	public abstract VariableSet updateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn);

	/** Populates the variable set with CASTER and WORLD variables only */
	public static class Dummy extends RootGlyph
	{
		public VariableSet updateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			return variablesIn;
		}
		
		public int castingCost() { return 0; }
		
		public VariableSet populateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn) { return variablesIn; }
		
		public void positionAndOrientSpell(Entity spellEntity, LivingEntity caster) { }
	}
	
	/** Populates the variable set with POSITION, LOOK, and TARGET variable */
	public static class Self extends RootGlyph
	{
		public VariableSet populateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			populateTarget(caster, variablesIn);
			variablesIn.set(Slot.POSITION, new VarVec(caster.position()));
			variablesIn.set(Slot.LOOK, new VarVec(caster.getLookAngle()));
			return variablesIn;
		}
		
		public VariableSet updateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			return populateCoreVariables(world, caster, variablesIn);
		}
		
		public void positionAndOrientSpell(Entity spellEntity, LivingEntity caster)
		{
			spellEntity.setPos(caster.position().add(0, caster.getBbHeight() / 2, 0));
			spellEntity.setYRot(caster.getYRot());
			spellEntity.setXRot(caster.getXRot());
		}
	}
	
	/** Populates the variable set with the POSITION variable */
	public static class Position extends RootGlyph
	{
		public VariableSet populateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn) { return variablesIn; }
		
		public VariableSet updateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			variablesIn.set(Slot.POSITION, new VarVec(caster.position()));
			return variablesIn;
		}
		
		public void positionAndOrientSpell(Entity spellEntity, LivingEntity caster)
		{
			spellEntity.setPos(caster.position());
			spellEntity.setXRot(90F);
			spellEntity.setYRot(caster.getYRot());
		}
	}
	
	/** Populates the variable set with POSITION and TARGET variables */
	public static class Target extends RootGlyph
	{
		public VariableSet populateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			populateTarget(caster, variablesIn);
			return variablesIn;
		}
		
		public VariableSet updateCoreVariables(Level world, LivingEntity caster, VariableSet variablesIn)
		{
			if(variablesIn.get(Slot.TARGET).type() == VariableType.ENTITY)
			{
				Entity ent = variablesIn.get(Slot.TARGET).asEntity();
				variablesIn.set(Slot.POSITION, new VarVec(ent.position().add(0, ent.getBbHeight() / 2, 0)));
				variablesIn.set(Slot.LOOK, new VarVec(ent.getLookAngle()));
			}
			return variablesIn;
		}
		
		public void positionAndOrientSpell(Entity spellEntity, LivingEntity caster)
		{
			VariableSet variable = populateTarget(caster, new VariableSet());
			spellEntity.setPos(variable.get(Slot.POSITION).asVec());
			
		}
	}
	
	protected VariableSet populateTarget(LivingEntity caster, VariableSet variablesIn)
	{
		Vec3 eyePos = caster.getEyePosition();
		Vec3 lookVec = caster.getLookAngle();
		Vec3 lookEnd = eyePos.add(lookVec.scale(64D));
		HitResult trace = caster.getLevel().clip(new ClipContext(eyePos, lookEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
		switch(trace.getType())
		{
			case BLOCK:
				BlockHitResult block = (BlockHitResult)trace;
				BlockPos targetBlock = block.getBlockPos();
				Vec3i look = block.getDirection().getNormal();
				BlockPos pos = targetBlock.offset(block.getDirection().getNormal());
				variablesIn.set(Slot.TARGET, new VarVec(targetBlock.getX() + 0.5D, targetBlock.getY(), targetBlock.getZ() + 0.5D));
				variablesIn.set(Slot.POSITION, new VarVec(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
				variablesIn.set(Slot.LOOK, new VarVec(look.getX(), look.getY(), look.getZ()));
				break;
			case ENTITY:
				EntityHitResult entity = (EntityHitResult)trace;
				Entity targetEntity = entity.getEntity();
				variablesIn.set(Slot.TARGET, new VarEntity(targetEntity));
				variablesIn.set(Slot.POSITION, new VarVec(targetEntity.position().add(0, targetEntity.getBbHeight() / 2, 0)));
				variablesIn.set(Slot.LOOK, new VarVec(targetEntity.getLookAngle()));
				break;
			case MISS:
			default:
				variablesIn.set(Slot.POSITION, new VarVec(caster.getEyePosition()));
				break;
		}
		return variablesIn;
	}
}
