package com.example.examplemod.entity;

import com.example.examplemod.entity.ai.Actions;
import com.example.examplemod.entity.ai.BehaviourTree;
import com.example.examplemod.entity.ai.Branches;
import com.example.examplemod.entity.ai.Checks;
import com.example.examplemod.entity.ai.TreeNode.*;
import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.reference.Reference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

public class TestEntity extends PathfinderMob
{
	public static final EntityDataAccessor<CompoundTag> TREE_STATUS = SynchedEntityData.defineId(TestEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private final Whiteboard<PathfinderMob> whiteboard;
	private final BehaviourTree behaviourTree;
	
	public TestEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_)
	{
		super(p_21683_, p_21684_);
		
		whiteboard = new MobWhiteboard<PathfinderMob>(this);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_SWORD, Whiteboard.Expansions::getBestSword);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_HEAD, Whiteboard.Expansions::getBestHead);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_CHEST, Whiteboard.Expansions::getBestChest);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_LEGS, Whiteboard.Expansions::getBestLegs);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_FEET, Whiteboard.Expansions::getBestFeet);
		
		behaviourTree = new BehaviourTree("main_tree", whiteboard, 
			Selector.root(
				Sequence.reactive(
					new Condition(Checks.HAS_TARGET).setCustomName("has_attack_target"), 
					Actions.LookAtConstant.normal(MobWhiteboard.MOB_TARGET),
					new Selector(
						Branches.attackRanged(),
						Branches.attackMelee())).setCustomName("combat_logic"),
				Sequence.reactive(
					Decorator.inverter(new Condition(Checks.HAS_TARGET)).setCustomName("no_attack_target"),
					new Selector(
						Branches.equipBestGear(0.125D),
						Parallel.any(
							Branches.lookRandom(Reference.Values.TICKS_PER_SECOND * 2, Reference.Values.TICKS_PER_SECOND * 10),
							Branches.wander()).setCustomName("wander"))).setCustomName("idle")));
	}
	
	protected void registerGoals()
	{
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
	}
	
	public void customServerAiStep()
	{
		behaviourTree.tick(this);
		
		CompoundTag data = new CompoundTag();
		behaviourTree.save(data);
		getEntityData().set(TREE_STATUS, data);
	}
	
	public BehaviourTree getTree()
	{
		if(getLevel().isClientSide())
			behaviourTree.load(getEntityData().get(TREE_STATUS));
		return this.behaviourTree;
	}
	
	protected void defineSynchedData()
	{
		super.defineSynchedData();
		this.entityData.define(TREE_STATUS, new CompoundTag());
	}
}
