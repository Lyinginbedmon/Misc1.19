package com.example.examplemod.deities.personality;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class ContextQuotients
{
	public static RegistryObject<ContextQuotient> STATIC = register("static", () -> (playerIn) -> 1D);
	
	// Values populated by events and stored inside player data
	public static RegistryObject<ContextQuotient> VIOLENCE = register("violence", () -> (playerIn) -> Mth.clamp(PlayerData.getCapability(playerIn).getQuotient(new ResourceLocation("examplemod:violence")) / 100D, 0D, 1D));
	public static RegistryObject<ContextQuotient> DAMAGE_TAKEN = register("damage_taken", () -> (playerIn) -> Mth.clamp(PlayerData.getCapability(playerIn).getQuotient(new ResourceLocation("examplemod:damage_taken")) / 100D, 0D, 1D));
	public static RegistryObject<ContextQuotient> CRAFTING = register("crafting", () -> (playerIn) -> 0D);
	public static RegistryObject<ContextQuotient> PRAYER = register("prayer", () -> (playerIn) -> 0D);	// TODO Action based on interaction with matching altar
	public static RegistryObject<ContextQuotient> EAT_MEAT = register("eating_meat", () -> (playerIn) -> 0D);
	public static RegistryObject<ContextQuotient> EAT_VEG = register("eating_veg", () -> (playerIn) -> 0D);
	public static RegistryObject<ContextQuotient> EAT_TABOO = register("eating_taboo", () -> (playerIn) -> 0D);
	
	// Values calculated moment-to-moment
	public static RegistryObject<ContextQuotient> NUDITY = register("nudity", () -> wearArmour(ItemStack::isEmpty));
	public static RegistryObject<ContextQuotient> EQUIP_MAGIC = register("magic_equip", () -> equip(ItemStack::isEnchanted));
	public static RegistryObject<ContextQuotient> WEAR_LEATHER = register("wear_leather", () -> wearArmour((stack) -> stack.is(ExItemTags.LEATHER_ARMOUR)));
	public static RegistryObject<ContextQuotient> WEAR_METAL = register("wear_metal", () -> wearArmour((stack) -> stack.is(ExItemTags.METAL_ARMOUR)));
	public static RegistryObject<ContextQuotient> SOCIAL = register("social", () -> entitiesInArea((living) -> living.getType().is(ExEntityTags.PEOPLE)));
	public static RegistryObject<ContextQuotient> ANIMALS = register("animals", () -> entitiesInArea((living) -> living.getType().is(ExEntityTags.ANIMAL)));
	public static RegistryObject<ContextQuotient> UNDERGROUND = register("underground", () -> (playerIn) -> {
		Level world = playerIn.getLevel();
		Vec3 topOfWorld = new Vec3(playerIn.getX(), world.getMaxBuildHeight(), playerIn.getZ());
		HitResult bottomUp = world.clip(new ClipContext(playerIn.position(), topOfWorld, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)null));
		HitResult topDown = world.clip(new ClipContext(topOfWorld, playerIn.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)null));
		return Mth.clamp(0D, 1D, topDown.getLocation().distanceTo(bottomUp.getLocation()) / 10D); });
	public static RegistryObject<ContextQuotient> NATURE = register("nature", () -> blocksInArea((state) -> (state.is(ExBlockTags.NATURE) || state.getFluidState().is(FluidTags.WATER))));
	public static RegistryObject<ContextQuotient> WATER = register("water", () -> blocksInArea((state) -> state.getFluidState().is(FluidTags.WATER)));
	public static RegistryObject<ContextQuotient> LIGHT = register("light", () -> (playerIn) -> (double)Math.max(playerIn.getLevel().getBrightness(LightLayer.SKY, playerIn.blockPosition()), playerIn.getLevel().getBrightness(LightLayer.BLOCK, playerIn.blockPosition())) / 15D);
	public static RegistryObject<ContextQuotient> TRAVEL = register("travel", () -> (playerIn) -> {
		BlockPos pos = playerIn.getLevel().isClientSide() ? playerIn.getLevel().getSharedSpawnPos() : ((ServerPlayer)playerIn).getRespawnPosition();	// FIXME Should only ever run on server side
		return Mth.clamp(0D, 1D, Math.sqrt(pos.distSqr(playerIn.blockPosition())) / 1000D);
	});
	
	private static RegistryObject<ContextQuotient> register(String nameIn, Supplier<ContextQuotient> quotientIn)
	{
		return ExRegistries.QUOTIENTS.register(nameIn, quotientIn);
	}
	
	public static void init() { }
	
	@Nullable
	public static RegistryObject<ContextQuotient> getByName(ResourceLocation registryName)
	{
		for(RegistryObject<ContextQuotient> entry : ExRegistries.QUOTIENTS.getEntries())
			if(entry.isPresent() && entry.getId().equals(registryName))
				return entry;
		return null;
	}
	
	private static ContextQuotient equip(Predicate<ItemStack> predicateIn)
	{
		return (playerIn) -> {
			double tally = 0D;
			for(EquipmentSlot slot : EquipmentSlot.values())
				if(predicateIn.apply(playerIn.getItemBySlot(slot)))
					tally++;
			return tally / (double)EquipmentSlot.values().length; };
	}
	
	private static ContextQuotient wearArmour(Predicate<ItemStack> predicateIn)
	{
		return (playerIn) -> {
			double tally = 0D;
			double count = 0D;
			for(EquipmentSlot slot : EquipmentSlot.values())
				if(slot.getType() == EquipmentSlot.Type.ARMOR)
				{
					count++;
					if(predicateIn.apply(playerIn.getItemBySlot(slot)))
						tally++;
				}
			return tally / count; };
	}
	
	private static ContextQuotient entitiesInArea(Predicate<LivingEntity> predicateIn)
	{
		return (playerIn) -> {
			Level world = playerIn.getLevel();
			List<LivingEntity> matches = world.getEntitiesOfClass(LivingEntity.class, playerIn.getBoundingBox().inflate(16D), predicateIn.and((living) -> !living.equals(playerIn)));
			return Mth.clamp((double)(matches.size()) / 10D, 0D, 1D); };
	}
	
	private static ContextQuotient blocksInArea(Predicate<BlockState> predicateIn)
	{
		return (playerIn) -> {
			AABB bounds = playerIn.getBoundingBox().inflate(5D);
			Level world = playerIn.getLevel();
			double water = 0D, total = 0D;
			for(double y=bounds.minY; y<bounds.maxY; y++)
				for(double x=bounds.minY; x<bounds.maxY; x++)
					for(double z=bounds.minY; z<bounds.maxY; z++)
					{
						BlockPos pos = new BlockPos(x, y, z);
						if(!world.isEmptyBlock(pos))
						{
							total++;
							if(predicateIn.apply(world.getBlockState(pos)))
								water++;
						}
					}
			return water / total; };
	}
}