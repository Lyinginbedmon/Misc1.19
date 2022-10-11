package com.example.examplemod.init;

import java.util.HashMap;
import java.util.Map;

import com.example.examplemod.entity.ai.Whiteboard.MobWhiteboard;
import com.example.examplemod.entity.ai.tree.Branches;
import com.example.examplemod.entity.ai.tree.SelectorSmart.SmartNode;
import com.example.examplemod.entity.ai.tree.SelectorSmart.TreeNodeSmart;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ExRegistries
{
	public static final Map<Item, TreeNodeSmart> ITEM_BRANCH_REGISTRY	= new HashMap<>();
	
	private static final SmartNode RANGE_SIMPLE = (mob, storage) -> 
	{
		Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
		return (float)Math.pow(mob.distanceTo(target) / 10, 5);
	};
	private static final SmartNode MELEE_SIMPLE = (mob, storage) -> 
	{
		Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
		return Math.max((1F - (float)Math.pow(mob.distanceTo(target) / 5, 3)) * (mob.getHealth() / mob.getMaxHealth()), 0.01F);
	};
	
	public static final TreeNodeSmart BASIC_MELEE = TreeNodeSmart.smart(MELEE_SIMPLE, Branches.attackMelee());
	
	private static void registerBranch(Item itemIn, TreeNodeSmart branchIn) { ITEM_BRANCH_REGISTRY.put(itemIn, branchIn); }
	
	static
	{
		// Item behaviour tree branches
		registerBranch(Items.BOW, TreeNodeSmart.smart(RANGE_SIMPLE, Branches.attackRangeBow()));
		registerBranch(Items.CROSSBOW, TreeNodeSmart.smart(RANGE_SIMPLE, Branches.attackRangeCrossbow()));
		registerBranch(Items.SPLASH_POTION, TreeNodeSmart.smart((mob, storage) -> 
		{
			Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
			return (float)Math.sin(3.5F + Math.min(16F, mob.distanceTo(target)) * 0.5F);
		}, Branches.attackSplashPotion()));
		registerBranch(Items.ENDER_PEARL, TreeNodeSmart.smart((mob, storage) ->
		{
			Entity target = storage.getEntity(MobWhiteboard.ATTACK_TARGET);
			return mob.getHealth() > 5F ? (float)Math.sin(4F + Math.min(16F, mob.distanceTo(target)) * 0.35F) * (mob.getHealth() / mob.getMaxHealth()) : 0F;
		}, Branches.throwEnderPearl()));
		registerBranch(Items.TRIDENT, TreeNodeSmart.smart((mob, storage) -> 
		{
			return Math.max(RANGE_SIMPLE.utility(mob, storage), MELEE_SIMPLE.utility(mob, storage));
		}, Branches.throwTrident()));
	}
}
