package com.example.examplemod.utility;

import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.MobCommand;
import com.example.examplemod.entity.ai.group.IMobGroup;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketMobCommand;
import com.example.examplemod.reference.Reference;
import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MobCommanding
{
	public static MarkCategory markCategory = MarkCategory.MOTION;
	
	private static Mark[] markOptions = new Mark[]{Mark.CANCEL};
	public static int markIndex = 0;
	
	private static Object targetObj = null;
	private static CompoundTag targetData = new CompoundTag();
	private static List<NotifyData> recipients = Lists.newArrayList();
	
	public static Object currentTarget() { return targetObj; }
	public static Mark currentAction() { return markOptions[markIndex]; }
	public static Mark[] currentOptions() { return markOptions; }
	
	public static boolean isMarking() { return targetObj != null; }
	
	//FIXME Allow for longer-range targeting
	private static final double TRACE_RANGE = 32D;
	
	@OnlyIn(Dist.CLIENT)
	public static void onNotifyPressed(Player player, boolean shiftDown)
	{
		HitResult hitResult = getEntityTarget(player, TRACE_RANGE);
		if(hitResult == null || hitResult.getType() != Type.ENTITY)
			return;
		
		Entity hitEntity = ((EntityHitResult)hitResult).getEntity();
		if(hitEntity instanceof ITreeEntity)
		{
			// If it is already a recipient, remove it
			// If it is not, add it or its group as a recipient
			if(isNotifyTarget(hitEntity))
			{
				removeNotifyTarget(hitEntity);
				player.displayClientMessage(Component.translatable("gui."+Reference.ModInfo.MOD_ID+".notify_target.remove", hitEntity.getDisplayName()), true);
			}
			else
			{
				NotifyData data = new NotifyData(hitEntity, shiftDown);
				recipients.add(data);
				player.displayClientMessage(Component.translatable("gui."+Reference.ModInfo.MOD_ID+".notify_target.add", data.translate()), true);
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void onMarkPressed(Player player)
	{
		targetData = new CompoundTag();
		ListTag variables = new ListTag();
		HitResult hitResult = getPlayerTarget(player, TRACE_RANGE);
		if(hitResult == null)
		{
			targetObj = player;
			variables.add(MobCommand.Utils.storeVariable(player));
		}
		else
		{
			switch(hitResult.getType())
			{
				case BLOCK:
					BlockHitResult blockHit = (BlockHitResult)hitResult;
					targetObj = blockHit.getBlockPos();
					
					// Block position hit
					variables.add(MobCommand.Utils.storeVariable(blockHit.getBlockPos()));
					// Direction hit from
					variables.add(MobCommand.Utils.storeVariable(blockHit.getDirection().getOpposite()));
					break;
				case ENTITY:
					EntityHitResult entityHit = (EntityHitResult)hitResult;
					targetObj = entityHit.getEntity();
					
					variables.add(MobCommand.Utils.storeVariable(entityHit.getEntity()));
					break;
				default:
					targetObj = player;
					variables.add(MobCommand.Utils.storeVariable(player));
					break;
			}
		}
		targetData.put("Variables", variables);
		
		List<Mark> options = Lists.newArrayList();
		for(Mark mark : Mark.values())
			if(mark.testInput(targetObj, player) && mark != Mark.CANCEL)
				options.add(mark);
		options.add(Mark.CANCEL);
		markOptions = options.toArray(new Mark[0]);
	}
	
	@Nullable
	private static HitResult getPlayerTarget(Player player, double range)
	{
		HitResult entityHit = getEntityTarget(player, range);
		HitResult blockHit = player.pick(range, 0F, false);
		
		boolean hasEntity = entityHit != null && entityHit.getType() != Type.MISS;
		boolean hasBlock = blockHit != null && blockHit.getType() != Type.MISS;
		
		if(hasEntity && hasBlock)
		{
			if(entityHit.distanceTo(player) < blockHit.distanceTo(player))
				return entityHit;
			else
				return blockHit;
		}
		else if(hasEntity || hasBlock)
			return hasEntity ? entityHit : blockHit;
		else
			return null;
	}
	
	private static HitResult getEntityTarget(Player player, double range)
	{
        Vec3 eyePos = player.getEyePosition();
        double maxDist = range * range;
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 eyeEnd = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
        AABB aabb = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);
        return ProjectileUtil.getEntityHitResult(player, eyePos, eyeEnd, aabb, (entity) -> {
           return !entity.isSpectator() && entity.isPickable() || entity.getType() == EntityType.ITEM;
        }, maxDist);
	}
	
	public static void onMarkReleased(Player player)
	{
		PacketMobCommand packet = makeServerPacket();
		if(packet != null)
		{
			if(recipients.isEmpty())
				PacketHandler.sendToServer(packet);
			else
				recipients.forEach((data) -> PacketHandler.sendToServer(packet.setTarget(data.hook.getUUID(), data.isGroup)));
			
			player.displayClientMessage(currentAction().translate(targetObj), true);
			recipients.clear();
		}
		
		targetObj = null;
		markIndex = 0;
	}
	
	@Nullable
	public static PacketMobCommand makeServerPacket()
	{
		targetData.putString("Type", currentAction().getSerializedName());
		return currentAction() != Mark.CANCEL ?  new PacketMobCommand(targetData) : null;
	}
	
	public static void incMarkIndex(int inc)
	{
		markIndex += inc;
		if(markIndex < 0)
			markIndex = markOptions.length - 1;
		else
			markIndex = markIndex % markOptions.length;
	}
	
	public static boolean isNotifyTarget(Entity ent)
	{
		for(NotifyData data : recipients)
			if(data.hook == ent)
				return true;
		
		return false;
	}
	
	public static void removeNotifyTarget(Entity ent)
	{
		NotifyData remove = null;
		for(NotifyData data : recipients)
			if(data.hook == ent)
			{
				remove = data;
				break;
			}
		recipients.remove(remove);
	}
	
	public static boolean hasRecipients() { return !recipients.isEmpty(); }
	
	public static List<NotifyData> getOrderRecipients()
	{
		recipients.removeIf((data) -> { return data.hook == null; });
		recipients.sort((o1,o2) ->
		{
			return o1.isGroup && !o2.isGroup ? 1 : !o1.isGroup && o2.isGroup ? -1 : 0;
		});
		return recipients;
	}
	
	public static class NotifyData
	{
		public final Entity hook;
		public final boolean isGroup;
		
		public NotifyData(Entity hookIn, boolean groupIn)
		{
			this.hook = hookIn;
			this.isGroup = groupIn;
		}
		
		public Component translate()
		{
			Component translation = Component.translatable("keys."+Reference.ModInfo.MOD_ID+".notify_target."+(isGroup ? "group" : "individual"), hook.getDisplayName());
			if(isGroup)
			{
				try
				{
					IMobGroup group = GroupSaveData.get(hook.getServer()).getGroup((LivingEntity)hook);
					if(group != null)
						return Component.empty().append(translation).append(Component.literal(" ("+group.size()+")"));
				}
				catch(Exception e) { }
			}
			return translation;
		}
	}
	
	public static enum MarkCategory
	{
		MOTION(Mark.GOTO_POS, Mark.GOTO_MOB, Mark.MOUNT, Mark.DISMOUNT, Mark.FOLLOW_MOB),
		COMBAT(Mark.ATTACK, Mark.CEASEFIRE_MOB, Mark.CEASEFIRE, Mark.GUARD_POS, Mark.GUARD_MOB),
		UTILITY(Mark.PICK_UP, Mark.DROP, Mark.EQUIP, Mark.ACTIVATE, Mark.MINE, Mark.QUARRY),
		GROUP(Mark.JOIN_GROUP, Mark.JOIN_MY_GROUP, Mark.START_GROUP),
		CANCEL(Mark.CANCEL);
		
		public final Mark[] commands;
		
		private MarkCategory(Mark... commandsIn)
		{
			List<Mark> marks = Lists.newArrayList(commandsIn);
			marks.add(Mark.CANCEL);
			this.commands = marks.toArray(new Mark[0]);
		}
	}
	
	public static enum Mark implements StringRepresentable
	{
		GOTO_POS(HitResult.Type.BLOCK, 0, (input, player) -> { return input instanceof BlockPos; }),
		GOTO_MOB(HitResult.Type.ENTITY, 0, (input, player) -> { return input instanceof Entity; }),
		ATTACK(HitResult.Type.ENTITY, 7, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		CEASEFIRE(HitResult.Type.MISS, 13, (input, player) -> { return player.getLevel().isClientSide() ? input == player : true; }),
		CEASEFIRE_MOB(HitResult.Type.ENTITY, 14, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		FOLLOW_MOB(HitResult.Type.ENTITY, 9, (input, player) -> { return input instanceof LivingEntity; }),
		GUARD_MOB(HitResult.Type.ENTITY, 1, (input, player) -> { return input instanceof LivingEntity; }),
		GUARD_POS(HitResult.Type.BLOCK, 2, (input, player) -> { return input instanceof BlockPos; }),
		MOUNT(HitResult.Type.ENTITY, 8, (input, player) -> 
			{
				if(input != player && input instanceof LivingEntity)
				{
					LivingEntity living = (LivingEntity)input;
					return !living.isPassenger();
				}
				return false;
			}),
		DISMOUNT(HitResult.Type.MISS, 12, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		PICK_UP(HitResult.Type.ENTITY, 10, (input, player) -> { return input instanceof ItemEntity; }),
		DROP(HitResult.Type.MISS, 3, (input, player) -> { return input == player || input instanceof BlockPos; }),
		EQUIP(HitResult.Type.ENTITY, 11, (input, player) -> { return input instanceof ItemEntity; }),
		ACTIVATE(HitResult.Type.BLOCK, 4, (input, player) -> { return input instanceof BlockPos; }),
		MINE(HitResult.Type.BLOCK, 5, (input, player) -> { return input instanceof BlockPos; }),
		QUARRY(HitResult.Type.BLOCK, 6, (input, player) -> { return input instanceof BlockPos; }),
		JOIN_GROUP(HitResult.Type.ENTITY, 16, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		JOIN_MY_GROUP(HitResult.Type.MISS, 17, (input, player) -> { return player.getLevel().isClientSide() ? input == player : true; }),
		START_GROUP(HitResult.Type.MISS, 18, (input, player) -> { return input == player || input instanceof BlockPos; }),
		CANCEL(HitResult.Type.MISS, 15, (input, player) -> { return true; });
		
		private final int iconIndex;
		private final HitResult.Type input;
		private final BiPredicate<Object, Player> predicate;
		
		private Mark(HitResult.Type inputIn, int iconIndexIn, BiPredicate<Object, Player> predicateIn)
		{
			this.input = inputIn;
			this.iconIndex = iconIndexIn;
			this.predicate = predicateIn;
		}
		
		public int iconIndex() { return this.iconIndex; }
		
		public HitResult.Type inputType() { return this.input; }
		
		public boolean testInput(Object objIn, Player player) { return predicate.test(objIn, player); }
		
		@OnlyIn(Dist.CLIENT)
		public MutableComponent translate(Object obj)
		{
			if(this != CANCEL)
				return Component.translatable("key."+Reference.ModInfo.MOD_ID+".mark."+name().toLowerCase(), getTranslationIdentifier(obj));
			else
				return Component.translatable("key."+Reference.ModInfo.MOD_ID+".mark."+name().toLowerCase());
		}
		
		@OnlyIn(Dist.CLIENT)
		public static Component getTranslationIdentifier(Object obj)
		{
			if(obj instanceof Entity)
			{
				if(obj == Minecraft.getInstance().player)
					return Component.translatable("key."+Reference.ModInfo.MOD_ID+".mark.self");
				else
					return ((Entity)obj).getDisplayName();
			}
			else if(obj instanceof BlockPos)
			{
				BlockPos pos = (BlockPos)obj;
				return Component.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]");
			}
			else
				return Component.literal("Unknown Object");
		}
		
		public String getSerializedName(){ return name().toLowerCase(); }
		
		public static Mark fromName(String nameIn)
		{
			for(Mark type : values())
				if(type.getSerializedName().equals(nameIn))
					return type;
			return null;
		}
	}
}
