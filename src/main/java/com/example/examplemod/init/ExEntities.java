package com.example.examplemod.init;

import com.example.examplemod.entities.EntityGuardSkeleton;
import com.example.examplemod.entities.EntityGuardZombie;
import com.example.examplemod.entities.EntityHearthLight;
import com.example.examplemod.reference.Reference;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ExEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.ModInfo.MOD_ID);
    
    public static final RegistryObject<EntityType<EntityHearthLight>> HEARTH_LIGHT = 	register("hearth_light",	EntityType.Builder.<EntityHearthLight>of(EntityHearthLight::new, MobCategory.AMBIENT).sized(0.5F, 0.5F).clientTrackingRange(16));
    public static final RegistryObject<EntityType<EntityGuardZombie>> GUARD_ZOMBIE =	register("guard_zombie",	EntityType.Builder.<EntityGuardZombie>of(EntityGuardZombie::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityGuardSkeleton>> GUARD_SKELETON =	register("guard_skeleton", EntityType.Builder.<EntityGuardSkeleton>of(EntityGuardSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).clientTrackingRange(8));
    
	private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder)
	{
		return ENTITIES.register(name, () -> builder.build(Reference.ModInfo.MOD_PREFIX + name));
	}
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
    	event.put(GUARD_ZOMBIE.get(), EntityGuardZombie.createAttributes().build());
    	event.put(GUARD_SKELETON.get(), EntityGuardSkeleton.createAttributes().build());
    	event.put(HEARTH_LIGHT.get(), EntityHearthLight.createLivingAttributes().build());
    }
}
