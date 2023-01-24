package com.example.examplemod.api.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class FortuneLevelEvent extends LivingEvent
{
	private final BlockState state;
	private final ItemStack tool;
	
	private final int originalLevel;
	private int fortuneLevel;
	
	public FortuneLevelEvent(@Nonnull LivingEntity entity, @Nonnull BlockState stateIn, @Nullable ItemStack stackIn, int fortuneIn)
	{
		super(entity);
		this.state = stateIn;
		this.tool = stackIn;
		this.originalLevel = fortuneIn;
		this.fortuneLevel = fortuneIn;
	}
	
	public BlockState getBlockState() { return this.state; }
	
	@Nullable
	public ItemStack getTool() { return this.tool.copy(); }
	
	public int getOriginalLevel() { return this.originalLevel; }
	public int getFortuneLevel() { return this.fortuneLevel; }
	
	public void setFortuneLevel(int fortuneLevel) { this.fortuneLevel = fortuneLevel; }
}
