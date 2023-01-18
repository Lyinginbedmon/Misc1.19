package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.examplemod.api.event.PlayerEnchantItemEvent;

import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

@Mixin(EnchantedItemTrigger.class)
public class EnchantedItemTriggerMixin
{
	@Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;I)V", at = @At("RETURN"))
	public void onEnchantedItemTrigger(ServerPlayer player, ItemStack stack, int level, final CallbackInfo ci)
	{
		MinecraftForge.EVENT_BUS.post(new PlayerEnchantItemEvent(player, stack, level));
	}
}
