package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.game.JinxGameState;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class GlobalFatigueAndDeathFiestaMechanic {
    private static final int FIESTA_DURATION_TICKS = 20 * 30;
    private static final int FATIGUE_REFRESH_INTERVAL_TICKS = 20;
    private static final int FATIGUE_DURATION_TICKS = 20 * 6;
    private static final int FATIGUE_AMPLIFIER = 4; // 挖掘疲劳 V

    private static int fiestaTicksRemaining = 0;
    private static int fatigueRefreshCooldown = 0;
    private static boolean fatigueReturnAnnounced = true;

    private static final List<RegistryEntry<StatusEffect>> POSITIVE_EFFECT_POOL = List.of(
            StatusEffects.SPEED,
            StatusEffects.HASTE,
            StatusEffects.STRENGTH,
            StatusEffects.RESISTANCE,
            StatusEffects.REGENERATION,
            StatusEffects.JUMP_BOOST,
            StatusEffects.NIGHT_VISION,
            StatusEffects.FIRE_RESISTANCE
    );

    private GlobalFatigueAndDeathFiestaMechanic() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GlobalFatigueAndDeathFiestaMechanic::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity deadPlayer)) {
                return;
            }
            onPlayerDeath(deadPlayer);
        });
    }

    private static void onServerTick(MinecraftServer server) {
        if (!JinxGameState.isStarted()) {
            fiestaTicksRemaining = 0;
            fatigueReturnAnnounced = true;
            return;
        }

        if (fiestaTicksRemaining > 0) {
            fiestaTicksRemaining--;
            if (fiestaTicksRemaining == 0) {
                applyFatigueToNonJinxPlayers(server);
                if (!fatigueReturnAnnounced) {
                    JinxFeedback.broadcastChat(server, Text.empty()
                            .append(JinxText.game("死亡狂欢 "))
                            .append(JinxText.muted("结束：全员重新获得 "))
                            .append(JinxText.warn("挖掘疲劳 V"))
                            .append(JinxText.muted("（倒霉蛋除外）。")));
                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                        JinxFeedback.playSound(p, SoundEvents.ENTITY_VILLAGER_NO, 0.4F, 0.85F);
                    }
                    fatigueReturnAnnounced = true;
                }
            }
            return;
        }

        if (fatigueRefreshCooldown > 0) {
            fatigueRefreshCooldown--;
        } else {
            applyFatigueToNonJinxPlayers(server);
            fatigueRefreshCooldown = FATIGUE_REFRESH_INTERVAL_TICKS;
        }
    }

    private static void onPlayerDeath(ServerPlayerEntity deadPlayer) {
        if (!JinxRuleHelper.isActiveJinx(deadPlayer)) {
            return;
        }

        // 狂欢窗口内再次死亡不重置计时，也不重复发奖励。
        if (fiestaTicksRemaining > 0) {
            return;
        }

        fiestaTicksRemaining = FIESTA_DURATION_TICKS;
        fatigueReturnAnnounced = false;
        net.minecraft.server.MinecraftServer server = ((net.minecraft.server.world.ServerWorld) deadPlayer.getEntityWorld()).getServer();
        clearFatigueForAll(server);
        grantRandomPositiveEffects(server);
        MutableText deathMsg = Text.empty()
                .append(JinxText.linePrefix("狂欢"))
                .append(JinxText.player(deadPlayer))
                .append(JinxText.muted(" 阵亡：开启 "))
                .append(JinxText.warn("30 秒"))
                .append(JinxText.accent("死亡狂欢"))
                .append(JinxText.muted("！"))
                .append(JinxText.good(" 挖掘疲劳已清除，全员随机增益。"));
        JinxFeedback.broadcastChat(server, deathMsg);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            JinxFeedback.playSound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.0F);
        }
    }

    private static void applyFatigueToNonJinxPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (JinxGameState.isJinx(player)) {
                player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
                continue;
            }

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.MINING_FATIGUE,
                    FATIGUE_DURATION_TICKS,
                    FATIGUE_AMPLIFIER,
                    true,
                    false,
                    true
            ));
        }
    }

    private static void clearFatigueForAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        }
    }

    private static void grantRandomPositiveEffects(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            RegistryEntry<StatusEffect> effect = POSITIVE_EFFECT_POOL.get(server.getOverworld().random.nextInt(POSITIVE_EFFECT_POOL.size()));
            player.addStatusEffect(new StatusEffectInstance(effect, FIESTA_DURATION_TICKS, 0, true, true, true));
        }
    }
}
