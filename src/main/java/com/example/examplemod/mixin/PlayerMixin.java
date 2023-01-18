package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.examplemod.api.event.PlayerEnchantEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

@Mixin(Player.class)
public class PlayerMixin
{
	@Inject(method = "onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("RETURN"))
	public void onEnchant(ItemStack stack, int level, final CallbackInfo ci)
	{
		MinecraftForge.EVENT_BUS.post(new PlayerEnchantEvent((Player)(Object)this, stack, level));
	}
}
