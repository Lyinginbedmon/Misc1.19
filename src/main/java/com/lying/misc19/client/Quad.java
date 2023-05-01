package com.lying.misc19.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;

/** Helper class for rendering */
public class Quad
{
	private static final Vec2[] DIRECTIONS = new Vec2[] { new Vec2(0, 1), new Vec2(0, -1), new Vec2(1, 0), new Vec2(-1, 0) };
	private final Line ab, bc, cd, da;
	private final Line[] boundaries;
	private final Vec2[] vertices;
	
	public Quad(Vec2 a, Vec2 b, Vec2 c, Vec2 d)
	{
		this.ab = new Line(a, b);
		this.bc = new Line(b, c);
		this.cd = new Line(c, d);
		this.da = new Line(d, a);
		
		this.vertices = new Vec2[] {a, b, c, d};
		this.boundaries = new Line[]{ab, bc, cd, da};
	}
	
	public String toString()
	{
		return "Quad["+String.join("/",
				"["+a().x+", "+a().y+"]",
				"["+b().x+", "+b().y+"]",
				"["+c().x+", "+c().y+"]",
				"["+d().x+", "+d().y+"]")+"]";
	}
	
	public Vec2 a() { return this.vertices[0]; }
	public Vec2 b() { return this.vertices[1]; }
	public Vec2 c() { return this.vertices[2]; }
	public Vec2 d() { return this.vertices[3]; }
	
	public Line ab(){ return ab; }
	public Line bc(){ return bc; }
	public Line cd(){ return cd; }
	public Line da(){ return da; }
	
	/** Returns the first boundary line of this quad the given quad intersects with */
	public Line intersects(Quad quad2)
	{
		for(Line bounds : boundaries)
			for(Line boundsB : quad2.boundaries)
				if(bounds.intercept(boundsB) != null)
					return bounds;
		return null;
	}
	
	/** Returns true if the given line intersects with this quad */
	public boolean intersects(Line line)
	{
		for(Line bounds : boundaries)
			if(bounds.intercept(line) != null)
				return true;
		return false;
	}
	
	/** Returns true if the space of the given quad is entirely within this quad */
	public boolean contains(Quad quad2)
	{
		// If there are no intersections...
		for(Line vec : boundaries)
			for(Line vec2 : quad2.boundaries)
				if(vec.intercept(vec2) != null)
					return false;
		
		// But at least one vertex of the quad is inside of this quad...
		for(Vec2 vertex : quad2.vertices)
			for(Vec2 dir : DIRECTIONS)
			{
				int intersections = 0;
				Line testLine = new Line(vertex, vertex.add(dir.scale(Float.MAX_VALUE)));
				for(Line bounds : boundaries)
					if(bounds.intercept(testLine) != null)
						intersections++;
				
				/**
				 * There will always be an odd number of intersections if the point is inside
				 * Because if the point is inside, it should only need to hit one boundary line to escape
				 */
				if(intersections % 2 != 0)
					return true;
			}
		
		return false;
	}
	
	/** Returns a set of quads by separating this quad along intercepts with the given line */
	public List<Quad> splitAlong(Line line)
	{
		/**
		 * Check if the left and right lines are both intersected
		 * 	If so, create new quads using the intersections
		 * If not, check if the top and bottom lines are both intersected
		 * 	If so, create new quads using the intersections
		 * Otherwise, return null
		 * FIXME Intersections that do not exclusively produce two quads are currently ignored
		 */
		
		Vec2 abInt = ab.intercept(line);
		Vec2 cdInt = cd.intercept(line);
		
		Vec2 bcInt = bc.intercept(line);
		Vec2 daInt = da.intercept(line);
		
		int tally = 0;
		for(Vec2 vec : new Vec2[]{abInt, bcInt, cdInt, daInt})
			if(vec != null)
				tally++;
		
		if(tally == 0 || tally != 2)
			return List.of(this);
		
		if(abInt != null && cdInt != null)
		{
			Quad q1 = new Quad(a(), abInt, cdInt, d());
			Quad q2 = new Quad(abInt, b(), c(), cdInt);
			return List.of(q1, q2);
		}
		
		if(bcInt != null && daInt != null)
		{
			Quad q1 = new Quad(a(), b(), bcInt, daInt);
			Quad q2 = new Quad(daInt, bcInt, c(), d());
			return List.of(q1, q2);
		}
		
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
			// And if intersection point -> end normalized matches start -> end normalized, then intercept is between the start and end
			// NOTE: Removing this check not only breaks almost all exclusion, it also sends the exclusion function into an infinite loop
			if(
				getA().add(intersect.negated()).normalized() == getA().add(getB().negated()).normalized() &&
				getB().add(intersect.negated()).normalized() == getB().add(getA().negated()).normalized())
					return intersect;
			
			return null;
		}
	}
}
