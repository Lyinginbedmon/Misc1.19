package com.example.examplemod.deities;

import java.util.List;
import java.util.Random;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.network.chat.Component;

public class Deity
{
	private final String simpleName;
	private final Component displayName;
	private final List<Domain> domains = Lists.newArrayList();
	private final long randSeed;
	
	private Random rand;
	
	public Deity(String simpleNameIn, Component nameIn, long seed, Domain... domainsIn)
	{
		this.simpleName = simpleNameIn;
		this.displayName = nameIn;
		this.randSeed = seed;
		this.rand = new Random(seed);
		
		for(int i=0; i<domainsIn.length; i++)
			domains.add(domainsIn[i]);
	}
	
	public String simpleName() { return this.simpleName; }
	
	public Component displayName() { return this.displayName; }
	
	public List<Domain> domains() { return this.domains; }
	
	public Random getRandom() { return this.rand; }
	
	public static Deity fromJson(String name, JsonElement json)
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
		
		Domain[] domains = {Domain.Good};
		if(object.has("Domains"))
		{
			JsonArray domainArray = object.get("Domains").getAsJsonArray();
			List<Domain> domainList = Lists.newArrayList();
			for(int i=0; i<domainArray.size(); i++)
			{
				Domain domain = Domain.fromName(domainArray.get(i).getAsString());
				if(domain != null)
					domainList.add(domain);
			}
			domains = domainList.toArray(new Domain[0]);
		}
		
		long seed = 0;
		if(object.has("Seed"))
			seed = object.get("Seed").getAsLong();
		
		return new Deity(name, displayName, seed, domains);
	}
	
	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		
		json.addProperty("DisplayName", Component.Serializer.toJson(this.displayName));
		json.addProperty("Seed", this.randSeed);
		JsonArray domains = new JsonArray();
			this.domains.forEach((domain) -> domains.add(domain.getSerializedName()));
		json.add("Domains", domains);
		
		return json;
	}
}
