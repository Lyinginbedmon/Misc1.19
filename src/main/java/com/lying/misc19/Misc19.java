package com.lying.misc19;

import org.slf4j.Logger;

import com.lying.misc19.client.ClientSetupEvents;
import com.lying.misc19.init.M19Entities;
import com.lying.misc19.init.M19Items;
import com.lying.misc19.init.SpellComponents;
import com.lying.misc19.reference.Reference;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Reference.ModInfo.MOD_ID)
public class Misc19
{
    // Directly reference a slf4j logger
    public static final Logger LOG = LogUtils.getLogger();
    
    public static final IEventBus EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.ModInfo.MOD_ID);

    public Misc19()
    {
    	DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
		{
			EVENT_BUS.register(ClientSetupEvents.class);
		});
    	
        EVENT_BUS.addListener(this::commonSetup);
        
        M19Entities.ENTITIES.register(EVENT_BUS);
        M19Items.ITEMS.register(EVENT_BUS);
        
        SpellComponents.COMPONENTS.register(EVENT_BUS);
        EVENT_BUS.addListener(SpellComponents::reportInit);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOG.info("HELLO FROM COMMON SETUP");
        LOG.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOG.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOG.info("HELLO FROM CLIENT SETUP");
            LOG.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
