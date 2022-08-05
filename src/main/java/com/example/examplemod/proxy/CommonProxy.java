package com.example.examplemod.proxy;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public abstract class CommonProxy implements IProxy
{
	public Player getPlayerEntity(NetworkEvent.Context ctx)
	{
		return ctx.getSender();
	}
}
