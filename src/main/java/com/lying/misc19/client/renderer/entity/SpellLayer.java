package com.lying.misc19.client.renderer.entity;

import java.util.List;

import com.lying.misc19.capabilities.LivingData;
import com.lying.misc19.capabilities.LivingData.SpellData;
import com.lying.misc19.client.renderer.ComponentRenderers;
import com.lying.misc19.magic.ISpellComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SpellLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
	private static final Vec3 OFFSET = new Vec3(0D, 0D, 0.1D);
	
	public SpellLayer(RenderLayerParent<T, M> parentRenderer)
	{
		super(parentRenderer);
	}
	
	public void render(PoseStack matrixStack, MultiBufferSource bufferSource, int p_117351_, T livingEntity, float p_117353_, float p_117354_, float p_117355_, float p_117356_, float p_117357_, float p_117358_)
	{
		LivingData data = LivingData.getCapability(livingEntity);
		if(data == null || !data.hasSpells())
			return;
		
		List<SpellData> spells = data.getActiveSpells();
		matrixStack.pushPose();
			matrixStack.translate(0D, 1.501F, 0D);
			matrixStack.translate(0D, -livingEntity.getBbHeight() * 0.5D, 0D);
			matrixStack.pushPose();
				matrixStack.mulPose(Vector3f.XP.rotationDegrees(-90F));
				matrixStack.pushPose();
				for(int i=0; i<spells.size(); i++)
				{
					if(i > 0)
					{
						int index = i - 1;
						int pair = 1 + index / 2;
						Vec3 position = OFFSET.multiply(pair, pair * (index%2 == 0 ? 1 : -1), pair);
						matrixStack.translate(position.x, position.y, position.z);
					}
					matrixStack.pushPose();
						ISpellComponent arrangement = spells.get(i).arrangement();
						ComponentRenderers.renderWorld(arrangement, matrixStack, bufferSource);
					matrixStack.popPose();
				}
				matrixStack.popPose();
			matrixStack.popPose();
		matrixStack.popPose();
	}
}
