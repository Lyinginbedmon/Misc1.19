package com.lying.misc19.magic.component;

import java.util.List;

import com.lying.misc19.magic.ISpellComponent;

public abstract class RootGlyph extends ComponentBase
{
	private ISpellComponent circle = null;
	
	public Type type() { return Type.ROOT; }
	
	public boolean isValidInput(ISpellComponent component) { return false; }
	
	public boolean isValidOutput(ISpellComponent component) { return circle == null && component.type() == Type.CIRCLE; }
	
	public List<ISpellComponent> outputs() { return List.of(circle); }
}
