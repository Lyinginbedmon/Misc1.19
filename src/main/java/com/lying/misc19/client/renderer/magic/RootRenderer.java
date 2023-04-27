package com.lying.misc19.client.renderer.magic;

import com.lying.misc19.client.renderer.RenderUtils;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class RootRenderer extends ComponentRenderer
{
	public void drawPattern(ISpellComponent component, PoseStack matrixStack)
	{
		super.drawPattern(component, matrixStack);
		RenderUtils.drawCircle(component.position(), 95, 1.25F);
		RenderUtils.drawCircle(component.position(), 105, 1.25F);
	}
}
