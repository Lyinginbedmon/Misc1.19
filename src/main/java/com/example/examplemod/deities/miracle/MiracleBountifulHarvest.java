package com.example.examplemod.deities.miracle;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleBountifulHarvest extends Miracle
{
	public MiracleBountifulHarvest()
	{
		super(Power.MINOR);
	}

	public float getUtility(Player playerIn, Level worldIn)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		// FIXME Needs dedicated Fortune level event
	}
}
