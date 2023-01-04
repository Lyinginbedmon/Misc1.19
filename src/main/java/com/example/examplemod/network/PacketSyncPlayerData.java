package com.example.examplemod.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.proxy.CommonProxy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPlayerData
{
	private UUID playerID;
	private CompoundTag dataNBT;
	
	public PacketSyncPlayerData(UUID idIn)
	{
		this.playerID = idIn;
	}
	public PacketSyncPlayerData(UUID idIn, PlayerData dataIn)
	{
		this(idIn);
		this.dataNBT = dataIn.serializeNBT();
	}
	
	public boolean isRequest(){ return dataNBT != null; }
	
	public static PacketSyncPlayerData decode(FriendlyByteBuf par1Buffer)
	{
		PacketSyncPlayerData packet = new PacketSyncPlayerData(par1Buffer.readUUID());
		if(par1Buffer.readBoolean())
			packet.dataNBT = par1Buffer.readNbt();
		return packet;
	}
	
	public static void encode(PacketSyncPlayerData msg, FriendlyByteBuf par1Buffer)
	{
		par1Buffer.writeUUID(msg.playerID);
		par1Buffer.writeBoolean(msg.isRequest());
		if(msg.isRequest())
			par1Buffer.writeNbt(msg.dataNBT);
	}
	
	public static void handle(PacketSyncPlayerData msg, Supplier<NetworkEvent.Context> cxt)
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
			
			if(target != null)
				PacketHandler.sendTo(player, new PacketSyncPlayerData(msg.playerID, PlayerData.getCapability(target)));
		}
		else
		{
			Player localPlayer = ((CommonProxy)ExampleMod.PROXY).getPlayerEntity(context);
			Player target = null;
			if(localPlayer != null)
				if(localPlayer.getUUID().equals(msg.playerID))
					target = localPlayer;
				else
					target = localPlayer.getLevel().getPlayerByUUID(msg.playerID);
			
			if(target != null)
			{
				PlayerData data = PlayerData.getCapability(target);
				if(data != null)
					data.deserializeNBT(msg.dataNBT);
			}
		}
	}
}
