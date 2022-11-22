package com.example.examplemod;

import org.slf4j.Logger;

import com.example.examplemod.client.ActionRenderManager;
import com.example.examplemod.client.GroupRenderer;
import com.example.examplemod.entity.ai.group.GroupType;
import com.example.examplemod.entity.ai.group.action.ActionType;
import com.example.examplemod.init.ExCommands;
import com.example.examplemod.init.ExEntities;
import com.example.examplemod.init.ExItems;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.proxy.ClientProxy;
import com.example.examplemod.proxy.CommonProxy;
import com.example.examplemod.proxy.ServerProxy;
import com.example.examplemod.reference.Reference;
import com.example.examplemod.utility.bus.ClientBus;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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
import net.minecraftforge.registries.RegistryObject;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Reference.ModInfo.MOD_ID)
public class ExampleMod
{
    // Directly reference a slf4j logger
    public static final Logger LOG = LogUtils.getLogger();
    
    @SuppressWarnings("deprecation")
	public static CommonProxy PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Reference.ModInfo.MOD_ID);
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));

    public ExampleMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ClientBus::registerOverlayEvent);
        
        ExEntities.ENTITIES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ExItems.ITEMS.register(modEventBus);
        GroupType.init();
        ActionType.init();
        PROXY.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOG.info("HELLO FROM COMMON SETUP");
        PacketHandler.init();
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
    	ExCommands.init(event);
    }
    
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
    	@OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        	MinecraftForge.EVENT_BUS.register(ClientBus.class);
        	MinecraftForge.EVENT_BUS.addListener(GroupRenderer::renderGroups);
        	ActionRenderManager.init();
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
