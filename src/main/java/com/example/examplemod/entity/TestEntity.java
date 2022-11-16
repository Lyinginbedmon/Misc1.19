package com.example.examplemod.entity;

import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.group.GroupSteve;
import com.example.examplemod.entity.ai.tree.Actions;
import com.example.examplemod.entity.ai.tree.BehaviourTree;
import com.example.examplemod.entity.ai.tree.Branches;
import com.example.examplemod.entity.ai.tree.NodePredicates;
import com.example.examplemod.entity.ai.tree.SelectorSmartItem;
import com.example.examplemod.entity.ai.tree.TreeNode.Condition;
import com.example.examplemod.entity.ai.tree.TreeNode.Decorator;
import com.example.examplemod.entity.ai.tree.TreeNode.Parallel;
import com.example.examplemod.entity.ai.tree.TreeNode.Selector;
import com.example.examplemod.entity.ai.tree.TreeNode.Sequence;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.GroupSaveData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

public class TestEntity extends PathfinderMob implements ITreeEntity
{
	public static final EntityDataAccessor<CompoundTag> TREE_STATUS = SynchedEntityData.defineId(TestEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private final Whiteboard<PathfinderMob> whiteboard;
	private final BehaviourTree behaviourTree;
	
	public TestEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_)
	{
		super(p_21683_, p_21684_);
		getNavigation().setCanFloat(true);
		((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
		
		whiteboard = new MobWhiteboard<PathfinderMob>(this);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_SWORD, Whiteboard.Expansions::getBestSword);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_HEAD, Whiteboard.Expansions::getBestHead);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_CHEST, Whiteboard.Expansions::getBestChest);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_LEGS, Whiteboard.Expansions::getBestLegs);
			whiteboard.addExpansion(Whiteboard.Expansions.BEST_FEET, Whiteboard.Expansions::getBestFeet);
		
		behaviourTree = new BehaviourTree("main_tree", this::getWhiteboard, 
			Selector.root(
				Branches.executeGroupCommand(),
				Sequence.reactive(
					new Condition(NodePredicates.HAS_LIVING_TARGET).setCustomName("has_attack_target"), 
					new Condition((mob, storage) -> { return !storage.hasCommands(); }),
					Selector.sequential(
						Sequence.reactive(
							new Condition((mob, storage)-> { return storage.getCounter(MobWhiteboard.MOB_TARGET_NOT_VISIBLE) <= Reference.Values.TICKS_PER_SECOND * 3; }).setCustomName("no_target_loss"),
							Sequence.reactive(
								Actions.LookAtConstant.normal(MobWhiteboard.ATTACK_TARGET),
								Branches.markTargetSighting()).setCustomName("target_vision_handling").setDiscrete(),
							new SelectorSmartItem().setCustomName("populating_selector")
							).setCustomName("combat_logic"),
						Sequence.reactive(
							Decorator.inverter(new Condition(NodePredicates.CAN_SEE_TARGET)).setCustomName("no_line_of_sight"),
							Branches.searchAroundPosition(10)).setCustomName("search_and_destroy"))).setCustomName("attack_behaviour").setDiscrete(),
				Sequence.reactive(
					Decorator.inverter(new Condition(NodePredicates.HAS_LIVING_TARGET)).setCustomName("no_attack_target"),
					Decorator.inverter(new Condition(NodePredicates.IN_GROUP)).setCustomName("no_group"),
					new Condition((mob, storage) -> { return !storage.hasCommands(); }),
					Selector.sequential(
						Branches.equipBestGear(0.125D).setDiscrete(),
						Parallel.any(
							Branches.lookRandom(Reference.Values.TICKS_PER_SECOND * 2, Reference.Values.TICKS_PER_SECOND * 10),
							Branches.wander()))).setCustomName("idle")));
	}
	
	protected void registerGoals()
	{
//		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Villager.class, false, true));
	}
	
	public void customServerAiStep()
	{
		GroupSaveData manager = GroupSaveData.get(getServer());
		if(!manager.isInAnyGroup(this))
		{
			GroupSteve group = (GroupSteve)manager.getNearestOfClass(GroupSteve.class, blockPosition(), 16D);
			if(group == null)
				manager.register(new GroupSteve(this));
			else
				group.add(this);
		}
		
		behaviourTree.tick(this);
		
		CompoundTag data = new CompoundTag();
		behaviourTree.save(data);
		getEntityData().set(TREE_STATUS, data);
	}
	
	public Whiteboard<PathfinderMob> getWhiteboard(PathfinderMob mob){ return this.whiteboard; }
	
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
