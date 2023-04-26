package com.lying.misc19.utility;

import net.minecraft.world.phys.Vec2;

public class M19Utils
{
	public static Vec2 rotate(Vec2 vec, double degrees)
	{
		double rads = Math.toRadians(degrees);
		return new Vec2((float)(vec.x * Math.cos(rads) - vec.y * Math.sin(rads)), (float)(vec.y * Math.cos(rads) + vec.x * Math.sin(rads)));
	}
}
