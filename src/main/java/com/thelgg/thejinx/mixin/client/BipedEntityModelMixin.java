package com.thelgg.thejinx.mixin.client;

import com.thelgg.thejinx.client.render.TheJinxRenderState;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
    private static final float SINGLE_LEG_DROP = 3.0F;
    private static final float DOUBLE_LEG_DROP = 6.0F;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
    private void theJinx$hideMissingParts(
            BipedEntityRenderState state,
            CallbackInfo ci
    ) {
        if (!(state instanceof TheJinxRenderState jinxState)) {
            return;
        }

        BipedEntityModelAccessor accessor = (BipedEntityModelAccessor) this;
        accessor.theJinx$getHead().visible = !jinxState.theJinx$isMissingHead();
        accessor.theJinx$getLeftArm().visible = !jinxState.theJinx$isMissingLeftArm();
        accessor.theJinx$getRightArm().visible = !jinxState.theJinx$isMissingRightArm();
        accessor.theJinx$getLeftLeg().visible = !jinxState.theJinx$isMissingLeftLeg();
        accessor.theJinx$getRightLeg().visible = !jinxState.theJinx$isMissingRightLeg();

        int missingLegs = 0;
        if (jinxState.theJinx$isMissingLeftLeg()) {
            missingLegs++;
        }
        if (jinxState.theJinx$isMissingRightLeg()) {
            missingLegs++;
        }

        float bodyDrop = switch (missingLegs) {
            case 1 -> SINGLE_LEG_DROP;
            case 2 -> DOUBLE_LEG_DROP;
            default -> 0.0F;
        };
        theJinx$applyUpperBodyDrop(accessor, bodyDrop);
    }

    private static void theJinx$applyUpperBodyDrop(BipedEntityModelAccessor accessor, float bodyDrop) {
        accessor.theJinx$getBody().originY += bodyDrop;
        accessor.theJinx$getHead().originY += bodyDrop;
        accessor.theJinx$getHat().originY += bodyDrop;
        accessor.theJinx$getLeftArm().originY += bodyDrop;
        accessor.theJinx$getRightArm().originY += bodyDrop;
    }
}
