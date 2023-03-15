package com.example.examplemod;

import org.slf4j.Logger;

import com.example.examplemod.commands.CommandDeity;
import com.example.examplemod.commands.CommandMiracle;
import com.example.examplemod.data.ExDataGenerators;
import com.example.examplemod.deities.DeityRegistry;
import com.example.examplemod.deities.miracle.Miracles;
import com.example.examplemod.init.ExBlocks;
import com.example.examplemod.init.ExEnchantments;
import com.example.examplemod.init.ExEntities;
import com.example.examplemod.init.ExItems;
import com.example.examplemod.init.ExMenus;
import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.proxy.ClientProxy;
import com.example.examplemod.proxy.CommonProxy;
import com.example.examplemod.proxy.ServerProxy;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.bus.ClientBus;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Reference.ModInfo.MOD_ID)
public class ExampleMod
{
    // Directly reference a slf4j logger
    public static final Logger LOG = LogUtils.getLogger();
    
    @SuppressWarnings("deprecation")
	public static CommonProxy PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    
    public static final IEventBus EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
    
    public ExampleMod()
    {
        // Register the commonSetup method for modloading
        EVENT_BUS.addListener(this::commonSetup);
        EVENT_BUS.addListener(this::completeSetup);
        EVENT_BUS.addListener(ClientBus::registerOverlayEvent);
        EVENT_BUS.addListener(ExDataGenerators::onGatherData);
        
        ExEntities.ENTITIES.register(EVENT_BUS);
        ExBlocks.BLOCKS.register(EVENT_BUS);
        ExItems.ITEMS.register(EVENT_BUS);
        ExRegistries.registerCustom(EVENT_BUS);
        ExEnchantments.ENCHANTMENTS.register(EVENT_BUS);
		ExMenus.MENUS.register(EVENT_BUS);
        PROXY.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOG.info("HELLO FROM COMMON SETUP");
        PacketHandler.init();
    }
    
    private void completeSetup(final FMLLoadCompleteEvent event)
    {
    	Miracles.registerMiracleListeners();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOG.info("HELLO from server starting");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
    	CommandDeity.register(event.getDispatcher());
    	CommandMiracle.register(event.getDispatcher());
    }
    
    @SubscribeEvent
    public void onReloadListenersEvent(AddReloadListenerEvent event)
    {
    	event.addListener(DeityRegistry.getInstance());
    }
    
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
    	@SuppressWarnings("removal")
		@OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
    		ItemBlockRenderTypes.setRenderLayer(ExBlocks.HOURGLASS_ALTAR.get(), RenderType.cutout());
    		ItemBlockRenderTypes.setRenderLayer(ExBlocks.BONE_ALTAR.get(), RenderType.cutout());
    		
        	MinecraftForge.EVENT_BUS.register(ClientBus.class);
        	PROXY.clientInit();
            // Some client setup code
            LOG.info("HELLO FROM CLIENT SETUP");
            LOG.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
        
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void registerKeybindings(RegisterKeyMappingsEvent event)
        {
        	ClientProxy.registerKeyMappings(event);
        }
    }
}
