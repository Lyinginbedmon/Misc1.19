package com.example.examplemod.deities.miracle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
	
	/**
	 * Defines a contract targeting a specific item in inventory (usually via {@link ExEnchantments#hasContractEnchantment})<br>
	 * Such items are destroyed if the player holding them has no companion contract of this type.
	 * @author Remem
	 */
	public interface IInventoryContract
	{
		public boolean targets(ItemStack stack);
		
		public static void destroyItem(ItemStack item, Player player)
		{
			if(player.getLevel().isClientSide()) return;
			
			ServerPlayer serverPlayer = (ServerPlayer)player;
			serverPlayer.broadcastBreakEvent(InteractionHand.MAIN_HAND);
			serverPlayer.getInventory().removeItem(item);
		}
	}
}
