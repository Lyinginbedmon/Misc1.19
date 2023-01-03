package com.example.examplemod.deities.personality;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class PersonalityModel
{
	List<Opinion> opinions = Lists.newArrayList();
	
	public PersonalityModel(Opinion... opinionsIn)
	{
		for(int i=0; i<opinionsIn.length; i++)
			if(opinionsIn[i] != null)
				this.opinions.add(opinionsIn[i]);
	}
	public PersonalityModel()
	{
		this(PersonalityTraits.PACIFIST.get(), PersonalityTraits.BRIGHT.get(), PersonalityTraits.CLAUSTROPHOBIC.get(), PersonalityTraits.ZOOLATER.get());
	}
	
	public double currentOpinion(Player playerIn) { return currentOpinion(new PersonalityContext(playerIn)); }
	
	public double currentOpinion(PersonalityContext contextIn)
	{
		double value = 0;
		for(Opinion opinion : this.opinions)
			value += opinion.view * contextIn.getQuotient(opinion.quotientId);
		
		return value / this.opinions.size();
	}
	
	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		
		JsonArray opinions = new JsonArray();
			this.opinions.forEach((opinion) -> opinions.add(opinion.getRegistryName().toString()));
		json.add("Traits", opinions);
		
		return json;
	}
	
	public static PersonalityModel fromJson(JsonObject json)
	{
		JsonArray opinions = json.getAsJsonArray("Traits");
		Opinion[] array = new Opinion[opinions.size()];
		for(int i=0; i<opinions.size(); i++)
		{
			ResourceLocation name = new ResourceLocation(opinions.get(i).getAsString());
			array[i] = PersonalityTraits.byRegistryName(name);
		}
		
		return new PersonalityModel(array);
	}
}
