package com.example.examplemod.entity;

import com.example.examplemod.entity.ai.BehaviourTree;
import com.example.examplemod.entity.ai.Branches;
import com.example.examplemod.entity.ai.Checks;
import com.example.examplemod.entity.ai.Node.Decorator;
import com.example.examplemod.entity.ai.Node.Parallel;
import com.example.examplemod.entity.ai.Node.Selector;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

public class TestEntity extends PathfinderMob
{
	private final BehaviourTree behaviourTree;
	
	public TestEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_)
	{
		super(p_21683_, p_21684_);
		
		Whiteboard<Mob> whiteboard = new MobWhiteboard<Mob>(this);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_SWORD, Whiteboard.Expansions::getBestSword);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_HEAD, Whiteboard.Expansions::getBestHead);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_CHEST, Whiteboard.Expansions::getBestChest);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_LEGS, Whiteboard.Expansions::getBestLegs);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_FEET, Whiteboard.Expansions::getBestFeet);
		
		behaviourTree = new BehaviourTree("main_tree", whiteboard, 
			Selector.root(
				new Decorator("has_attack_target", Checks.HAS_TARGET, Branches.attackMelee()),
				new Selector("idle",
					Branches.equipBestGear(),
					new Parallel("wandering",
							Branches.wander(),
							Branches.lookRandom(Reference.Values.TICKS_PER_SECOND * 2, Reference.Values.TICKS_PER_SECOND * 10)).setToInterrupt(Checks.HAS_TARGET))));
	}
	
	protected void registerGoals()
	{
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
	}
	
	public void customServerAiStep()
	{
		behaviourTree.tick(this);
	}
	
	public BehaviourTree getTree() { return this.behaviourTree; }
}
