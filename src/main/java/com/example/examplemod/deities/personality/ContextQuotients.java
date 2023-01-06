package com.example.examplemod.deities.personality;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.example.examplemod.capabilities.PlayerData;
import com.example.examplemod.data.ExBlockTags;
import com.example.examplemod.data.ExEntityTags;
import com.example.examplemod.data.ExItemTags;
import com.example.examplemod.init.ExRegistries;
import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class ContextQuotients
{
	public static RegistryObject<ContextQuotient> STATIC = register("static", () -> ContextQuotient.staticValue(1D));
	
	// Values populated by events and stored inside player data
	public static RegistryObject<ContextQuotient> DAMAGE_GIVEN = register("damage_given", () -> playerValue(new ResourceLocation("examplemod:damage_given"), 100D));
	public static RegistryObject<ContextQuotient> MELEE = register("melee", () -> playerValue(new ResourceLocation("examplemod:melee"), 100D));
	public static RegistryObject<ContextQuotient> ARCHERY = register("archery", () -> playerValue(new ResourceLocation("examplemod:archery"), 100D));
	public static RegistryObject<ContextQuotient> DAMAGE_TAKEN = register("damage_taken", () -> playerValue(new ResourceLocation("examplemod:damage_taken"), 100D));
	public static RegistryObject<ContextQuotient> CRAFTING = register("crafting", () -> playerValue(new ResourceLocation("examplemod:crafting"), 100D));
	public static RegistryObject<ContextQuotient> SMELTING = register("smelting", () -> playerValue(new ResourceLocation("examplemod:smelting"), 100D));
	public static RegistryObject<ContextQuotient> PRAYER = register("prayer", () -> ContextQuotient.staticValue(0D));	// TODO Action based on interaction with matching altar
	public static RegistryObject<ContextQuotient> EAT_MEAT = register("eating_meat", () -> playerDiet(ExItemTags.MEAT));
	public static RegistryObject<ContextQuotient> EAT_VEG = register("eating_veg", () -> playerDiet(ExItemTags.VEGETABLE));
	public static RegistryObject<ContextQuotient> EAT_FISH = register("eating_fish", () -> playerDiet(ItemTags.FISHES));
	public static RegistryObject<ContextQuotient> EAT_TABOO = register("eating_taboo", () -> playerValue(new ResourceLocation("examplemod:eating_taboo"), 12D));
	public static RegistryObject<ContextQuotient> DRINK_POTION = register("drink_potion", () -> playerValue(new ResourceLocation("examplemod:drink_potion"), 12D));
	public static RegistryObject<ContextQuotient> STATUS_STRENGTH = register("status_strength", () -> playerValue(new ResourceLocation("examplemod:status_strength"), 1000D));
	public static RegistryObject<ContextQuotient> STATUS_RESISTANCE = register("status_resistance", () -> playerValue(new ResourceLocation("examplemod:status_resistance"), 100D));
	public static RegistryObject<ContextQuotient> STATUS_POISON = register("status_poison", () -> playerValue(new ResourceLocation("examplemod:status_poison"), 100D));
	
	// Values calculated moment-to-moment
	public static RegistryObject<ContextQuotient> LEVEL = register("level", () -> (playerIn) -> { return (double)playerIn.experienceLevel / 50D; });
	public static RegistryObject<ContextQuotient> NUDITY = register("nudity", () -> wearArmour(ItemStack::isEmpty));
	public static RegistryObject<ContextQuotient> EQUIP_MAGIC = register("magic_equip", () -> equip(ItemStack::isEnchanted));
	public static RegistryObject<ContextQuotient> WEAR_LEATHER = register("wear_leather", () -> wearArmour((stack) -> stack.is(ExItemTags.LEATHER_ARMOUR)));
	public static RegistryObject<ContextQuotient> WEAR_METAL = register("wear_metal", () -> wearArmour((stack) -> stack.is(ExItemTags.METAL_ARMOUR)));
	public static RegistryObject<ContextQuotient> SOCIAL = register("social", () -> entitiesInArea((living) -> living.getType().is(ExEntityTags.PEOPLE)));
	public static RegistryObject<ContextQuotient> ANIMALS = register("animals", () -> entitiesInArea((living) -> living.getType().is(ExEntityTags.ANIMAL)));
	public static RegistryObject<ContextQuotient> UNDERGROUND = register("underground", () -> (playerIn) -> {
		Level world = playerIn.getLevel();
		double avgDepth = 0D;
		for(int x=-5; x<5; x++)
			for(int z=-5; z<5; z++)
			{
				Vec3 topOfWorld = new Vec3(playerIn.getX() + x, world.getMaxBuildHeight(), playerIn.getZ() + z);
				HitResult bottomUp = world.clip(new ClipContext(playerIn.position(), topOfWorld, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)null));
				HitResult topDown = world.clip(new ClipContext(topOfWorld, playerIn.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)null));
				avgDepth += topDown.getLocation().distanceTo(bottomUp.getLocation());
			}
		avgDepth /= 5 * 5;
		return avgDepth / 10D; });
	public static RegistryObject<ContextQuotient> AREA = register("space", () -> (playerIn) -> {
		Level world = playerIn.getLevel();
		double range = 20D;
		Vec3 eyePos = playerIn.getEyePosition();
		Vec3[] directions = 
			{
				new Vec3(1, 0, 0),
				new Vec3(0, 1, 0),
				new Vec3(1, 1, 0),
				new Vec3(0, 0, 1),
				new Vec3(1, 0, 1),
				new Vec3(0, 1, 1),
				new Vec3(1, 1, 1)
			};
		double avgDist = 0D;
		for(Vec3 dir : directions)
		{
			BlockHitResult trace1 = world.clip(new ClipContext(eyePos, eyePos.add(dir.scale(+range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
			BlockHitResult trace2 = world.clip(new ClipContext(eyePos, eyePos.add(dir.scale(-range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
			avgDist += eyePos.subtract(trace1.getLocation()).length() + eyePos.subtract(trace2.getLocation()).length();
		}
		avgDist /= directions.length * 2;
		return avgDist / range; });
	public static RegistryObject<ContextQuotient> NATURE = register("nature", () -> blocksInArea((state) -> state.is(ExBlockTags.NATURE) || state.getFluidState().is(FluidTags.WATER)));
	public static RegistryObject<ContextQuotient> WATER = register("water", () -> blocksInArea((state) -> state.getFluidState().is(FluidTags.WATER)));
	public static RegistryObject<ContextQuotient> FIRE = register("fire", () -> blocksInArea((state) -> state.is(ExBlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)));
	public static RegistryObject<ContextQuotient> FUNGUS = register("fungus", () -> blocksInArea((state) -> state.is(ExBlockTags.MUSHROOM)));
	public static RegistryObject<ContextQuotient> LIGHT = register("light", () -> (playerIn) -> (double)Math.max(playerIn.getLevel().getBrightness(LightLayer.SKY, playerIn.blockPosition()), playerIn.getLevel().getBrightness(LightLayer.BLOCK, playerIn.blockPosition())) / 15D);
	public static RegistryObject<ContextQuotient> TRAVEL = register("travel", () -> (playerIn) -> {
		BlockPos pos = playerIn.getLevel().isClientSide() ? playerIn.getLevel().getSharedSpawnPos() : ((ServerPlayer)playerIn).getRespawnPosition();	// FIXME Should only ever run on server side
		return pos == null ? 0D : Mth.clamp(0D, 1D, Math.sqrt(pos.distSqr(playerIn.blockPosition())) / 1000D);
	});
	
	private static RegistryObject<ContextQuotient> register(String nameIn, Supplier<ContextQuotient> quotientIn)
	{
		return ExRegistries.QUOTIENTS.register(nameIn, quotientIn);
	}
	
	public static void init() { }
	
	@Nonnull
	public static RegistryObject<ContextQuotient> getByName(ResourceLocation registryName)
	{
		for(RegistryObject<ContextQuotient> entry : ExRegistries.QUOTIENTS.getEntries())
			if(entry.isPresent() && entry.getId().equals(registryName))
				return entry;
		return STATIC;
	}
	
	private static ContextQuotient playerValue(ResourceLocation id, double upperBounds)
	{
		return new ContextQuotient.Modifiable((playerIn) -> PlayerData.getCapability(playerIn).getQuotient(id) / upperBounds);
	}
	
	private static ContextQuotient playerDiet(TagKey<Item> tag)
	{
		return new ContextQuotient.Modifiable((playerIn) -> PlayerData.getCapability(playerIn).getRecentDiet().getOrDefault(tag, 0D));
	}
	
	private static ContextQuotient equip(Predicate<ItemStack> predicateIn)
	{
		return new ContextQuotient.Modifiable((playerIn) -> {
			double tally = 0D;
			for(EquipmentSlot slot : EquipmentSlot.values())
				if(predicateIn.apply(playerIn.getItemBySlot(slot)))
					tally++;
			return tally / (double)EquipmentSlot.values().length; });
	}
	
	private static ContextQuotient wearArmour(Predicate<ItemStack> predicateIn)
	{
		return new ContextQuotient.Modifiable((playerIn) -> {
			double tally = 0D;
			double count = 0D;
			for(EquipmentSlot slot : EquipmentSlot.values())
				if(slot.getType() == EquipmentSlot.Type.ARMOR)
				{
					count++;
					if(predicateIn.apply(playerIn.getItemBySlot(slot)))
						tally++;
				}
			return tally / count; });
	}
	
	private static ContextQuotient entitiesInArea(Predicate<LivingEntity> predicateIn)
	{
		return new ContextQuotient.Modifiable((playerIn) -> {
			Level world = playerIn.getLevel();
			List<LivingEntity> matches = world.getEntitiesOfClass(LivingEntity.class, playerIn.getBoundingBox().inflate(16D), predicateIn.and((living) -> !living.equals(playerIn)));
			return Mth.clamp((double)(matches.size()) / 10D, 0D, 1D); });
	}
	
	private static ContextQuotient blocksInArea(Predicate<BlockState> predicateIn)
	{
		return new ContextQuotient.Modifiable((playerIn) -> {
			AABB bounds = playerIn.getBoundingBox().inflate(5D);
			Level world = playerIn.getLevel();
			double water = 0D, total = 0D;
			for(double y=bounds.minY; y<bounds.maxY; y++)
				for(double x=bounds.minX; x<bounds.maxX; x++)
					for(double z=bounds.minZ; z<bounds.maxZ; z++)
					{
						BlockPos pos = new BlockPos(x, y, z);
						if(!world.isEmptyBlock(pos))
						{
							total++;
							if(predicateIn.apply(world.getBlockState(pos)))
								water++;
						}
					}
			return total > 0 ? water / total : 0D; });
	}
}