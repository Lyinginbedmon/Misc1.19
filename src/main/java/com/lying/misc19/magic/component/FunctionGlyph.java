package com.lying.misc19.magic.component;

import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.Misc19;
import com.lying.misc19.entities.SpellEntity;
import com.lying.misc19.init.M19Blocks;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VarLevel;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.magic.variable.VariableSet.VariableType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A glyph that performs an actual function based on its inputs and does not have any outputs */
public abstract class FunctionGlyph extends ComponentBase
{
	protected static final Param POS = Param.of("pos", VariableType.VECTOR);
	protected static final Param ENTITY = Param.of("entity", VariableType.ENTITY);
	private final int cost;
	
	protected FunctionGlyph(int costIn, Param... inputs)
	{
		super(inputs);
		this.cost = costIn;
	}
	
	public Category category() { return Category.FUNCTION; }
	
	public Type type() { return Type.FUNCTION; }
	
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
				Entity caster = variablesIn.get(Slot.CASTER).asEntity();
				if(caster.getType() == EntityType.PLAYER)
				{
					Player player = (Player)caster;
					messages.forEach((line) -> player.displayClientMessage(line, false));
				}
			}
			else
				messages.forEach((line) -> Misc19.LOG.info(line.getString()));
		}
	}
	
	public static class Teleport extends FunctionGlyph
	{
		
		public Teleport() { super(15, ENTITY, POS); }
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			Entity entity = ENTITY.get(params).asEntity();
			Vec3 pos = POS.get(params).asVec();
			
			if(entity != null && entity.isAlive())
			{
				Vec3 original = entity.position();
				entity.teleportTo(original.x + pos.x, original.y + pos.y, original.z + pos.z);
			}
		}
	}
	
	public static class Create extends FunctionGlyph
	{
		public Create()
		{
			super(15, POS);
		}
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			Level world = ((VarLevel)variablesIn.get(Slot.WORLD)).get();
			Vec3 pos = POS.get(params).asVec();
			
			BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
			if(blockPos.getY() < -64)
				return;
			
			BlockState state = M19Blocks.PHANTOM_CUBE.get().defaultBlockState();
			if(world.isEmptyBlock(blockPos) || world.getBlockState(blockPos).getMaterial().isReplaceable())
			{
				world.playSound((Player)null, blockPos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS,  0.5F + world.random.nextFloat(), world.random.nextFloat() * 0.7F + 0.6F);
				world.setBlockAndUpdate(blockPos, state);
			}
		}
	}
	
	public static class Dispel extends FunctionGlyph
	{
		protected static final Param RAD = Param.of("radius", VariableType.DOUBLE);
		
		public Dispel()
		{
			super(32, POS, RAD);
		}
		
		protected void run(VariableSet variablesIn, Map<String, IVariable> params)
		{
			Level world = ((VarLevel)variablesIn.get(Slot.WORLD)).get();
			Vec3 pos = POS.get(params).asVec();
			double radius = RAD.get(params).asDouble();
			
			AABB bounds = new AABB(-radius, -radius, -radius, radius, radius, radius).move(pos);
			for(SpellEntity spell : world.getEntitiesOfClass(SpellEntity.class, bounds))
				spell.kill();
		}
	}
}
