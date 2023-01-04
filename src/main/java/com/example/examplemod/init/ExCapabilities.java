package com.example.examplemod.init;

import com.example.examplemod.capabilities.PlayerData;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class ExCapabilities
{
	public static final Capability<PlayerData> PLAYER_DATA	= CapabilityManager.get(new CapabilityToken<>() {});
	
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
	{
		event.register(PlayerData.class);
	}
}
