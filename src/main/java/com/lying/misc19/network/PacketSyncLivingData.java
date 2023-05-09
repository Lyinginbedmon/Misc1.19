package com.lying.misc19.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.lying.misc19.capabilities.LivingData;
import com.lying.misc19.client.ClientSetupEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncLivingData
{
	private UUID entityUUID;
	private CompoundTag dataNBT;
	
	public PacketSyncLivingData(UUID idIn)
	{
		this.entityUUID = idIn;
	}
	public PacketSyncLivingData(UUID idIn, LivingData dataIn)
	{
		this(idIn);
		this.dataNBT = dataIn.serializeNBT();
	}
	
	public boolean isRequest(){ return dataNBT != null; }
	
	public static PacketSyncLivingData decode(FriendlyByteBuf par1Buffer)
	{
		PacketSyncLivingData packet = new PacketSyncLivingData(par1Buffer.readUUID());
		if(par1Buffer.readBoolean())
			packet.dataNBT = par1Buffer.readNbt();
		return packet;
	}
	
	public static void encode(PacketSyncLivingData msg, FriendlyByteBuf par1Buffer)
	{
		par1Buffer.writeUUID(msg.entityUUID);
		par1Buffer.writeBoolean(msg.isRequest());
		if(msg.isRequest())
			par1Buffer.writeNbt(msg.dataNBT);
	}
	
	public static void handle(PacketSyncLivingData msg, Supplier<NetworkEvent.Context> cxt)
	{
		NetworkEvent.Context context = cxt.get();
		context.setPacketHandled(true);
		if(context.getDirection().getReceptionSide().isServer())
		{
			ServerPlayer player = context.getSender();
			LivingEntity target = null;
			if(player.getUUID().equals(msg.entityUUID))
				target = player;
			else
			{
				target = player.getLevel().getPlayerByUUID(msg.entityUUID);
				
				if(target == null)
				{
					// Non-player target entity
				}
			}
			
			if(target != null)
				PacketHandler.sendTo(player, new PacketSyncLivingData(msg.entityUUID, LivingData.getCapability(target)));
		}
		else
			ClientSetupEvents.getLocalData().deserializeNBT(msg.dataNBT);
	}
}
