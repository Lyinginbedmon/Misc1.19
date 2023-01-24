package com.example.examplemod.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.examplemod.api.event.FortuneLevelEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;

@Mixin(ApplyBonusCount.class)
public class ApplyBonusCountMixin
{
	private static final List<LootContextParam<?>> neededParams = List.of(LootContextParams.THIS_ENTITY, LootContextParams.BLOCK_STATE);
	
	@Shadow
	Enchantment enchantment;
	
	@Shadow
	ApplyBonusCount.Formula formula;
	
	@Inject(method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	public void run(ItemStack stack, LootContext context, final CallbackInfoReturnable<ItemStack> ci)
	{
		if(enchantment != Enchantments.BLOCK_FORTUNE)
			return;
		
		for(LootContextParam<?> param : neededParams)
			if(!context.hasParam(param) || context.getParamOrNull(param) == null)
				return;
		
		Entity entity = context.getParam(LootContextParams.THIS_ENTITY);
		if(entity instanceof LivingEntity)
		{
			LivingEntity living = (LivingEntity)entity;
			BlockState state = context.getParam(LootContextParams.BLOCK_STATE);
			ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
			
			FortuneLevelEvent event = new FortuneLevelEvent(living, state, tool, EnchantmentHelper.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE, living));
			
			int fortune = MinecraftForge.EVENT_BUS.post(event) ? 0 : event.getFortuneLevel();
			int amount = formula.calculateNewCount(context.getRandom(), stack.getCount(), fortune);
			stack.setCount(amount);
			ci.setReturnValue(stack);
		}
	}
}
