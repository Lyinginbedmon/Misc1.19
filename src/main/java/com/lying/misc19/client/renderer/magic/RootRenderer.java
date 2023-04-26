package com.lying.misc19.client.renderer.magic;

import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class RootRenderer extends ComponentRenderer
{
	public void render(ISpellComponent component, PoseStack matrixStack)
	{
		super.render(component, matrixStack);
		drawCircle(component, 105, matrixStack);
		drawCircle(component, 125, matrixStack);
	}
}
