package com.example.examplemod.init;

import com.example.examplemod.deities.Miracle;
import com.example.examplemod.reference.Reference;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class MiracleTags
{
	public static final TagKey<Miracle> AIR = makeTag("air");
	public static final TagKey<Miracle> ANIMAL = makeTag("animal");
	public static final TagKey<Miracle> CHAOS = makeTag("chaos");
	public static final TagKey<Miracle> CREATION = makeTag("creation");
	public static final TagKey<Miracle> DARKNESS = makeTag("darkness");
	public static final TagKey<Miracle> DEATH = makeTag("death");
	public static final TagKey<Miracle> DESTRUCTION = makeTag("destruction");
	public static final TagKey<Miracle> EARTH = makeTag("earth");
	public static final TagKey<Miracle> EVIL = makeTag("evil");
	public static final TagKey<Miracle> FIRE = makeTag("fire");
	public static final TagKey<Miracle> GOOD = makeTag("good");
	public static final TagKey<Miracle> HEALING = makeTag("healing");
	public static final TagKey<Miracle> KNOWLEDGE = makeTag("knowledge");
	public static final TagKey<Miracle> LAW = makeTag("law");
	public static final TagKey<Miracle> LIGHT = makeTag("light");
	public static final TagKey<Miracle> LUCK = makeTag("luck");
	public static final TagKey<Miracle> MAGIC = makeTag("magic");
	public static final TagKey<Miracle> PLANT = makeTag("plant");
	public static final TagKey<Miracle> PROTECTION = makeTag("protection");
	public static final TagKey<Miracle> STRENGTH = makeTag("strength");
	public static final TagKey<Miracle> TRAVEL = makeTag("travel");
	public static final TagKey<Miracle> TRICKERY = makeTag("trickery");
	public static final TagKey<Miracle> WAR = makeTag("war");
	public static final TagKey<Miracle> WATER = makeTag("water");
	
	private static TagKey<Miracle> makeTag(String nameIn)
	{
		return TagKey.create(Miracle.REGISTRY_KEY, new ResourceLocation(Reference.ModInfo.MOD_ID, nameIn));
	}
}
