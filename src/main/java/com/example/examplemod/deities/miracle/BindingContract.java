package com.example.examplemod.deities.miracle;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class BindingContract
{
	/** How long this contract will continue to operate, or -1 if it does not expire due to age */
	private int duration;
	
	protected BindingContract(int durationIn)
	{
		this.duration = durationIn;
	}
	
	public final void tick(Player player, Level world)
	{
		if(!isComplete())
			doEffect(duration < 0 ? 0 : duration--, player, world);
	}
	
	/** Returns true when {@link #duration} equals 0 */
	public final boolean isComplete() { return duration == 0; }
	
	/** Sets {@link #duration} to 0, preventing further ticks and marking this contract complete */
	protected final void disregard()
	{
		this.duration = 0;
	}
	
	public void start(Player player, Level world) { }
	
	/** Called by {@link #tick(Player, Level)} whilst {@link #duration} does not equal 0 */
	public abstract void doEffect(int ticksRemaining, Player player, Level world);
	
	/** Used to tidy up when this contract is cancelled or completed early */
	public void cleanup(Player player, Level world) { }
}
