package com.example.examplemod.deities.miracle;

import java.util.Collection;

import javax.annotation.Nullable;

import com.example.examplemod.utility.ExUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleReapersBag extends Miracle 
{
	public MiracleReapersBag()
	{
		super(Power.MINOR);
	}
	
	@Override
	public float getUtility(Player playerIn, Level worldIn)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPlayerDeath);
	}
	
	public void onPlayerDeath(LivingDropsEvent event)
	{
		if(!event.isCanceled() && event.getEntity().getType() == EntityType.PLAYER && !event.getDrops().isEmpty())
		{
			Player player = (Player)event.getEntity();
			Level world = player.getLevel();
			if(world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || !checkMiracle(player, Miracles.REAPERS_BAG.get()))
				return;
			
			Collection<ItemEntity> drops = event.getDrops();
			
			BlockPos bagPos = findSafePosNearby(player.blockPosition(), world);
			if(bagPos == null)
				return;
			
			Vec3 pos = new Vec3(bagPos.getX() + 0.5D, bagPos.getY(), bagPos.getZ() + 0.5D);
			RandomSource rand = player.getRandom();
			drops.forEach((drop) -> 
			{
				drop.moveTo(pos.add((rand.nextDouble() - 0.5D) * 0.3D, 0D, (rand.nextDouble() - 0.5D) * 0.3D));
				drop.setDeltaMovement(Vec3.ZERO);
			});
			
			// Light block prevents the position from being flowed into by liquids post-miracle
			world.setBlock(bagPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 0), 0);
			// TODO Custom block that removes itself when items are no longer present
			
			reportMiracle(player, Miracles.REAPERS_BAG.get());
		}
	}
	
	@Nullable
	private static BlockPos findSafePosNearby(BlockPos pos, Level worldIn)
	{
		return ExUtils.searchAreaFor(pos, worldIn, 8, MiracleReapersBag::isValidPos);
	}
	
	private static boolean isValidPos(BlockPos pos, Level worldIn)
	{
		BlockState state = worldIn.getBlockState(pos);
		
		if(!worldIn.isEmptyBlock(pos) || !state.getFluidState().isEmpty())
			return false;
		
		BlockPos below = pos.below();
		BlockState stateBelow = worldIn.getBlockState(below);
		if(!stateBelow.isFaceSturdy(worldIn, below, Direction.UP) || stateBelow.getBlock() == Blocks.CACTUS || stateBelow.getBlock() == Blocks.HOPPER)
			return false;
		
		return true;
	}
}
