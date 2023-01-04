package com.example.examplemod.deities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.deities.miracle.Miracle;
import com.example.examplemod.deities.personality.PersonalityModel;
import com.example.examplemod.deities.personality.PersonalityTraits;
import com.example.examplemod.init.MiracleTags;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
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
	
	public Set<String> getDeityNames(){ return this.deities.keySet(); }
	
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
                	String domains = " [";
                	List<TagKey<Miracle>> domainList = builder.domains();
                	for(int i=0; i<domainList.size(); i++)
                	{
                		domains += domainList.get(i).location().getPath();
                		if(i < domainList.size() - 1)
                			domains += ", ";
                		else
                			domains += "]";
                	}
                	ExampleMod.LOG.info(" -Loaded: "+builder.simpleName() + domains + "["+builder.miracles().size()+"]");
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
	
	@SafeVarargs
	private static void addDefault(String simple, Component name, PersonalityModel personality, long seed, TagKey<Miracle>... domainsIn)
	{
		List<TagKey<Miracle>> domains = Lists.newArrayList();
		for(int i=0; i<domainsIn.length; i++)
			domains.add(domainsIn[i]);
		DEFAULT_DEITIES.add(new Deity(simple, name, personality, seed, domains));
	}
	
	@SafeVarargs
	private static void addDefault(String simple, String name, PersonalityModel personality, long seed, TagKey<Miracle>... domains)
	{
		addDefault(simple, Component.literal(name), personality, seed, domains);
	}
	
	@SafeVarargs
	private static void addDefault(String simple, String name, long seed, TagKey<Miracle>... domains)
	{
		addDefault(simple, Component.literal(name), new PersonalityModel(), seed, domains);
	}
	
	public static List<Deity> getDefaultDeities() { return DEFAULT_DEITIES; }
	
	static
	{
		addDefault("acinum", "Acinum the Water Bringer", new PersonalityModel(List.of(PersonalityTraits.ZOOLATER, PersonalityTraits.SEAFARER)), 46654, MiracleTags.WATER);
		addDefault("aeneas", "Aeneas the Builder of Vessels", new PersonalityModel(List.of(PersonalityTraits.WANDERER, PersonalityTraits.INVENTOR)), 5388, MiracleTags.TRAVEL, MiracleTags.CREATION, MiracleTags.WATER);
		addDefault("basilla", "Basilla the Firm", new PersonalityModel(List.of(PersonalityTraits.HOMEBODY, PersonalityTraits.AGRIPHOBIC, PersonalityTraits.INVENTOR)), 5391, MiracleTags.PROTECTION, MiracleTags.CREATION, MiracleTags.EARTH);
		addDefault("erinus", "Erinus the Ever-Green", new PersonalityModel(List.of(PersonalityTraits.BRIGHT, PersonalityTraits.DRUID, PersonalityTraits.ZOOLATER)), 66091, MiracleTags.ANIMAL, MiracleTags.PLANT);
		addDefault("etronicus", "Etronicus of the Graven Mists", new PersonalityModel(List.of(PersonalityTraits.SHADOW, PersonalityTraits.PESSIMIST)), 34031, MiracleTags.DEATH, MiracleTags.DARKNESS);
		addDefault("flying", "Flying of the Parallel", 17218, MiracleTags.LAW);
		addDefault("moriboca", "Moriboca the Furious", new PersonalityModel(List.of(PersonalityTraits.BRUTE)), 77384, MiracleTags.FIRE, MiracleTags.STRENGTH, MiracleTags.WAR);
		addDefault("philopos", "Philopos the All-Watching", 85134, MiracleTags.LIGHT, MiracleTags.KNOWLEDGE);
		addDefault("phoenix", "Phoenix of the Bow", 19214, MiracleTags.AIR, MiracleTags.TRAVEL);
		addDefault("placitos", "Placitos the Englightened", 66732, MiracleTags.MAGIC, MiracleTags.KNOWLEDGE);
		addDefault("urlin", "Urlin of Cloven Feet", 2847, MiracleTags.ANIMAL, MiracleTags.CHAOS);
		addDefault("lying", "The Lying Vision", 2847, MiracleTags.KNOWLEDGE, MiracleTags.TRICKERY);
	}
}
