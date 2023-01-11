package com.example.examplemod.api.event;

import com.example.examplemod.deities.Deity;
import com.example.examplemod.deities.miracle.Miracle;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public abstract class MiracleEvent extends PlayerEvent
{
	protected final Deity god;
	protected final Miracle miracle;
	
	protected MiracleEvent(Player playerIn, Deity godIn, Miracle miracleIn)
	{
		super(playerIn);
		this.god = godIn;
		this.miracle = miracleIn;
	}
	
	public Deity godResponsible() { return this.god; }
	public Miracle selectedMiracle( ){ return this.miracle; }
	
	/**
	 * Fired when a miracle is performed, after all checks.
	 * @author Remem
	 *
	 */
	public static class PerformMiracleEvent extends MiracleEvent
	{
		public PerformMiracleEvent(Player playerIn, Deity godIn, Miracle miracleIn)
		{
			super(playerIn, godIn, miracleIn);
		}
	}
	
	/**
	 * Fired before a miracle is performed to test its validity
	 * @author Remem
	 */
	@HasResult
	public static class CheckMiracleEvent extends MiracleEvent
	{
		private final float utility;
		
		public CheckMiracleEvent(Player playerIn, Deity godIn, Miracle miracleIn)
		{
			super(playerIn, godIn, miracleIn);
			this.utility = miracleIn.getUtility(playerIn, playerIn.getLevel());
			
			if(this.utility < 0.5F)
				setResult(Result.DENY);
			else
				for(Miracle miracle : godIn.miracles())
					if(miracle.getClass() != miracleIn.getClass() && miracle.getUtility(playerIn, playerIn.getLevel()) > this.utility)
					{
						setResult(Result.DENY);
						break;
					}
		}
		
		public float value() { return this.utility; }
	}
}
