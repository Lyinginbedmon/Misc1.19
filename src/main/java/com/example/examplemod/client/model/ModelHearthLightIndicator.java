package com.example.examplemod.client.model;

import com.example.examplemod.client.renderer.Animations;
import com.example.examplemod.entities.EntityHearthLight;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class ModelHearthLightIndicator<T extends EntityHearthLight> extends HierarchicalModel<T>
{
	private ModelPart root;
	
	public ModelHearthLightIndicator(ModelPart partsIn)
	{
		root = partsIn.getChild("root");
	}
	
	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
		
		PartDefinition palm = root.addOrReplaceChild("palm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 24.0F, 0.0F));
		
		PartDefinition finger1P = palm.addOrReplaceChild("finger1P", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-1.5F, -2.0F, 0.0F));
		PartDefinition finger1M = finger1P.addOrReplaceChild("finger1M", CubeListBuilder.create().texOffs(8, 0).addBox(-2.0F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(1.5F, -2.0F, 0.0F));
		finger1M.addOrReplaceChild("finger1D", CubeListBuilder.create().texOffs(8, 0).addBox(-2.0F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		
		PartDefinition finger2P = palm.addOrReplaceChild("finger2P", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		PartDefinition finger2M = finger2P.addOrReplaceChild("finger2M", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		finger2M.addOrReplaceChild("finger2D", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		
		PartDefinition finger3P = palm.addOrReplaceChild("finger3P", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(1.5F, -2.0F, 0.0F));
		PartDefinition finger3M = finger3P.addOrReplaceChild("finger3M", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		finger3M.addOrReplaceChild("finger3D", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		
		PartDefinition thumb = palm.addOrReplaceChild("thumbP", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(-2.0F, 0.0F, 0.0F));
		thumb.addOrReplaceChild("thumbD", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -2.0F, 0.0F));
		
		return LayerDefinition.create(meshdefinition, 16, 16);
	}
	
	public ModelPart root() { return this.root; }
	
	public void setupAnim(T p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_)
	{
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.animate(p_102618_.indicatorHandIdle, Animations.HAND_IDLE, p_102621_);
		this.animate(p_102618_.indicatorHandWave, Animations.HAND_WAVING, p_102621_);
		this.animate(p_102618_.indicatorHandBeckoning, Animations.HAND_BECKONING, p_102621_);
	}
}
