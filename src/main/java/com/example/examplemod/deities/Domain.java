package com.example.examplemod.deities;

import net.minecraft.util.StringRepresentable;

public enum Domain implements StringRepresentable 
{
	Air,
	Animal,
	Chaos,
	Crafting,
	Darkness,
	Death,
	Destruction,
	Earth,
	Evil,
	Fire,
	Good,
	Healing,
	Knowledge,
	Law,
	Luck,
	Magic,
	Plant,
	Protection,
	Strength,
	Sun,
	Travel,
	Trickery,
	War,
	Water;

	@Override
	public String getSerializedName(){ return name().toLowerCase(); }
	
	public static Domain fromName(String nameIn)
	{
		for(Domain type : Domain.values())
			if(type.getSerializedName().equalsIgnoreCase(nameIn))
				return type;
		return null;
	}
}
