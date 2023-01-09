package com.example.examplemod.deities.personality;

import com.example.examplemod.reference.Reference;
import com.google.common.base.Function;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface ContextQuotient
{
	public static final ResourceKey<Registry<ContextQuotient>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "quotients"));
	/** Default rate at which quotients stored in player data decay per tick */
	public static final double DEFAULT_DECAY = 1D / Reference.Values.TICKS_PER_MINUTE;
	
	/** The value of this quotient for the given player, between 0 and 1 */
	public double get(Player playerIn);
	
	/** How quickly the value of this quotient decays in PlayerData */
	public default double decayRate() { return DEFAULT_DECAY; }
	public default ContextQuotient decay(double value) { return this; }
	
	public static ContextQuotient staticValue(double valueIn) { return (player) -> valueIn; }
	
	public static class Modifiable implements ContextQuotient
	{
		private final Function<Player, Double> operation;
		private double decay = ContextQuotient.DEFAULT_DECAY;
		
		public Modifiable(Function<Player, Double> opIn)
		{
			this.operation = opIn;
		}
		
		/** The value of this quotient for the given player, between 0 and 1 */
		public double get(Player playerIn) { return this.operation.apply(playerIn); }
		
		/** How quickly the value of this quotient decays in PlayerData */
		public double decayRate() { return this.decay; }
		public ContextQuotient decay(double value) { this.decay = value; return this; }
	}
}