package com.example.examplemod.proxy;

import com.example.examplemod.capabilities.PlayerData;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public abstract class CommonProxy implements IProxy
{
	public Player getPlayerEntity(NetworkEvent.Context ctx)
	{
		return ctx.getSender();
	}
	
	public PlayerData getPlayerData(Player playerIn) { return new PlayerData(playerIn); }
}
