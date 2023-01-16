package com.example.examplemod.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class PlayerPrayerEvent extends PlayerEvent
{
	private final BlockPos pos;
	
	public PlayerPrayerEvent(Player player, BlockPos posIn)
	{
		super(player);
		pos = posIn;
	}
	
	public BlockPos position() { return this.pos; }
}
