package com.example.examplemod.deities.miracle;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.reference.Reference;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleLightning extends Miracle
{
	public MiracleLightning()
	{
		super(Power.MAJOR);
	}
	
	@Override
	public float getUtility(Player playerIn, Level worldIn)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void addListeners(IEventBus bus)
	{
		bus.addListener(this::onPlayerHurt);
	}
	
	public void onPlayerHurt(LivingHurtEvent event)
	{
		if(!event.isCanceled() && event.getEntity().getType() == EntityType.PLAYER)
		{
			Player player = (Player)event.getEntity();
			Entity aggressor = event.getSource().getEntity();
			if(aggressor == null || !aggressor.isAlive() || !(aggressor instanceof LivingEntity) || aggressor.isInvulnerableTo(DamageSource.LIGHTNING_BOLT))
				return;
			
			if(!checkMiracle(player, Miracles.LIGHTNING.get()))
				return;
			
			PlayerData.getCapability(player).addContract(new ContractLightning((LivingEntity)aggressor));
			reportMiracle(player, Miracles.LIGHTNING.get());
		}
	}
	
	private static class ContractLightning extends BindingContract
	{
		private static final int DURATION = Reference.Values.TICKS_PER_SECOND * 20;
		private final LivingEntity target;
		private boolean causedThunder = false;
		
		public ContractLightning(LivingEntity targetIn)
		{
			super(DURATION);
			this.target = targetIn;
		}
		
		public void start(Player player, Level world)
		{
			if(!world.isClientSide() && !((ServerLevel)world).isThundering())
			{
				((ServerLevel)world).setWeatherParameters(0, DURATION + (Reference.Values.TICKS_PER_SECOND * 5), true, true);
				causedThunder = true;
			}
		}
		
		public void doEffect(int ticksRemaining, Player player, Level world)
		{
			if(!target.isAlive() || player.distanceTo(target) > 16D)
				disregard();
			else if(!world.isClientSide() && DURATION - ticksRemaining >= (Reference.Values.TICKS_PER_SECOND * 5))
				if(player.distanceTo(target) > 6D && world.canSeeSky(target.blockPosition()))
				{
					for(int i=0; i<(3 + target.getRandom().nextInt(3)); i++)
					{
						LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
						lightning.moveTo(target.getX(), target.getY(), target.getZ());
						lightning.setVisualOnly(i > 0);
						world.addFreshEntity(lightning);
					}
					
					disregard();
				}
		}
		
		public void cleanup(Player player, Level world)
		{
			if(!world.isClientSide() && causedThunder)
				((ServerLevel)world).setWeatherParameters(6000, 0, false, false);
		}
	}
}
