package com.example.examplemod.deities.miracle;

import java.util.Comparator;
import java.util.List;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class MiracleRedirect extends Miracle
{
	protected MiracleRedirect()
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
		bus.addListener(this::onPlayerImpact);
	}
	
	public void onPlayerImpact(ProjectileImpactEvent event)
	{
		if(event.getRayTraceResult().getType() == HitResult.Type.ENTITY && ((EntityHitResult)event.getRayTraceResult()).getEntity().getType() == EntityType.PLAYER)
		{
			Player player = (Player)((EntityHitResult)event.getRayTraceResult()).getEntity();
			if(!checkMiracle(player, Miracles.REDIRECT.get()))
				return;
			
			event.setCanceled(true);
			
			Projectile projectile = event.getProjectile();
			List<Mob> mobs = player.getLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8D), (living) -> living.getTarget() == player);
			if(!mobs.isEmpty())
			{
				Comparator<Mob> sorter = new Comparator<Mob>()
						{
							public int compare(Mob o1, Mob o2)
							{
								double dist1 = o1.distanceToSqr(player);
								double dist2 = o2.distanceToSqr(player);
								return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
							}
						};
				mobs.sort(sorter);
				
				Mob target = mobs.get(0);
				Vec3 centreMass = target.position().add(0, target.getBbHeight() / 2, 0);
				centreMass = centreMass.subtract(projectile.position());
				
				projectile.shoot(centreMass.x, centreMass.y, centreMass.z, 1.6F, 0F);
			}
			
			reportMiracle(player, Miracles.REDIRECT.get());
		}
	}
}
