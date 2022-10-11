package com.example.examplemod.entity.ai.group;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ITreeEntity;
import com.example.examplemod.entity.ai.CommandStack;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding.Mark;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class Strategy<T extends PathfinderMob & ITreeEntity>
{
	private boolean running = false;
	
	public final Status update(IMobGroup groupIn)
	{
		if(running)
		{
			Status result = tick(groupIn);
			if(result != Status.RUNNING)
				running = false;
			return result;
		}
		else
		{
			running = true;
			return start(groupIn);
		}
	}
	
	public float utility(IMobGroup groupIn) { return 0.5F; }
	
	public abstract Status start(IMobGroup groupIn);
	
	public Status tick(IMobGroup groupIn) { return Status.SUCCESS; }
	
	public void cancel(IMobGroup groupIn) { groupIn.clearCommands(); }
	
	/** Used by mobs when determining where to stand whilst not actively attacking */
	public float evaluatePosition(BlockPos pos, IMobGroup members) { return 1F; }
	
	public static double getAttackReachSqr(Entity ent, Entity member)
	{
		if(ent.getType() == EntityType.PLAYER)
			return ((Player)ent).getAttackRange() + member.getBbWidth();
		else
			return (double)(ent.getBbWidth() * 2F * ent.getBbWidth() + member.getBbWidth());
	}
	
	public static enum Status
	{
		SUCCESS,
		FAILURE,
		RUNNING;
	}
	
	/** Entire group attacks immediately */
	public static class StrategyBrawl<T extends PathfinderMob & ITreeEntity> extends Strategy<T>
	{
		public float utility(IMobGroup groupIn)
		{
			int tally = 0;
			float avgHP = 0F;
			for(LivingEntity member : groupIn.members()) 
			{
				avgHP += member.getHealth() / member.getMaxHealth();
				for(LivingEntity target : groupIn.targets())
					if(member.distanceToSqr(target) <= getAttackReachSqr(target, member))
					{
						tally++;
						break;
					}
			}
			avgHP /= groupIn.size();
			
			return (float)Math.pow((double)tally/(double)groupIn.size(), 6D) * avgHP;
		}
		
		public Status start(IMobGroup groupIn)
		{
			groupIn.members().forEach((member) -> 
			{
				Entity nearest = null;
				double minDist = Double.MAX_VALUE;
				for(LivingEntity target : groupIn.targets())
				{
					double dist = member.distanceToSqr(target);
					if(dist < minDist)
					{
						nearest = target;
						minDist = dist;
					}
				}
				
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
				if(board != null)
					board.setCommands(CommandStack.single(Mark.ATTACK, nearest));
			});
			return Status.SUCCESS;
		}
	}
	
	/** Half of group flanks, then entire group attacks */
	public static class StrategyFlank<T extends PathfinderMob & ITreeEntity> extends Strategy<T>
	{
		private int ambushTicks = 0;
		
		public Status start(IMobGroup groupIn)
		{
			Entity target = groupIn.target();
			
			for(int i=0; i<groupIn.size(); i++)
			{
				LivingEntity member = groupIn.members().get(i);
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
				if(board == null)
					continue;
				
				RandomSource rand = member.getRandom();
				switch(i%2)
				{
					case 0:
						board.setCommands(groupACommand(target));
						break;
					case 1:
						// Calculate flanking position
						PathfinderMob partner = (PathfinderMob)groupIn.members().get(i-1);
						double targetReach = getAttackReachSqr(target, member);
						double range = Math.max(partner.distanceToSqr(target), targetReach + 1);
						int attempts = 50;
						BlockPos flankPos = target.blockPosition();
						do
						{
							Vec3 offset = new Vec3(rand.nextDouble() - 0.5D, rand.nextDouble() - 0.5D, rand.nextDouble() - 0.5D).normalize();
							
							// Discount any offset vectors that move closer to the partner from the target
							if(target.position().add(offset).distanceToSqr(partner.position()) < partner.distanceToSqr(target))
								continue;
							
							offset = offset.scale(targetReach + (range - targetReach) * rand.nextDouble());
							flankPos = target.blockPosition().offset(offset.x, offset.y, offset.z);
						}
						while(attempts-- > 0 && !validateFlank(flankPos, member, partner, target));
						
						CommandStack stack = CommandStack.single(Mark.CEASEFIRE);
						if(validateFlank(flankPos, member, partner, target))
							stack.addTask(Mark.GOTO_POS, flankPos);
						
						board.setCommands(stack);
						break;
				}
			}
			
			return Status.RUNNING;
		}
		
		protected CommandStack groupACommand(Entity target) { return CommandStack.single(Mark.CEASEFIRE); }
		
		protected static boolean validateFlank(BlockPos pos, LivingEntity steve, LivingEntity ally, Entity target)
		{
			/*
			 * Must be
			 * 	* Further away from ally than target is
			 *  * No further from target than ally
			 *  * Pathable to from current position
			 */
			double distFromAlly = ally.distanceToSqr(target);
			double distFromTarg = pos.distSqr(target.blockPosition());
			return 
					distFromTarg <= distFromAlly && 
					pos.distSqr(ally.blockPosition()) >= distFromAlly && (steve instanceof PathfinderMob ? ((PathfinderMob)steve).getNavigation().createPath(pos, 64) != null : true);
		}
		
		public Status tick(IMobGroup groupIn)
		{
			boolean startAttack = true;
			for(LivingEntity member : groupIn.members())
			{
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
				if(board != null && board.hasCommands() && board.currentCommand().type() == Mark.GOTO_POS)
				{
					startAttack = false;
					break;
				}
			}
			
			if(startAttack && ambushTicks++ >= (Reference.Values.TICKS_PER_SECOND * 3))
			{
				Entity target = groupIn.target();
				
				// Once Steve 2 is in position, attack!
				groupIn.members().forEach((member) -> 
				{
					Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
					if(board != null)
						board.setCommands(CommandStack.single(Mark.ATTACK, target));
				});
				ambushTicks = 0;
				return Status.SUCCESS;
			}
			else
				return Status.RUNNING;
		}
	}
	
	/** Half of group attack immediately, other half try to flank target first */
	public static class StrategyAggroFlank<T extends PathfinderMob & ITreeEntity> extends StrategyFlank<T>
	{
		public float utility(IMobGroup groupIn)
		{
			Entity target = groupIn.target();
			
			int tally = 0;
			for(LivingEntity member : groupIn.members())
			{
				if(member.distanceToSqr(target) <= getAttackReachSqr(target, member))
					tally++;
			};
			
			return (float)Math.pow((double)tally/(double)groupIn.size(), 0.75D);
		}
		
		protected CommandStack groupACommand(Entity target) { return CommandStack.single(Mark.ATTACK, target); }
		
		public Status tick(IMobGroup groupIn)
		{
			List<LivingEntity> noncombatants = Lists.newArrayList();
			for(LivingEntity member : groupIn.members())
			{
				Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
				if(board != null && board.getEntity(MobWhiteboard.ATTACK_TARGET) == null)
					noncombatants.add(member);
			}
			
			if(noncombatants.isEmpty())
				return Status.SUCCESS;
			else
				for(LivingEntity member : noncombatants)
				{
					Whiteboard<?> board = Whiteboard.tryGetWhiteboard(member);
					if(board != null && board.getEntity(MobWhiteboard.ATTACK_TARGET) == null && !board.hasCommands())
						board.setCommands(CommandStack.single(Mark.ATTACK, groupIn.target()));
				}
			
			return Status.RUNNING;
		}
	}
}