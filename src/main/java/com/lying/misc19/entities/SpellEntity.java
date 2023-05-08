package com.lying.misc19.entities;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.lying.misc19.init.M19Entities;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.component.RootGlyph;
import com.lying.misc19.magic.variable.VarBool;
import com.lying.misc19.magic.variable.VarDouble;
import com.lying.misc19.magic.variable.VarEntity;
import com.lying.misc19.magic.variable.VarLevel;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.reference.Reference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class SpellEntity extends Entity
{
	protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	protected static final EntityDataAccessor<CompoundTag> SPELL_DATA = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<Integer> VISIBILITY = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
	private static final int VANISH_TIME = Reference.Values.TICKS_PER_SECOND * 2;
	
	private int ticks = 0;
	private VariableSet variableSet = new VariableSet();
	private LivingEntity ownerCached = null;
	
	protected SpellEntity(Level worldIn) { this(M19Entities.SPELL.get(), worldIn); }
	public SpellEntity(EntityType<SpellEntity> typeIn, Level worldIn)
	{
		super(typeIn, worldIn);
	}
	
	public Packet<?> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
	
	public static SpellEntity create(ISpellComponent spellIn, LivingEntity owner, Level world)
	{
		SpellEntity spell = new SpellEntity(world);
		spell.setOwner(owner);
		spell.setSpell(spellIn);
		spell.setPos(owner.position().x, owner.position().y + (owner.getBbHeight() / 2F), owner.position().z);
		spell.setYRot(owner.getYRot());
		spell.setXRot(owner.getXRot());
		return spell;
	}
	
	public void setVariables(VariableSet variablesIn) { this.variableSet = variablesIn; }
	
	protected void defineSynchedData()
	{
		getEntityData().define(SPELL_DATA, new CompoundTag());
		getEntityData().define(OWNER_UUID, Optional.empty());
		getEntityData().define(VISIBILITY, VANISH_TIME);
	}
	
	protected void readAdditionalSaveData(CompoundTag compound)
	{
		if(compound.contains("Owner", Tag.TAG_INT_ARRAY))
			getEntityData().set(OWNER_UUID, Optional.of(NbtUtils.loadUUID(compound.get("Owner"))));
		getEntityData().set(SPELL_DATA, compound.getCompound("Spell"));
		
		this.variableSet = VariableSet.readFromNBT(compound.getCompound("Variables"));
		this.ticks = compound.getInt("Ticks");
		
		getEntityData().set(VISIBILITY, compound.getInt("Vanish"));
	}
	
	protected void addAdditionalSaveData(CompoundTag compound)
	{
		if(getOwnerId().isPresent())
			compound.put("Owner", NbtUtils.createUUID(getOwnerId().get()));
		compound.put("Spell", getEntityData().get(SPELL_DATA));
		
		compound.put("Variables", this.variableSet.writeToNBT(new CompoundTag()));
		compound.putInt("Ticks", this.ticks);
		
		compound.putInt("Vanish", getEntityData().get(VISIBILITY).intValue());
	}
	
	public boolean isAttackable() { return false; }
	
	public float getVisibility() { return getEntityData().get(VISIBILITY).floatValue() / (float)VANISH_TIME; }
	
	public void tick()
	{
		super.tick();
		if(getLevel().isClientSide())
			return;
		
		// Prevent any spell below the world from surviving
		if(this.getY() <= -64)
			kill();
		
		// Gradual fading out of expired spells
		if(getVisibility() < 1F)
		{
			int visibility = getEntityData().get(VISIBILITY).intValue() - 1;
			getEntityData().set(VISIBILITY, visibility);
			
			if(visibility <= 0)
				kill();
			return;
		}
		
		ISpellComponent spell = getSpell();
		
		variableSet.set(Slot.WORLD, new VarLevel(getLevel()));
		LivingEntity caster = getOwner();
		if(caster != null)
			this.variableSet.set(Slot.CASTER, new VarEntity(caster));
		
		this.ticks++;
		try
		{
			RootGlyph root = (RootGlyph)spell;
			if(ticks % root.tickRate() == 0)
			{
				this.variableSet.set(Slot.CONTINUE, VarBool.FALSE);
				root.performExecution(getLevel(), caster, this.variableSet);
				this.variableSet.set(Slot.AGE, new VarDouble(this.variableSet.get(Slot.AGE).asDouble() + 1));
				
				if(!this.variableSet.get(Slot.CONTINUE).asBoolean())
					getEntityData().set(VISIBILITY, VANISH_TIME - 1);
				else
					setSpell(spell);
			}
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	public LivingEntity getOwner()
	{
		try
		{
			Optional<UUID> ownerId = getOwnerId();
			return !ownerId.isPresent() ? ownerCached : (this.ownerCached = getLevel().getPlayerByUUID(ownerId.get()));
		}
		catch(IllegalArgumentException e)
		{
			return this.ownerCached;
		}
	}
	
	protected Optional<UUID> getOwnerId(){ return getEntityData().get(OWNER_UUID); }
	
	protected void setOwner(@Nonnull LivingEntity ownerIn)
	{
		ownerCached = ownerIn;
		getEntityData().set(OWNER_UUID, Optional.of(ownerIn.getUUID()));
	}
	
	public ISpellComponent getSpell()
	{
		return SpellComponents.readFromNBT(getEntityData().get(SPELL_DATA));
	}
	
	public void setSpell(ISpellComponent spellIn)
	{
		getEntityData().set(SPELL_DATA, ISpellComponent.saveToNBT(spellIn));
	}
}
