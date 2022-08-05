package com.example.examplemod.init;

import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.ModInfo.MOD_ID);
    
//  public static final RegistryObject<EntityType<EntityGrizzlyBear>> GRIZZLY_BEAR = DEF_REG.register("grizzly_bear", () -> registerEntity(EntityType.Builder.of(EntityGrizzlyBear::new, MobCategory.CREATURE).sized(1.6F, 1.8F), "grizzly_bear"));
	@SuppressWarnings("unchecked")
	public static final RegistryObject<EntityType<TestEntity>> TEST	= ENTITIES.register("test", () -> registerEntity(EntityType.Builder.of(TestEntity::new, MobCategory.CREATURE).sized(0.6F, 1.5F), "test"));//, 1, 1);
	
//	@SuppressWarnings("deprecation")
//	private static <T extends Mob> EntityType<T> register(String name, EntityType.Builder<T> builder, int primaryColor, int secondaryColor)
//	{
//		EntityType<T> type = register(name, builder);
//		ExItems.register(name, new SpawnEggItem(type, primaryColor, secondaryColor, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
//		return register(name, builder);
//	}
	
	@SuppressWarnings("unused")
	private static <T extends Entity> RegistryObject<EntityType<?>> register(String name, EntityType.Builder<T> builder)
	{
		return ENTITIES.register(name, () -> (EntityType<?>)builder.build(name));
	}
	
	@SuppressWarnings("rawtypes")
	private static final EntityType registerEntity(EntityType.Builder builder, String entityName)
	{
		return (EntityType)builder.build(entityName);
	}
	
	@SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
    	event.put(TEST.get(), Monster.createMonsterAttributes().build());
    }
}
