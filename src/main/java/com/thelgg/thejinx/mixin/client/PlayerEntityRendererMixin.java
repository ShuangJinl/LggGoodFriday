package com.thelgg.thejinx.mixin.client;

import com.thelgg.thejinx.client.render.TheJinxRenderState;
import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.data.LimbPart;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void theJinx$copyMissingPartState(
            PlayerLikeEntity player,
            PlayerEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!(player instanceof PlayerEntity playerEntity)) {
            return;
        }

        ((TheJinxRenderState) state).theJinx$setMissingParts(
                JinxPlayerData.get(playerEntity).hasMissingPart(LimbPart.HEAD),
                JinxPlayerData.get(playerEntity).hasMissingPart(LimbPart.LEFT_ARM),
                JinxPlayerData.get(playerEntity).hasMissingPart(LimbPart.RIGHT_ARM),
                JinxPlayerData.get(playerEntity).hasMissingPart(LimbPart.LEFT_LEG),
                JinxPlayerData.get(playerEntity).hasMissingPart(LimbPart.RIGHT_LEG)
        );
    }
}

