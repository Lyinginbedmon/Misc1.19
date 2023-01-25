package com.example.examplemod.utility;

import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ExUtils
{
	@Nullable
	public static BlockPos searchAreaFor(BlockPos pos, Level worldIn, int radiusIn, BiPredicate<BlockPos, Level> predicateIn)
	{
		AABB oldBounds = new AABB(new Vec3(0, 0, 0), new Vec3(1, 1, 1));
		for(int radius = 0; radius < radiusIn; radius++)
		{
			for(int x=-radius; x<=radius; x++)
				for(int z=-radius; z<=radius; z++)
					for(int y=-radius; y<=radius; y++)
					{
						if(radius > 0 && oldBounds.contains(new Vec3(x + 0.5D, y + 0.5D, z + 0.5D)))
							continue;
						
						BlockPos bagPos = pos.offset(x, y, z);
						if(predicateIn.test(bagPos, worldIn))
							return bagPos;
					}
			
			oldBounds = new AABB(new Vec3(-radius, -radius, -radius), new Vec3(radius + 1, radius + 1, radius + 1));
		}
		return null;
	}
}
