package com.example.examplemod.network;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class PacketMobCommand
{
	private UUID targetID = null;
	private boolean isGroup = false;
	
	private CompoundTag commandData = new CompoundTag();
	
	public PacketMobCommand(CompoundTag commandDataIn)
	{
		this.commandData = commandDataIn;
	}
	
	public PacketMobCommand(MobCommand commandIn)
	{
		this(commandIn.saveToNBT(new CompoundTag()));
	}
	
	public static PacketMobCommand decode(FriendlyByteBuf par1Buffer)
	{
		PacketMobCommand packet = new PacketMobCommand(par1Buffer.readNbt());
		if(par1Buffer.readBoolean())
			packet.setTarget(par1Buffer.readUUID(), par1Buffer.readBoolean());
		
		return packet;
	}
	
	public static void encode(PacketMobCommand msg, FriendlyByteBuf par1Buffer)
	{
		par1Buffer.writeNbt(msg.commandData);
		
		par1Buffer.writeBoolean(msg.isTargeted());
		par1Buffer.writeUUID(msg.isTargeted() ? msg.targetID : Reference.Values.DUMMY_ID);
		par1Buffer.writeBoolean(msg.isGroup);
	}
	
	public boolean isTargeted() { return this.targetID != null; }
	public PacketMobCommand setTarget(@Nullable UUID targetIn, boolean groupIn)
	{
		this.targetID = targetIn;
		this.isGroup = groupIn;
		return this;
	}
	
	public static void handle(PacketMobCommand msg, Supplier<NetworkEvent.Context> cxt)
	{
		NetworkEvent.Context context = cxt.get();
		context.setPacketHandled(true);
		
		if(context.getDirection().getReceptionSide().isServer())
		{
			Player player = context.getSender();
			Level world = player.getLevel();
			if(player != null)
			{
				CommandStack commandStack = CommandStack.single(MobCommand.loadFromNBT(msg.commandData, 16D, world));
				if(!msg.isTargeted())
					tryGiveToGroup(GroupSaveData.get(player.getServer()).getGroup(player), commandStack, player);
				else
				{
					// Find specific recipient
					Entity targetByID = MobCommand.Utils.findEntityOfUUID(msg.targetID, player.blockPosition(), 16D, world);
					if(targetByID != null && LivingEntity.class.isAssignableFrom(targetByID.getClass()))
					{
						LivingEntity target = (LivingEntity)targetByID;
						if(msg.isGroup)
							tryGiveToGroup(GroupSaveData.get(player.getServer()).getGroup(target), commandStack, player);
						else if(target instanceof ITreeEntity)
						{
							Whiteboard<?> board = Whiteboard.tryGetWhiteboard(target);
							if(board != null)
								board.setCommands(commandStack);
						}
					}
				}
			}
		}
	}
	
	private static void tryGiveToGroup(IMobGroup group, CommandStack command, Player player)
	{
		if(group != null && (player.getAbilities().instabuild || group.shouldListenTo(player)))
			group.giveCommandToAll(command);
		else
			player.displayClientMessage(Component.translatable("gui."+Reference.ModInfo.MOD_ID+".notify_target.none"), true);
	}
}
