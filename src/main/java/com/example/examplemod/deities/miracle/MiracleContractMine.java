package com.example.examplemod.deities.miracle;

import com.example.examplemod.api.event.PlayerBreakItemEvent;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.data.ExBlockTags;
import com.example.examplemod.deities.miracle.BindingContract.IInventoryContract;
import com.example.examplemod.init.ExEnchantments;
import com.example.examplemod.utility.ExUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleContractMine extends Miracle
{
	public MiracleContractMine()
	{
		super(Power.MINOR);
	}

	@Override
	public float getUtility(Player playerIn, Level worldIn) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPickaxeBreak);
	}
	
	public void onPickaxeBreak(PlayerBreakItemEvent event)
	{
		if(event.getItem().getEnchantmentLevel(ExEnchantments.CONTRACT_ITEM.get()) > 0)
		{
			event.setCanceled(true);
			return;
		}
		
		if(!event.isCanceled() && event.getItem().getItem() instanceof PickaxeItem)
		{
			Player player = event.getEntity();
			if(!ContractMining.anyOreAroundPos(player.blockPosition(), player.getLevel()) || !checkMiracle(player, Miracles.CONTRACT_MINE.get()))
				return;
			
			PlayerData.getCapability(player).addContract(new ContractMining(player, event.getItem()));
			event.setCanceled(true);
			reportMiracle(player, Miracles.CONTRACT_MINE.get());
		}
	}
	
	public static class ContractMining extends BindingContract implements IInventoryContract
	{
		private static final double RANGE = 8D;
		
		private final ItemStack stack;
		private final BlockPos areaCore;
		
		public ContractMining(Player targetIn, ItemStack stackIn)
		{
			super(-1);
			this.areaCore = targetIn.blockPosition();
			this.stack = stackIn;
		}
		
		public void start(Player player, Level world)
		{
			this.stack.enchant(ExEnchantments.CONTRACT_ITEM.get(), 1);
		}
		
		public void doEffect(int ticksRemaining, Player player, Level world)
		{
			if(player == null || !player.isAlive())
				disregard();
			else if(player.blockPosition().distSqr(areaCore) > (RANGE * RANGE))
				disregard();
			else if(!anyOreAroundPos(areaCore, world))
				disregard();
		}
		
		public static boolean anyOreAroundPos(BlockPos core, Level worldIn)
		{
			return ExUtils.searchAreaFor(core, worldIn, (int)RANGE, (pos, world) -> world.getBlockState(pos).is(ExBlockTags.ORE_BLOCKS)) != null;
		}
		
		public boolean targets(ItemStack stack) { return stack.equals(this.stack, false); }
	}
}
