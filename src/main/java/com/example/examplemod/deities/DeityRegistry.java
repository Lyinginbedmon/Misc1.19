package com.example.examplemod.deities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class DeityRegistry extends SimpleJsonResourceReloadListener
{
	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	
	private static DeityRegistry instance;
	
	public DeityRegistry()
	{
		super(GSON, "deities");
	}
	
	public static DeityRegistry getInstance()
	{
		if(instance == null)
			instance = new DeityRegistry();
		return instance;
	}
	
	private static final List<Deity> DEFAULT_DEITIES = Lists.newArrayList();
	
	private Map<String, Deity> deities = new HashMap<>();
	
	public Deity getDeity(String simpleName) { return deities.getOrDefault(simpleName, null); }
	
	public void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager manager, ProfilerFiller filler)
	{
		ExampleMod.LOG.info("Attempting to load deities from data, entries: "+objectIn.size());
		Map<String, Deity> loaded = new HashMap<>();
		objectIn.forEach((name, json) -> {
            try
            {
            	Deity builder = Deity.fromJson(name.getPath(), json);
                if(builder != null)
                {
                	ExampleMod.LOG.info(" -Loaded: "+builder.simpleName());
                    loaded.put(builder.simpleName(), builder);
                }
            }
            catch (IllegalArgumentException | JsonParseException e)
            {
            	ExampleMod.LOG.error("Failed to load deity {}: {}", name);
            }
            catch(Exception e)
            {
            	ExampleMod.LOG.error("Unrecognised error loading deity {}", name);
            }
        });
		
		// If no gods were found in the datapack, load the defaults
		if(loaded.isEmpty())
		{
			ExampleMod.LOG.warn("No deities found, loading defaults");
			DEFAULT_DEITIES.forEach((god) -> loaded.put(god.simpleName(), god));
		}
		
		deities.clear();
		loaded.forEach((name,deity) -> deities.put(name, deity));
	}
	
	private static void addDefault(String simple, Component name, long seed, Domain... domains)
	{
		DEFAULT_DEITIES.add(new Deity(simple, name, seed, domains));
	}
	
	private static void addDefault(String simple, String name, long seed, Domain... domains)
	{
		addDefault(simple, Component.literal(name), seed, domains);
	}
	
	public static List<Deity> getDefaultDeities() { return DEFAULT_DEITIES; }
	
	static
	{
		addDefault("acinum", "Acinum the Water Bringer", 46654, Domain.Water);
		addDefault("aeneas", "Aeneas the Builder of Vessels", 5388, Domain.Travel, Domain.Crafting, Domain.Water);
		addDefault("basilla", "Basilla the Firm", 5391, Domain.Protection, Domain.Crafting, Domain.Earth);
		addDefault("erinus", "Erinus the Ever-Green", 66091, Domain.Animal, Domain.Plant);
		addDefault("etronicus", "Etronicus of the Graven Mists", 34031, Domain.Death, Domain.Darkness);
		addDefault("flying", "Flying of the Parallel", 17218, Domain.Law);
		addDefault("moriboca", "Moriboca the Furious", 77384, Domain.Fire, Domain.War);
		addDefault("philopos", "Philopos the All-Watching", 85134, Domain.Sun, Domain.Knowledge);
		addDefault("phoenix", "Phoenix of the Bow", 19214, Domain.Air, Domain.Travel);
		addDefault("placitos", "Placitos the Englightened", 66732, Domain.Magic, Domain.Knowledge);
		addDefault("urlin", "Urlin of Cloven Feet", 2847, Domain.Animal, Domain.Chaos);
		addDefault("lying", "The Lying Vision", 2847, Domain.Knowledge, Domain.Trickery);
	}
}
