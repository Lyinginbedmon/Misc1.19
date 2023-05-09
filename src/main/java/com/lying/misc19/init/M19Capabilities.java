package com.lying.misc19.init;

import com.lying.misc19.capabilities.LivingData;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class M19Capabilities
{
	public static final Capability<LivingData> LIVING_DATA	= CapabilityManager.get(new CapabilityToken<>() {});
	
	public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event)
	{
		event.register(LivingData.class);
	}
}
