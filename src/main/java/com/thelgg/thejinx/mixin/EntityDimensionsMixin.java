package com.thelgg.thejinx.mixin;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.data.LimbPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {
    @Inject(method = "getDimensions(Lnet/minecraft/entity/EntityPose;)Lnet/minecraft/entity/EntityDimensions;", at = @At("HEAD"), cancellable = true)
    private void theJinx$modifyDimensionsForLegLoss(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) {
            return;
        }

        int missingLegs = 0;
        if (JinxPlayerData.get(player).hasMissingPart(LimbPart.LEFT_LEG)) {
            missingLegs++;
        }
        if (JinxPlayerData.get(player).hasMissingPart(LimbPart.RIGHT_LEG)) {
            missingLegs++;
        }

        if (missingLegs <= 0) {
            return;
        }

        // 丢失腿部后降低碰撞箱高度（视觉上更接地）
        if (missingLegs >= 2) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F));
        } else {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.5F));
        }
    }
}

