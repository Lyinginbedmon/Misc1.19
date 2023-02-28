package com.example.examplemod.deities.miracle;

import java.util.List;
import java.util.function.BiPredicate;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entities.EntityHearthLight;
import com.example.examplemod.entities.IGuardMob;
import com.example.examplemod.init.ExEntities;
import com.example.examplemod.utility.ExUtils;
import com.google.common.base.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public abstract class MiracleSummon extends Miracle
{
	private final RegistryObject<Miracle> registry;
	
	protected MiracleSummon(Power levelIn, RegistryObject<Miracle> objIn)
	{
		super(levelIn);
		this.registry = objIn;
	}
	
	public abstract List<Entity> getSummonsFor(Player playerIn);
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onDamageEvent);
	}
	
	public void onDamageEvent(LivingHurtEvent event)
	{
		if(!event.isCanceled() && event.getSource().getEntity() != null && event.getEntity().getType() == EntityType.PLAYER)
			checkAndPerformMiracle((Player)event.getEntity());
	}
	
	protected void checkAndPerformMiracle(Player player)
	{
		if(!checkMiracle(player, registry.get()))
			return;
		
		// Perform miracle
		getSummonsFor(player).forEach((summon) -> 
		{
			Level world = summon.getLevel();
			world.addFreshEntity(summon);
			world.levelEvent(2004, summon.blockPosition(), 0);
			world.gameEvent(summon, GameEvent.ENTITY_PLACE, summon.blockPosition());
		});
		
		reportMiracle(player, registry.get());
	}
	
	protected static void scatterSummonsAround(BlockPos position, Level world, RandomSource rand, List<Entity> summons)
	{
		scatterSummonsAround(position, world, rand, MiracleSummon::isValidPos, summons);
	}
	
	protected static void scatterSummonsAround(BlockPos position, Level world, RandomSource rand, BiPredicate<BlockPos,Level> qualifier, List<Entity> summons)
	{
		List<BlockPos> spawnPositions = ExUtils.searchForPositions(position, world, 8, (pos,level) -> pos !=position && qualifier.test(pos,level));
		for(int i=0; i<summons.size(); i++)
		{
			Entity guard = summons.get(i);
			BlockPos spawn = spawnPositions.get(i%spawnPositions.size());
			guard.moveTo(new Vec3(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D));
		}
	}
	
	protected static boolean isValidPos(BlockPos pos, Level worldIn)
	{
		if(worldIn.getRandom().nextInt(4) > 0)
			return false;
		
		BlockState state = worldIn.getBlockState(pos);
		if(!worldIn.isEmptyBlock(pos) || !worldIn.isEmptyBlock(pos.above()) || !state.getFluidState().isEmpty())
			return false;
		
		BlockPos below = pos.below();
		BlockState stateBelow = worldIn.getBlockState(below);
		if(!stateBelow.isFaceSturdy(worldIn, below, Direction.UP) || stateBelow.getBlock() == Blocks.CACTUS || stateBelow.getBlock() == Blocks.MAGMA_BLOCK)
			return false;
		
		return true;
	}
	
	public static class HearthLight extends MiracleSummon
	{
		public HearthLight()
		{
			super(Power.MINOR, Miracles.HEARTH_LIGHT);
		}
		
		@Override
		public float getUtility(Player playerIn, Level worldIn) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public List<Entity> getSummonsFor(Player playerIn)
		{
			List<Entity> summons = Lists.newArrayList();
			Level world = playerIn.getLevel();
			
			EntityHearthLight light = ExEntities.HEARTH_LIGHT.get().create(world);
			light.moveTo(playerIn.position());
			light.setOwnerUUID(playerIn.getUUID());
			
			summons.add(light);
			return summons;
		}
	}
	
	public static class DeathGuard extends MiracleSummon
	{
		private static final Function<Level, Mob> GUARD_SKELETON = (world) -> ExEntities.GUARD_SKELETON.get().create(world);
		private static final Function<Level, Mob> GUARD_ZOMBIE = (world) -> ExEntities.GUARD_ZOMBIE.get().create(world);
		
		public DeathGuard()
		{
			super(Power.MAJOR, Miracles.DEATHGUARD);
		}
		
		@Override
		public float getUtility(Player playerIn, Level worldIn) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public List<Entity> getSummonsFor(Player playerIn)
		{
			List<Entity> summons = Lists.newArrayList();
			Level world = playerIn.getLevel();
			if(world.isClientSide())
				return summons;
			
			List<Mob> hostiles = world.getEntitiesOfClass(Mob.class, playerIn.getBoundingBox().inflate(8D), (living) -> living.getTarget() == playerIn);
			
			RandomSource rand = playerIn.getRandom();
			int count = 3 + rand.nextInt(3);
			for(int i=0; i<count; i++)
			{
				Mob guard = rand.nextInt(3) == 0 ? GUARD_SKELETON.apply(world) : GUARD_ZOMBIE.apply(world);
				guard.finalizeSpawn((ServerLevel)world, world.getCurrentDifficultyAt(playerIn.blockPosition()), MobSpawnType.SPAWNER, (SpawnGroupData)null, (CompoundTag)null);
				
				if(guard instanceof IGuardMob)
					((IGuardMob)guard).setOwnerUUID(playerIn.getUUID());
				else if(guard instanceof TamableAnimal)
					((TamableAnimal)guard).setOwnerUUID(playerIn.getUUID());
				
				guard.setTarget(hostiles.get(i%hostiles.size()));
				
				summons.add(guard);
			}
			
			scatterSummonsAround(playerIn.blockPosition(), world, playerIn.getRandom(), summons);
			return summons;
		}
	}
}
