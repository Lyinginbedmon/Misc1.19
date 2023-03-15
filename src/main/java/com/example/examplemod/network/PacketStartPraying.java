package com.example.examplemod.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.example.examplemod.capabilities.PlayerData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class PacketStartPraying
{
	private UUID playerID;
	private boolean start;
	
	public PacketStartPraying(UUID idIn)
	{
		this(idIn, true);
	}
	public PacketStartPraying(UUID idIn, boolean operation)
	{
		this.playerID = idIn;
		this.start = operation;
	}
	
	public static PacketStartPraying decode(FriendlyByteBuf par1Buffer)
	{
		PacketStartPraying packet = new PacketStartPraying(par1Buffer.readUUID());
		return packet;
	}
	
	public static void encode(PacketStartPraying msg, FriendlyByteBuf par1Buffer)
	{
		par1Buffer.writeUUID(msg.playerID);
	}
	
	public static void handle(PacketStartPraying msg, Supplier<NetworkEvent.Context> cxt)
	{
		NetworkEvent.Context context = cxt.get();
		context.setPacketHandled(true);
		if(context.getDirection().getReceptionSide().isServer())
		{
			ServerPlayer player = context.getSender();
			Player target = null;
			if(player.getUUID().equals(msg.playerID))
				target = player;
			else
				target = player.getLevel().getPlayerByUUID(msg.playerID);
			
			PlayerData.getCapability(target).setPraying(msg.start);
		}
		else
		{
			
		}
	}
}
