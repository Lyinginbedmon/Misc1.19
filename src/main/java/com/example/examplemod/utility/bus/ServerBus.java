package com.example.examplemod.utility.bus;

import com.example.examplemod.reference.Reference;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBus
{
//	public static Minecraft mc = Minecraft.getInstance();
//	public static Deity currentGod = null;
//	
//	@SubscribeEvent
//	public static void onServerTick(TickEvent.ClientTickEvent event)
//	{
//		if(currentGod == null)
//			currentGod = DeityRegistry.getInstance().getDeity("acinum");
//		
//		ExampleMod.LOG.info("Current deity: "+currentGod.displayName().getString());
//		ExampleMod.LOG.info("# Opinion: "+currentGod.opinionOf(mc.player));
//	}
}
