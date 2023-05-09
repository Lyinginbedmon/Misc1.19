package com.lying.misc19.capabilities;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import com.lying.misc19.client.ClientSetupEvents;
import com.lying.misc19.entities.SpellEntity;
import com.lying.misc19.init.M19Capabilities;
import com.lying.misc19.init.M19Entities;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.network.PacketHandler;
import com.lying.misc19.network.PacketSyncLivingData;
import com.lying.misc19.reference.Reference;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class LivingData implements ICapabilitySerializable<CompoundTag>
{
	public static final ResourceLocation IDENTIFIER = new ResourceLocation(Reference.ModInfo.MOD_ID, "living_data");
	
	private LivingEntity theEntity;
	
	private final List<SpellData> unloadedSpells = Lists.newArrayList();
	private final List<SpellEntity> activeSpellEntities = Lists.newArrayList();
	
	private boolean isDirty = true;
	
	public LivingData(LivingEntity livingIn)
	{
		this.theEntity = livingIn;
	}
	
	public void setLiving(LivingEntity livingIn) { this.theEntity = livingIn; }
	
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
	{
		return M19Capabilities.LIVING_DATA.orEmpty(cap, LazyOptional.of(() -> this));
	}
	
	@Nullable
	public static LivingData getCapability(LivingEntity living)
	{
		if(living == null || living.getLevel() == null)
			return null;
		else if(living.getLevel().isClientSide())
			return ClientSetupEvents.getLivingData(living);
		
		LivingData data = living.getCapability(M19Capabilities.LIVING_DATA).orElse(new LivingData(living));
		data.setLiving(living);
		return data;
	}
	
	public CompoundTag serializeNBT()
	{
		CompoundTag data = new CompoundTag();
		ListTag spells = new ListTag();
		this.unloadedSpells.forEach((spell) -> spells.add(spell.saveToNbt(new CompoundTag())));
		this.activeSpellEntities.forEach((spell) -> spells.add(SpellData.create(spell).saveToNbt(new CompoundTag())));
		if(!spells.isEmpty())
			data.put("Spells", spells);
		return data;
	}
	
	public void deserializeNBT(CompoundTag data)
	{
		this.unloadedSpells.clear();
		this.activeSpellEntities.clear();
		if(!data.contains("Spells", Tag.TAG_LIST))
			return;
		
		ListTag spells = data.getList("Spells", Tag.TAG_COMPOUND);
		for(int i=0; i<spells.size(); i++)
		{
			CompoundTag nbt = spells.getCompound(i);
			try
			{
				this.unloadedSpells.add(SpellData.loadFrom(nbt));
			}
			catch(Exception e) { }
		}
	}
	
	public boolean hasSpells() { return !this.activeSpellEntities.isEmpty() || !this.unloadedSpells.isEmpty(); }
	public List<SpellEntity> getActiveSpellEntities() { return this.activeSpellEntities; }
	public List<SpellData> getActiveSpells()
	{
		List<SpellData> spells = Lists.newArrayList();
		spells.addAll(unloadedSpells);
		this.activeSpellEntities.forEach((spell) -> spells.add(SpellData.create(spell)));
		return spells;
	}
	public void addSpell(SpellEntity spellIn)
	{
		this.activeSpellEntities.add(spellIn);
		markDirty();
	}
	
	public void tick()
	{
		if(this.theEntity == null || this.theEntity.getLevel() == null)
			return;
		
		if(this.theEntity.getLevel() != null && !this.unloadedSpells.isEmpty())
		{
			Level world = this.theEntity.getLevel();
			this.unloadedSpells.forEach((data) -> this.activeSpellEntities.add(data.createEntity(world)));
			this.unloadedSpells.clear();
			markDirty();
		}
		
		if(hasSpells())
		{
			Vec3 pos = this.theEntity.getPosition(0F).add(0D, this.theEntity.getBbHeight() / 2, 0D);
			this.activeSpellEntities.forEach((spell) -> 
			{
				spell.setXRot(-90F);
				spell.setYRot(this.theEntity.getYRot());
				spell.setPos(pos);
				
				if(spell.isAlive())
					spell.tick();
			});
			this.activeSpellEntities.removeIf((spell) -> !spell.isAlive());
			markDirty();
		}
		
		if(isDirty && !this.theEntity.getLevel().isClientSide())
		{
			PacketSyncLivingData packet = new PacketSyncLivingData(this.theEntity.getUUID(), this);
			if(this.theEntity.getType() == EntityType.PLAYER)
				PacketHandler.sendTo((ServerPlayer)this.theEntity, packet);
			else
				PacketHandler.sendToAll((ServerLevel)this.theEntity.getLevel(), packet);
			
			this.isDirty = false;
		}
	}
	
	public void markDirty() { this.isDirty = true; }
	
	public static class SpellData
	{
		ISpellComponent arrangement;
		VariableSet variables;
		UUID ownerUUID;
		
		public SpellEntity createEntity(Level world)
		{
			SpellEntity spell = M19Entities.SPELL.get().create(world);
			spell.setSpell(arrangement);
			spell.setVariables(variables);
			spell.setOwnerUUID(ownerUUID);
			return spell;
		}
		
		public static SpellData create(SpellEntity entity)
		{
			SpellData data = new SpellData();
			data.arrangement = entity.getSpell();
			data.variables = entity.getVariables();
			data.ownerUUID = entity.getOwnerId().get();
			
			return data;
		}
		
		public CompoundTag saveToNbt(CompoundTag nbt)
		{
			nbt.put("Spell", ISpellComponent.saveToNBT(arrangement));
			nbt.put("Vars", variables.writeToNBT(new CompoundTag()));
			nbt.putUUID("Owner", ownerUUID);
			return nbt;
		}
		
		public static SpellData loadFrom(CompoundTag nbt)
		{
			SpellData data = new SpellData();
			data.arrangement = SpellComponents.readFromNBT(nbt.getCompound("Spell"));
			data.variables = VariableSet.readFromNBT(nbt.getCompound("Vars"));
			data.ownerUUID = nbt.getUUID("Owner");
			return data;
		}
		
		public ISpellComponent arrangement() { return this.arrangement; }
	}
}