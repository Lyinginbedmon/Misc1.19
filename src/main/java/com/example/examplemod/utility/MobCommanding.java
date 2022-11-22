package com.example.examplemod.utility;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;
import net.minecraftforge.common.extensions.IForgeBoat;

public class MobCommanding
{
	private static CommandData currentVars = null;
	
	private static List<NotifyData> recipients = Lists.newArrayList();
	
	public static boolean isMarking() { return currentVars != null; }
	public static float categoryLocking() { return currentVars.lockProgress(); }
	
	public static Object currentTarget() { return currentVars.target(); }
	
	public static int currentCategory() { return currentVars.categoryIndex(); }
	public static Mark[] currentHeaders() { return currentVars.headers(); }
	public static Mark[] currentOptions() { return currentVars.options(); }
	public static Mark currentAction() { return currentVars.action(); }
	
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
		HitResult hitResult = getPlayerTarget(player, TRACE_RANGE);
		Object targetObj = null;
		if(hitResult == null)
			targetObj = player;
		else
			switch(hitResult.getType())
			{
				case BLOCK:
					targetObj = ((BlockHitResult)hitResult).getBlockPos();
					break;
				case ENTITY:
					targetObj = ((EntityHitResult)hitResult).getEntity();
					break;
				case MISS:
				default:
					targetObj = player;
					break;
			}
		
		currentVars = new CommandData(targetObj, hitResult, player);
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
		PacketMobCommand packet = currentVars.makePacket(player);
		if(packet != null)
		{
			if(recipients.isEmpty())
				PacketHandler.sendToServer(packet);
			else
				recipients.forEach((data) -> PacketHandler.sendToServer(packet.setTarget(data.hook.getUUID(), data.isGroup)));
			
			player.displayClientMessage(currentAction().translate(currentVars.target()), true);
			recipients.clear();
		}
		
		currentVars = null;
	}
	
	public static void inc(int inc, Player player)
	{
		if(currentVars.isLocked())
			currentVars.incOption(inc);
		else
			currentVars.incCategory(inc, player);
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
	
	/** Holder object for managing current selection details */
	@OnlyIn(Dist.CLIENT)
	public static class CommandData
	{
		private Map<MarkCategory, Mark[]> categoryToOptions = new HashMap<>();
		private MarkCategory category = MarkCategory.MOTION;
		private int optionIndex = 0;
		
		/** System time at instantiation or last change of category */
		private long timeStarted = System.currentTimeMillis();
		/** How long before category becomes locked, in seconds */
		private int lockPeriod = 1000;
		
		/** The initial targeted object, used for option filtration */
		private final Object targetObject;
		/** The actual trace from which the targeted object was derived */
		private final HitResult trace;
		
		public CommandData(Object obj, HitResult traceIn, Player player)
		{
			this.targetObject = obj;
			this.trace = traceIn;
			
			for(MarkCategory cat : MarkCategory.values())
			{
				List<Mark> set = Lists.newArrayList();
				cat.set(player.isCreative()).forEach((mark) -> 
				{
					if(mark.testInput(targetObject, player) || mark == Mark.CANCEL)
						set.add(mark);
				});
				
				if(!set.isEmpty())
				{
					// Always include the option to cancel
					if(!set.contains(Mark.CANCEL))
						set.add(Mark.CANCEL);
					
					categoryToOptions.put(cat, set.toArray(new Mark[0]));
				}
			}
		}
		
		public Object target() { return targetObject; }
		public Mark[] options() { return categoryToOptions.containsKey(category) ? categoryToOptions.get(category) : new Mark[0]; }
		public Mark[] headers()
		{
			List<Mark> heads = Lists.newArrayList();
			for(MarkCategory cat : MarkCategory.values())
				if(categoryToOptions.containsKey(cat))
					heads.add(categoryToOptions.get(cat)[0]);
			return heads.toArray(new Mark[0]);
		}
		public int categoryIndex()
		{
			int index = 0;
			for(Mark head : headers())
				if(head.category() == category)
					return index;
				else
					index++;
			return 0;
		}
		public Mark action() { return options().length == 0 ? null : options()[optionIndex]; }
		
		@Nullable
		public PacketMobCommand makePacket(Player player)
		{
			if(options().length == 0)
				return null;
			
			Map<String, Object> variableMap = new HashMap<>();
			if(trace == null)
				variableMap.put("Entity", player);
			else
				switch(trace.getType())
				{
					case BLOCK:
						BlockHitResult blockHit = (BlockHitResult)trace;
						// Block position hit
						variableMap.put("Pos", blockHit.getBlockPos());
						// Direction hit from
						variableMap.put("Facing", blockHit.getDirection().getOpposite());
						break;
					case ENTITY:
						EntityHitResult entityHit = (EntityHitResult)trace;
						variableMap.put("Entity", entityHit.getEntity());
						break;
					case MISS:
					default:
						variableMap.put("Entity", player);
						break;
				}
			
			return action() != Mark.CANCEL ?  new PacketMobCommand(action().makeCommand(player, variableMap)) : null;
		}
		
		public boolean isLocked() { return lockProgress() >= 1F; }
		public float lockProgress() { return Math.min(1F, (float)(System.currentTimeMillis() - timeStarted) / (float)(lockPeriod)); }
		
		public void incCategory(int inc, Player player)
		{
			MarkCategory[] values = MarkCategory.values();
			int index = category.ordinal() + inc;
			if(index < 0)
				index = values.length - 1;
			else
				index = index % values.length;
			
			category = values[index];
			timeStarted = System.currentTimeMillis();
			
			if(!categoryToOptions.containsKey(category))
				incCategory((int)Math.signum(inc), player);
		}
		
		public void incOption(int inc)
		{
			optionIndex += inc;
			if(optionIndex < 0)
				optionIndex = options().length - 1;
			else
				optionIndex = optionIndex % options().length;
		}
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
		MOTION(Mark.GOTO_POS, Mark.GOTO_MOB, Mark.GOTO_ME, Mark.FOLLOW_ME, Mark.FOLLOW_MOB, Mark.STOP_MOVING),
		COMBAT(Mark.ATTACK, Mark.GUARD_POS, Mark.GUARD_ME, Mark.CEASEFIRE, Mark.CEASEFIRE_MOB, Mark.GUARD_MOB),
		UTILITY(Mark.PICK_UP, Mark.DROP, Mark.EQUIP, Mark.MOUNT, Mark.DISMOUNT, Mark.ACTIVATE, Mark.FARM, Mark.BONEMEAL, Mark.PLACE_BLOCK, Mark.MINE, Mark.QUARRY, Mark.WAIT),
		GROUP(Mark.JOIN_GROUP, Mark.JOIN_MY_GROUP, Mark.START_GROUP),
		CANCEL(Mark.CANCEL);
		
		private final List<Mark> commands;
		
		private MarkCategory(Mark... commandsIn)
		{
			commands = Lists.newArrayList();
			for(Mark mark : commandsIn)
				commands.add(mark);
		}
		
		public List<Mark> set(boolean isCreative)
		{
			List<Mark> options = Lists.newArrayList();
			options.addAll(commands);
			if(!isCreative)
				options.removeAll(Mark.CREATIVE_ONLY);
			return options;
		}
		
		public boolean includes(Mark command) { return commands.contains(command); }
	}
	
	public static enum Mark implements StringRepresentable
	{
		GOTO_POS(Type.BLOCK, 0, false, (input, player) -> { return input instanceof BlockPos; }),
		GOTO_MOB(0, false, (input, player) -> { return input instanceof Entity; }),
		GOTO_ME(0, (player, variables) -> new MobCommand(GOTO_MOB, player)),
		STOP_MOVING(15),
		ATTACK(7, false, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		CEASEFIRE(13),
		CEASEFIRE_MOB(14, false, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		FOLLOW_MOB(9, true, (input, player) -> { return input instanceof LivingEntity; }), // TODO Branch implement Follow
		FOLLOW_ME(9, (player, variables) -> new MobCommand(FOLLOW_MOB, player)),
		GUARD_MOB(1, true, (input, player) -> { return input instanceof LivingEntity; }), // TODO Branch implement Guard mob
		GUARD_ME(1, (player, variables) -> new MobCommand(GUARD_MOB, player)),
		GUARD_POS(Type.BLOCK, 2, true, (input, player) -> { return input instanceof BlockPos; }), // TODO Branch implement Guard pos
		MOUNT(8, false, (input, player) ->
			{
				if(input != player && input instanceof LivingEntity)
				{
					if(player.isCreative())
						return true;
					LivingEntity living = (LivingEntity)input;
					if(living.isPassenger() || living.isVehicle())
						return false;
					
					// Not definitive but Minecraft lacks vital architecture for a conclusive ID method
					if(living instanceof Saddleable)
						return ((Saddleable)living).isSaddled();
					else if(living instanceof IForgeAbstractMinecart)
						return ((IForgeAbstractMinecart)living).canBeRidden();
					else if(living instanceof IForgeBoat)
						return true;
				}
				return false;
			}),
		DISMOUNT(12),
		PICK_UP(10, false, (input, player) -> { return input instanceof ItemEntity; }),
		DROP(3, false, (input, player) -> { return input == player || input instanceof BlockPos; }),
		EQUIP(11, false, (input, player) -> { return input instanceof ItemEntity; }),
		ACTIVATE(Type.BLOCK, 4, false, (input, player) -> { return input instanceof BlockPos; }), // TODO Implement block activation
		MINE(Type.BLOCK, 5, false, (input, player) -> { return input instanceof BlockPos; }),
		QUARRY(Type.BLOCK, 6, false, (input, player) -> { return input instanceof BlockPos; }),
		FARM(Type.BLOCK, 6, true, (input, player) -> input instanceof BlockPos),
		BONEMEAL(Type.BLOCK, 6, false, (input, player) -> input instanceof BlockPos),
		PLACE_BLOCK(Type.BLOCK, 6, false, (input, player) -> input instanceof BlockPos),
		WAIT(15),
		JOIN_GROUP(16, false, (input, player) -> { return input instanceof LivingEntity && input != player; }),
		JOIN_MY_GROUP(17, (player, variables) -> new MobCommand(JOIN_GROUP, player)),
		START_GROUP(18),
		CANCEL(15);
		
		public static final EnumSet<Mark> CREATIVE_ONLY = EnumSet.of(Mark.STOP_MOVING, Mark.BONEMEAL, Mark.PLACE_BLOCK, Mark.WAIT);
		public static final EnumSet<Mark> SURVIVAL_ALLOWED = EnumSet.complementOf(CREATIVE_ONLY);
		
		private final int iconIndex;
		private final BiPredicate<Object, Player> predicate;
		private final boolean isEternal;
		private final CommandSupplier supplier;
		
		private Mark(int iconIndexIn)
		{
			this(Type.MISS, iconIndexIn, false, (input, player) -> true);
		}
		
		private Mark(int iconIndexIn, CommandSupplier supplierIn)
		{
			this.iconIndex = iconIndexIn;
			this.predicate = (input, player) -> true;
			this.isEternal = false;
			this.supplier = supplierIn;
		}
		
		private Mark(int iconIndexIn, boolean eternalIn, BiPredicate<Object, Player> predicateIn)
		{
			this(Type.ENTITY, iconIndexIn, eternalIn, predicateIn);
		}
		
		private Mark(Type inputIn, int iconIndexIn, boolean eternalIn, BiPredicate<Object, Player> predicateIn)
		{
			this.iconIndex = iconIndexIn;
			this.predicate = predicateIn;
			this.isEternal = eternalIn;
			this.supplier = (player, variables) -> new MobCommand(this, variables);
		}
		
		public MobCommand makeCommand() { return makeCommand(new HashMap<String, Object>()); }
		public MobCommand makeCommand(Map<String, Object> variables) { return makeCommand(null, variables); }
		public MobCommand makeCommand(Player player, Map<String, Object> variables) { return supplier.apply(player, variables); }
		
		public static MobCommand atPos(Mark mark, BlockPos pos)
		{
			Map<String, Object> variables = new HashMap<>();
			variables.put("Pos", pos);
			return mark.makeCommand(variables);
		}
		
		public static MobCommand onEntity(Mark mark, Entity ent)
		{
			Map<String, Object> variables = new HashMap<>();
			variables.put("Entity", ent);
			return mark.makeCommand(variables);
		}
		
		public static MobCommand placeBlock(BlockPos pos, Direction facing, Block toPlace)
		{
			Map<String, Object> variables = new HashMap<>();
			variables.put("Pos", pos);
			variables.put("Facing", facing);
			variables.put("Block", toPlace);
			return Mark.PLACE_BLOCK.makeCommand(variables);
		}
		
		public int iconIndex() { return this.iconIndex; }
		
		public boolean testInput(Object objIn, Player player) { return predicate.test(objIn, player); }
		
		/** Returns true if this type of command can be completed solely by the actions of its recipient and not by external events */
		public boolean canBeCompleted() { return !this.isEternal; }
		
		public MarkCategory category()
		{
			for(MarkCategory cat : MarkCategory.values())
				if(cat.includes(this))
					return cat;
			
			return MarkCategory.UTILITY;
		}
		
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
		
		@FunctionalInterface
		public static interface CommandSupplier
		{
			public MobCommand apply(Player player, Map<String, Object> variables);
		}
	}
}
