package com.thelgg.thejinx.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BipedEntityModel.class)
public interface BipedEntityModelAccessor {
    @Accessor("head")
    ModelPart theJinx$getHead();

    @Accessor("hat")
    ModelPart theJinx$getHat();

    @Accessor("body")
    ModelPart theJinx$getBody();

    @Accessor("leftArm")
    ModelPart theJinx$getLeftArm();

    @Accessor("rightArm")
    ModelPart theJinx$getRightArm();

    @Accessor("leftLeg")
    ModelPart theJinx$getLeftLeg();

    @Accessor("rightLeg")
    ModelPart theJinx$getRightLeg();
}
