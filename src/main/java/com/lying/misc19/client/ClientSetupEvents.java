package com.lying.misc19.client;

import com.lying.misc19.client.gui.screen.ScreenSandbox;
import com.lying.misc19.client.renderer.entity.PendulumLayer;
import com.lying.misc19.init.M19Items;
import com.lying.misc19.init.M19Menus;
import com.lying.misc19.reference.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetupEvents
{
    @SubscribeEvent
	public static void clientInit(final FMLClientSetupEvent event)
	{
		event.enqueueWork(() ->
		{
//    		ItemBlockRenderTypes.setRenderLayer(VGBlocks.HOURGLASS_ALTAR.get(), RenderType.cutout());
			
//        	MinecraftForge.EVENT_BUS.register(ClientBus.class);
			
			ItemProperties.register(M19Items.PENDULUM.get(), new ResourceLocation(Reference.ModInfo.MOD_ID, "cast"), (stack, world, entity, slot) ->
			{
				return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F;
			});
			
			MenuScreens.register(M19Menus.SANDBOX_MENU.get(), ScreenSandbox::new);
		});
	}
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@SubscribeEvent
    public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event)
    {
    	EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    	for(EntityRenderer<? extends Player> renderer : dispatcher.getSkinMap().values())
    	{
    		PlayerRenderer value = (PlayerRenderer)renderer;
    		value.addLayer((PendulumLayer)(new PendulumLayer<>(value)));
    	}
    }
}
