package com.example.examplemod.deities.miracle;

import com.example.examplemod.api.event.AttemptNockEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleLastArrow extends Miracle
{
	public MiracleLastArrow()
	{
		super(Power.MINOR);
	}
	
	public float getUtility(Player playerIn, Level worldIn)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onNockWhileEmpty);
	}
	
	public void onNockWhileEmpty(AttemptNockEvent event)
	{
		if(event.getResult() != Result.DENY && !event.hasAmmo())
		{
			Player player = event.getEntity();
			if(!checkMiracle(player, Miracles.LAST_ARROW.get()))
				return;
			
			player.addItem(new ItemStack(Items.ARROW));
			reportMiracle(player, Miracles.LAST_ARROW.get());
		}
	}
}
