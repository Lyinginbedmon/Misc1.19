package com.example.examplemod.entity.ai.group.action;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;

public class ActionOption
{
	private final ActionOption.ActionSupplier supplier;
	private final ActionOption.OptionPredicate utility;
	
	public ActionOption(ActionOption.OptionPredicate utilityIn, ActionOption.ActionSupplier supplierIn)
	{
		this.supplier = supplierIn;
		this.utility = utilityIn;
	}
	
	public boolean isValid(List<LivingEntity> targets, GroupAction parentAction, int supply)
	{
		return utility(targets, parentAction, supply) >= 0F;
	}
	
	public float utility(List<LivingEntity> targets, GroupAction parentAction, int supply)
	{
		return utility.test(targets, parentAction, supply);
	}
	
	public GroupAction get(GroupAction parent, int supply) { return supplier.get(parent, supply); }
	
	@FunctionalInterface
	public interface OptionPredicate
	{
		public float test(List<LivingEntity> targets, GroupAction parentAction, int supply);
	}
	
	@FunctionalInterface
	public interface ActionSupplier
	{
		public GroupAction get(GroupAction parentAction, int supply);
	}
}