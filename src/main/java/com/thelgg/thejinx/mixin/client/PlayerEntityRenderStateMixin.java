package com.thelgg.thejinx.mixin.client;

import com.thelgg.thejinx.client.render.TheJinxRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements TheJinxRenderState {
    private boolean theJinx$missingHead;
    private boolean theJinx$missingLeftArm;
    private boolean theJinx$missingRightArm;
    private boolean theJinx$missingLeftLeg;
    private boolean theJinx$missingRightLeg;

    @Override
    public void theJinx$setMissingParts(boolean head, boolean leftArm, boolean rightArm, boolean leftLeg, boolean rightLeg) {
        this.theJinx$missingHead = head;
        this.theJinx$missingLeftArm = leftArm;
        this.theJinx$missingRightArm = rightArm;
        this.theJinx$missingLeftLeg = leftLeg;
        this.theJinx$missingRightLeg = rightLeg;
    }

    @Override
    public boolean theJinx$isMissingHead() {
        return theJinx$missingHead;
    }

    @Override
    public boolean theJinx$isMissingLeftArm() {
        return theJinx$missingLeftArm;
    }

    @Override
    public boolean theJinx$isMissingRightArm() {
        return theJinx$missingRightArm;
    }

    @Override
    public boolean theJinx$isMissingLeftLeg() {
        return theJinx$missingLeftLeg;
    }

    @Override
    public boolean theJinx$isMissingRightLeg() {
        return theJinx$missingRightLeg;
    }
}

