package com.lying.misc19.client.renderer.magic;

import com.lying.misc19.client.Canvas;
import com.lying.misc19.client.Canvas.Circle;
import com.lying.misc19.client.Canvas.ExclusionQuad;
import com.lying.misc19.client.Canvas.Line;
import com.lying.misc19.magic.ISpellComponent;

import net.minecraft.world.phys.Vec2;

public class RootRenderer extends ComponentRenderer
{
	protected int spriteScale() { return 24; }
	
	public void addToCanvas(ISpellComponent component, Canvas canvas)
	{
		Vec2 pos = component.position();
//		canvas.addElement(new Circle(pos, spriteScale() + 5, 1.5F), Canvas.GLYPHS);
		canvas.addElement(new ExclusionQuad(pos.add(new Vec2(-100, -10)), pos.add(new Vec2(100, -10)), pos.add(new Vec2(100, 10)), pos.add(new Vec2(-100, 10))), Canvas.EXCLUSIONS);
		
		canvas.addElement(new Line(pos.add(component.up().scale(50)), pos.add(component.up().negated().scale(50)), 50, 255, 255, 255, 255), Canvas.DECORATIONS);
		
//		canvas.addElement(new Circle(pos, 75, 1.25F), Canvas.DECORATIONS);
//		canvas.addElement(new Circle(pos, 85, 1.25F), Canvas.DECORATIONS);
		
	}
}
