package com.example.examplemod.client.gui.menu;

import com.example.examplemod.block.BlockAltar;
import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.init.ExMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.ForgeMod;

public class MenuAltar extends AbstractContainerMenu
{
	private final BlockPos position;
	
	public MenuAltar(int containerId, Inventory inv)
	{
		this(containerId, (HitResult)null);
	}
	
	public MenuAltar(int containerId, HitResult hitResult)
	{
		super(ExMenus.ALTAR_MENU.get(), containerId);
		this.position = hitResult == null || hitResult.getType() != Type.BLOCK ? null : ((BlockHitResult)hitResult).getBlockPos();
	}
	
	public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
	
	public boolean stillValid(Player player)
	{
		Level world = player.getLevel();
		return player.blockPosition().closerThan(position, player.getAttributeValue(ForgeMod.REACH_DISTANCE.get())) && world.getBlockState(position).getBlock() instanceof BlockAltar;
	}
	
	public void removed(Player player)
	{
		if(player instanceof ServerPlayer)
			PlayerData.getCapability(player).setPraying(false);
	}
}
