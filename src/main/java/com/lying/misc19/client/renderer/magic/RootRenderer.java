package com.lying.misc19.client.renderer.magic;

import com.lying.misc19.client.Canvas;
import com.lying.misc19.client.Canvas.Circle;
import com.lying.misc19.magic.ISpellComponent;

public class RootRenderer extends ComponentRenderer
{
	public void addToCanvas(ISpellComponent component, Canvas canvas)
	{
		canvas.addElement(new Circle(component.position(), 20, 1.5F), 0);
		
		canvas.addElement(new Circle(component.position(), 75, 1.25F), 1);
		canvas.addElement(new Circle(component.position(), 85, 1.25F), 1);
	}
}
