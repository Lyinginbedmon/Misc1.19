package com.example.examplemod.client.renderer;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class Animations
{
	public static final AnimationDefinition HAND_IDLE = AnimationDefinition.Builder.withLength(5F).looping()
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.POSITION,
				new Keyframe(0F, KeyframeAnimations.posVec(0, 4F, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(82.5F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(75F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(82.5F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("thumbP", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(20F, -15F, -45F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(60F, -15F, -45F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(20F, -15F, -45F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("thumbD", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(20F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(60F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(20F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(12F, 0, -5), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(18F, 0, -5), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(12F, 0, -5), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(12F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(18F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(12F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(12F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(18F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(12F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(35F, 0, 5), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(65F, 0, 5), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(35F, 0, 5), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(35F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(65F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(35F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(35F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(65F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(35F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(45F, 0, 10), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(75F, 0, 10), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(45F, 0, 10), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(45F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(75F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(45F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(45F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2.5F, KeyframeAnimations.degreeVec(75F, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(5F, KeyframeAnimations.degreeVec(45F, 0, 0), AnimationChannel.Interpolations.CATMULLROM))).build();
	public static final AnimationDefinition HAND_WAVING = AnimationDefinition.Builder.withLength(1F).looping()
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0.0F, KeyframeAnimations.degreeVec(0F, 0F, -30F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, 30F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.0F, KeyframeAnimations.degreeVec(0F, 0F, -30F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0.0F, KeyframeAnimations.degreeVec(0F, 0F, -10F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, 5F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.0F, KeyframeAnimations.degreeVec(0F, 0F, -10F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0.0F, KeyframeAnimations.degreeVec(0F, 0F, -5F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, 12.5F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.0F, KeyframeAnimations.degreeVec(0F, 0F, -5F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0.0F, KeyframeAnimations.degreeVec(0F, 0F, -2.5F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, 22.5F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.0F, KeyframeAnimations.degreeVec(0F, 0F, -2.5F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("thumbP", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0.0F, KeyframeAnimations.degreeVec(0F, 0F, -35F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, -20F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.0F, KeyframeAnimations.degreeVec(0F, 0F, -35F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(7.5F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger1D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(10F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger2M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(5F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger2D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(10F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger3M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(5F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger3D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(17.5F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("thumbD", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 12.5F), AnimationChannel.Interpolations.LINEAR))).build();
	public static final AnimationDefinition HAND_BECKONING = AnimationDefinition.Builder.withLength(2F).looping()
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(95F, 1.7F, -200F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(198F, 13F, -233F), AnimationChannel.Interpolations.CATMULLROM),
				new Keyframe(2F, KeyframeAnimations.degreeVec(95F, 1.7F, -200F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(12.5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger1P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(10F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(32.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(10F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger1M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(12.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(35F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(12.5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger2P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(2.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(47.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(2.5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger2M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(7.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(25F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(7.5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger2D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(15F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(27.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(15F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger3P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(-5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(52.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(-5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger3M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(7.5F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(30F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(7.5F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("finger3D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(15F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(30F, 0, 0), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(15F, 0, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("thumbP", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(0, 0, -15F), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(40F, 0, -15F), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(0, 0, -15F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("thumbD", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(0, 0, 20F), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(1.5F, KeyframeAnimations.degreeVec(0F, 0, 47.5F), AnimationChannel.Interpolations.LINEAR),
				new Keyframe(2F, KeyframeAnimations.degreeVec(0, 0, 20F), AnimationChannel.Interpolations.LINEAR))).build();
	public static final AnimationDefinition HAND_LANTERN_IDLE = AnimationDefinition.Builder.withLength(5F).looping()
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.POSITION,
				new Keyframe(0F, KeyframeAnimations.posVec(0, 4F, 0), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("palm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(65F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("thumbP", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(58.5F, -6F, 25.5F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("thumbD", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(3F, 7F, -67F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(30F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(60F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger1D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(80F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(17.5F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(60F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger2D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(75F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3P", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(22.5F, 0, 10), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3M", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(52F, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("finger3D", new AnimationChannel(AnimationChannel.Targets.ROTATION,
				new Keyframe(0F, KeyframeAnimations.degreeVec(89.5F, 0, 0), AnimationChannel.Interpolations.CATMULLROM))).build();
}
