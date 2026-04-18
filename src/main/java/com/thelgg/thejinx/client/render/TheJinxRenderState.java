package com.thelgg.thejinx.client.render;

public interface TheJinxRenderState {
    void theJinx$setMissingParts(boolean head, boolean leftArm, boolean rightArm, boolean leftLeg, boolean rightLeg);

    boolean theJinx$isMissingHead();

    boolean theJinx$isMissingLeftArm();

    boolean theJinx$isMissingRightArm();

    boolean theJinx$isMissingLeftLeg();

    boolean theJinx$isMissingRightLeg();
}

