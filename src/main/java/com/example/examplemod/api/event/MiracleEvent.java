package com.example.examplemod.api.event;

import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.miracle.Miracle;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class MiracleEvent extends PlayerEvent
{
	private final Deity god;
	private final Miracle miracle;
	private final float utility;
	
	public MiracleEvent(Player playerIn, Deity godIn, Miracle miracleIn, float utilityIn)
	{
		super(playerIn);
		this.god = godIn;
		this.miracle = miracleIn;
		this.utility = utilityIn;
	}
	
	public Deity godResponsible() { return this.god; }
	public Miracle selectedMiracle( ){ return this.miracle; }
	public float value() { return this.utility; }
}
