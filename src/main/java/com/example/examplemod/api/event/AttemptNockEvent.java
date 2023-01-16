package com.example.examplemod.api.event;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event.HasResult;

@HasResult
public class AttemptNockEvent extends PlayerEvent
{
    private final ItemStack bow;
    private final InteractionHand hand;
    private final Level level;
    private final boolean hasAmmo;
    
    public AttemptNockEvent(Player player, @NotNull ItemStack item, InteractionHand hand, Level level)
    {
        super(player);
        this.bow = item;
        this.hand = hand;
        this.level = level;
        this.hasAmmo = player.getProjectile(player.getItemInHand(hand)).isEmpty();
    }
    
    @NotNull
    public ItemStack getBow() { return this.bow.copy(); }
    public Level getLevel() { return this.level; }
    public InteractionHand getHand() { return this.hand; }
    public boolean hasAmmo() { return this.hasAmmo; }
}
