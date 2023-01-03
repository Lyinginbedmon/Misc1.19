package com.example.examplemod.deities;

import java.util.List;
import java.util.Random;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.personality.PersonalityModel;
import com.example.examplemod.init.ExRegistries;
import com.example.examplemod.init.MiracleTags;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

public class Deity
{
	private final String simpleName;
	private final Component displayName;
	private final List<TagKey<Miracle>> domains = Lists.newArrayList();
	private final long randSeed;
	
	private final PersonalityModel personality;
	
	private Random rand;
	
	public Deity(String simpleNameIn, Component nameIn, PersonalityModel personalityIn, long seed, List<TagKey<Miracle>> domainsIn)
	{
		this.simpleName = simpleNameIn;
		this.displayName = nameIn;
		this.personality = personalityIn;
		this.randSeed = seed;
		this.rand = new Random(seed);
		this.domains.addAll(domainsIn);
	}
	public Deity(String simpleNameIn, Component nameIn, long seed, List<TagKey<Miracle>> domainsIn)
	{
		this(simpleNameIn, nameIn, new PersonalityModel(), seed, domainsIn);
	}
	
	public String simpleName() { return this.simpleName; }
	
	public Component displayName() { return this.displayName; }
	
	public List<TagKey<Miracle>> domains() { return this.domains; }
	
	public List<Miracle> miracles()
	{
		List<Miracle> miracles = Lists.newArrayList();
		for(TagKey<Miracle> domain : domains())
		{
			for(RegistryObject<Miracle> miracle : ExRegistries.MIRACLES.getEntries())
				if(miracle.isPresent() && miracle.get().is(domain) && !miracles.contains(miracle.get()))
					miracles.add(miracle.get());
		}
		return miracles;
	}
	
	public double opinionOf(Player playerIn) { return this.personality.currentOpinion(playerIn); }	
	
	public Random getRandom() { return this.rand; }
	
	public static Deity fromJson(String name, JsonElement json) throws JsonSyntaxException
	{
		if(json == null)
			return null;
		JsonObject object = json.getAsJsonObject();
		
		Component displayName = Component.literal(name);
		if(object.has("CustomName"))
		{
			String s = object.get("CustomName").getAsString();
			try
			{
				displayName = Component.Serializer.fromJson(s);
			}
			catch (Exception exception)
			{
				ExampleMod.LOG.warn("Failed to parse deity display name {}", s, exception);
			}
		}
		
		PersonalityModel personality = new PersonalityModel();
		if(object.has("Personality"))
			personality = PersonalityModel.fromJson(object.get("Personality").getAsJsonObject());
		
		List<TagKey<Miracle>> domains = Lists.newArrayList();
		if(object.has("Domains"))
		{
			JsonArray domainArray = object.get("Domains").getAsJsonArray();
			for(int i=0; i<domainArray.size(); i++)
			{
				String s = domainArray.get(i).getAsString();
				if(s.startsWith("#"))
				{
					ResourceLocation registryName = new ResourceLocation(s.substring(1));
					TagKey<Miracle> domain = TagKey.create(Miracle.REGISTRY_KEY, registryName);
					if(domain != null)
						domains.add(domain);
				}
				else
					throw new JsonSyntaxException("Unknown domain tag '" + s);
			}
		}
		else
			domains = Lists.newArrayList(MiracleTags.GOOD);
		
		long seed = 0;
		if(object.has("Seed"))
			seed = object.get("Seed").getAsLong();
		
		return new Deity(name, displayName, personality, seed, domains);
	}
	
	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		
		json.addProperty("DisplayName", Component.Serializer.toJson(this.displayName));
		json.add("Personality", this.personality.toJson());
		json.addProperty("Seed", this.randSeed);
		JsonArray domains = new JsonArray();
			this.domains.forEach((domain) -> domains.add("#" + domain.location().toString()));
		json.add("Domains", domains);
		return json;
	}
}
