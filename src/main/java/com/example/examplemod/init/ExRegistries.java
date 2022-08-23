package com.example.examplemod.init;

import java.util.HashMap;
import java.util.Map;

import com.example.examplemod.entity.ai.Branches;
import com.example.examplemod.entity.ai.SelectorSmart.SmartNode;
import com.example.examplemod.entity.ai.SelectorSmart.TreeNodeSmart;
import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ExRegistries
{
	public static final Map<Item, TreeNodeSmart> ITEM_BRANCH_REGISTRY	= new HashMap<>();
	
	private static final SmartNode rangeAttackSimple = (mob, storage) -> 
	{
		Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
		return mob.distanceTo(target) / 7F;
	};
	private static final SmartNode meleeAttackSimple = (mob, storage) -> 
	{
		Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
		return (1F - (float)Math.pow(mob.distanceTo(target) / 5, 4)) * mob.getHealth() / mob.getMaxHealth();
	};
	
	public static final TreeNodeSmart BASIC_MELEE = TreeNodeSmart.smart(meleeAttackSimple, Branches.attackMelee());
	
	private static void registerBranch(Item itemIn, TreeNodeSmart branchIn)
	{
		ITEM_BRANCH_REGISTRY.put(itemIn, branchIn);
	}
	
	static
	{
		registerBranch(Items.BOW, TreeNodeSmart.smart(rangeAttackSimple, Branches.attackRangeBow()));
		registerBranch(Items.CROSSBOW, TreeNodeSmart.smart(rangeAttackSimple, Branches.attackRangeCrossbow()));
		registerBranch(Items.SPLASH_POTION, TreeNodeSmart.smart((mob, storage) -> 
		{
			Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
			float dist = mob.distanceTo(target);
			return dist > 3F ? dist / 7F : 0F;
		}, Branches.attackSplashPotion()));
		registerBranch(Items.ENDER_PEARL, TreeNodeSmart.smart((mob, storage) ->
		{
			Entity target = storage.getEntity(MobWhiteboard.MOB_TARGET);
			return mob.distanceTo(target) > 4F ? mob.getHealth() / mob.getMaxHealth() : 0F;
		}, Branches.throwEnderPearl()));
		registerBranch(Items.TRIDENT, TreeNodeSmart.smart(rangeAttackSimple, Branches.throwTrident()));
	}
}
