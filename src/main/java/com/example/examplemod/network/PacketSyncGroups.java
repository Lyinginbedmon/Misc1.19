package com.example.examplemod.network;

import java.util.function.Supplier;

import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncGroups
{
	private CompoundTag data = new CompoundTag();
	
	protected PacketSyncGroups() { }
	
	public PacketSyncGroups(GroupSaveData managerIn)
	{
		this.data = managerIn.save(new CompoundTag());
	}
	
	public static PacketSyncGroups decode(FriendlyByteBuf par1Buffer)
	{
		PacketSyncGroups packet = new PacketSyncGroups();
		packet.data = par1Buffer.readNbt();
		return packet;
	}
	
	public static void encode(PacketSyncGroups msg, FriendlyByteBuf par1Buffer)
	{
		par1Buffer.writeNbt(msg.data);
	}
	
	public static void handle(PacketSyncGroups msg, Supplier<NetworkEvent.Context> cxt)
	{
		NetworkEvent.Context context = cxt.get();
		context.setPacketHandled(true);
		
		if(context.getDirection().getReceptionSide().isClient())
			GroupSaveData.clientStorageCopy = GroupSaveData.read(msg.data, true);
	}
}
