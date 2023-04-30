package com.lying.misc19.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;

/** Helper class for rendering */
public class Quad
{
	private final Line ab, bc, cd, da;
	
	public Quad(Vec2 a, Vec2 b, Vec2 c, Vec2 d)
	{
		this.ab = new Line(a, b);
		this.bc = new Line(b, c);
		this.cd = new Line(c, d);
		this.da = new Line(d, a);
	}
	
	public Vec2 a() { return this.ab.getA(); }
	public Vec2 b() { return this.ab.getB(); }
	public Vec2 c() { return this.cd.getA(); }
	public Vec2 d() { return this.cd.getB(); }
	
	public Line ab(){ return ab; }
	public Line bc(){ return bc; }
	public Line cd(){ return cd; }
	public Line da(){ return da; }
	
	/** Returns true if the given quad intersects with this one */
	public boolean intersects(Quad quadB)
	{
		Line[] lines = new Line[] {quadB.ab, quadB.bc, quadB.cd, quadB.da};
		for(Line bounds : new Line[] {ab, bc, cd, da})
			for(Line boundsB : lines)
				if(bounds.intercept(boundsB) != null)
					return true;
		return false;
	}
	
	/** Returns a set of quads by separating this quad along intercepts with the given line */
	public List<Quad> splitAlong(Line line)
	{
		return List.of(this);
	}
	
	public static class Line extends Tuple<Vec2, Vec2>
	{
		// Components of the slope intercept equation of this line
		// y = mx + b
		private float m, b;
		
		public Line(Vec2 posA, Vec2 posB)
		{
			super(posA.length() < posB.length() ? posA : posB, posA.length() < posB.length() ? posB : posA);
			
			m = (getB().y - getA().y) / (getB().x - getA().x);
			b = getA().y - (getA().x * m);
		}
		
		/** Returns the point that this line intersects with the given line, if at all */
		@Nullable
		public Vec2 intercept(Line line2)
		{
			float a = this.m, c = line2.m;
			float b = this.b, d = line2.b;
			
			// Return null if both lines have the same slope and are therefore parallel
			if(a == b)
				return null;
			
			float x = (d - c) / (a - b);
			float y = (a * x) + c;
			
			// Point at which these lines would intersect, if they were of infinite length
			Vec2 intersect = new Vec2(x, y);
			
			// If intersection point -> start normalized matches end -> start normalized
			if(getA().add(intersect.negated()).normalized() == getA().add(getB().negated()).normalized())
				// And If intersection point -> end normalized matches start -> end normalized, then intercept is between the start and end
				if(getB().add(intersect.negated()).normalized() == getB().add(getA().negated()).normalized())
					return intersect;
			
			return null;
		}
	}
}
