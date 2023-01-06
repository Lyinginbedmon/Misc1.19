package com.example.examplemod.deities.personality;

import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

public class PersonalityModel
{
	List<RegistryObject<Opinion>> opinions = Lists.newArrayList();
	
	public PersonalityModel(@SuppressWarnings("unchecked") RegistryObject<Opinion>... opinionsIn)
	{
		for(int i=0; i<opinionsIn.length; i++)
			if(opinionsIn[i] != null)
				this.opinions.add(opinionsIn[i]);
	}
	public PersonalityModel(Collection<RegistryObject<Opinion>> opinionsIn)
	{
		this.opinions.addAll(opinionsIn);
	}
	@SuppressWarnings("unchecked")
	public PersonalityModel()
	{
		this(PersonalityTraits.PACIFIST, PersonalityTraits.BRIGHT, PersonalityTraits.CLAUSTROPHOBIC, PersonalityTraits.ZOOLATER);
	}
	
	public List<RegistryObject<Opinion>> getTraits(){ return this.opinions; }
	
	public double currentOpinion(Player playerIn) { return currentOpinion(new PersonalityContext(playerIn)); }
	
	public double currentOpinion(PersonalityContext contextIn)
	{
		double value = 0;
		
		for(RegistryObject<Opinion> opinion : this.opinions)
			if(opinion.isPresent())
				value += opinion.get().value(contextIn);
		
		Tuple<Double, Double> range = range();
		return (value - range.getA()) / (range.getB() - range.getA());
	}
	
	public Tuple<Double, Double> range()
	{
		double min = 0, max = 0;
		for(RegistryObject<Opinion> opinion : this.opinions)
			if(opinion.isPresent())
			{
				Tuple<Double,Double> range = opinion.get().range();
				
				double lowest = Math.min(range.getA(), range.getB());
				double highest = Math.max(range.getA(), range.getB());
				min += Math.min(lowest, 0);
				max += Math.max(highest, 0);
			}
		
		return new Tuple<Double,Double>(min, max);
	}
	
	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		
		JsonArray opinions = new JsonArray();
			this.opinions.forEach((opinion) -> opinions.add(opinion.getId().toString()));
		json.add("Traits", opinions);
		
		return json;
	}
	
	public static PersonalityModel fromJson(JsonObject json)
	{
		JsonArray opinions = json.getAsJsonArray("Traits");
		List<RegistryObject<Opinion>> list = Lists.newArrayList();
		for(int i=0; i<opinions.size(); i++)
			list.add(PersonalityTraits.byRegistryName(new ResourceLocation(opinions.get(i).getAsString())));
		
		return new PersonalityModel(list);
	}
}
