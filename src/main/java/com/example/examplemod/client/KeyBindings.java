package com.example.examplemod.client;

import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.MobCommanding;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class KeyBindings
{
	private static final List<KeyMapping> KEYS = Lists.newArrayList();
	private static final String CATEGORY = "keys."+Reference.ModInfo.MOD_ID+".category";
	
	private static final String DO_COMMAND = "keys."+Reference.ModInfo.MOD_ID+".issue_command";
	private static final String DO_NOTIFY = "keys."+Reference.ModInfo.MOD_ID+".notify_target";
	
	public static final KeyMapping COMMAND = register(new KeyMapping(DO_COMMAND, KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY));
	public static final KeyMapping NOTIFY = register(new KeyMapping(DO_NOTIFY, KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
	
	private static KeyMapping register(KeyMapping binding)
	{
		KEYS.add(binding);
		return binding;
	}
	
	public static void register(Consumer<KeyMapping> consumer)
	{
		MinecraftForge.EVENT_BUS.register(new KeyBindings());
		KEYS.forEach((key) -> { consumer.accept(key); });
	}
	
	@SubscribeEvent
	public void handleInputEvent(InputEvent.Key event)
	{
		Minecraft mc = Minecraft.getInstance();
		int keyID = event.getKey();
		LocalPlayer player = mc.player;
		if(player == null || !player.isAlive() || (player.isSleeping() || player.isSleepingLongEnough()) || mc.screen != null)
			return;
		
		if(!mc.isWindowActive())
			return;
		if(keyID == COMMAND.getKey().getValue())
			switch(event.getAction())
			{
				case 0:
					if(MobCommanding.isMarking())
						MobCommanding.onMarkReleased(player);
					break;
				case 1:
					MobCommanding.onMarkPressed(player);
					break;
			}
		else if(keyID == NOTIFY.getKey().getValue())
		{
			if(event.getAction() == 1)
				MobCommanding.onNotifyPressed(player, Screen.hasShiftDown());
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void mouseScrollEvent(InputEvent.MouseScrollingEvent event)
	{
		if(MobCommanding.isMarking())
		{
			MobCommanding.incMarkIndex((int)Math.signum(event.getScrollDelta()));
			event.setCanceled(true);
		}
	}
	
	public KeyMapping getPressedKey()
	{
		for(KeyMapping key : KEYS)
			if(key.isDown())
				return key;
		
		return null;
	}
}
