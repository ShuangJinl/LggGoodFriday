package com.thelgg.thejinx.mixin;

import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.mechanic.JinxRuleHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {
    @Inject(method = "jump", at = @At("TAIL"))
    private void theJinx$spawnLightningOnJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!JinxRuleHelper.isActiveJinx(player)) {
            return;
        }

        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(serverWorld, SpawnReason.TRIGGERED);
        if (lightning == null) {
            return;
        }
        lightning.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
        serverWorld.spawnEntity(lightning);
        JinxFeedback.actionBar(player, Text.empty()
                .append(JinxText.linePrefix("倒霉蛋"))
                .append(JinxText.warn("跳跃 "))
                .append(JinxText.accent("召唤雷击！")));
        JinxFeedback.playSound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3F, 1.4F);
    }
}
