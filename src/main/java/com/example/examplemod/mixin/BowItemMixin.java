package com.example.examplemod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.examplemod.api.event.AttemptNockEvent;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event.Result;

@Mixin(BowItem.class)
public class BowItemMixin
{
	@Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", at = @At("HEAD"), cancellable = true)
	public void attemptToNock(Level world, Player player, InteractionHand hand, final CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci)
	{
		AttemptNockEvent event = new AttemptNockEvent(player, player.getItemInHand(hand), hand, world);
		MinecraftForge.EVENT_BUS.post(event);
		if(event.getResult() == Result.DENY)
			ci.setReturnValue(InteractionResultHolder.fail(event.getBow()));
	}
}
