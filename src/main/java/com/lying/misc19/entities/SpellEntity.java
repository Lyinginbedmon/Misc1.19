package com.lying.misc19.entities;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.init.M19Entities;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.component.RootGlyph;
import com.lying.misc19.magic.variable.VarEntity;
import com.lying.misc19.magic.variable.VarLevel;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;

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
	
	protected void defineSynchedData()
	{
		getEntityData().define(SPELL_DATA, new CompoundTag());
		getEntityData().define(OWNER_UUID, Optional.empty());
	}
	
	protected void readAdditionalSaveData(CompoundTag compound)
	{
		if(compound.contains("Owner", Tag.TAG_INT_ARRAY))
			getEntityData().set(OWNER_UUID, Optional.of(NbtUtils.loadUUID(compound.get("Owner"))));
		getEntityData().set(SPELL_DATA, compound.getCompound("Spell"));
		
		this.variableSet = VariableSet.readFromNBT(compound.getCompound("Variables"));
	}
	
	protected void addAdditionalSaveData(CompoundTag compound)
	{
		if(getOwnerId().isPresent())
			compound.put("Owner", NbtUtils.createUUID(getOwnerId().get()));
		compound.put("Spell", getEntityData().get(SPELL_DATA));
		
		compound.put("Variables", this.variableSet.writeToNBT(new CompoundTag()));
	}
	
	public boolean isAttackable() { return false; }
	
	public void tick()
	{
		super.tick();
		ISpellComponent spell = getSpell();
		variableSet.set(Slot.WORLD, new VarLevel(getLevel()));
		LivingEntity caster = getOwner();
		if(caster != null)
			this.variableSet.set(Slot.CASTER, new VarEntity(caster));
		
		try
		{
			
			((RootGlyph)spell).performExecution(getLevel(), caster, this.variableSet);
		}
		catch(Exception e) { }
		
		if(!this.variableSet.get(Slot.CONTINUE).asBoolean())
			kill();
		else
			setSpell(spell);
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
