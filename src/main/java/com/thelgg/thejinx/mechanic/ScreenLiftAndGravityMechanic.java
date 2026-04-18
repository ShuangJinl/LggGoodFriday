package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.game.JinxFeatureFlags;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class ScreenLiftAndGravityMechanic {
    private static final double LIFT_UP_SPEED = 0.92D;
    private static final double LIFT_DELTA_Y_PER_TICK = 0.24D;
    private static final double OPEN_IMPULSE_UP_SPEED = 1.15D;
    private static final double OPEN_IMPULSE_DELTA_Y = 0.38D;
    private static final long OPEN_IMPULSE_DURATION_TICKS = 5L;
    // 原版约 0.08/tick，下落五倍体感 => 额外施加 0.32（总计约 0.40）
    private static final double EXTRA_GRAVITY_PER_TICK = 0.32D;
    private static final Map<UUID, Long> OPEN_IMPULSE_UNTIL_TICK = new ConcurrentHashMap<>();

    private ScreenLiftAndGravityMechanic() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        if (!JinxRuleHelper.isActiveJinx(player)) {
            player.setNoGravity(false);
            OPEN_IMPULSE_UNTIL_TICK.remove(player.getUuid());
            return;
        }

        if (HumanTntMechanic.isJinxBeingCarried(player)) {
            player.setNoGravity(false);
            OPEN_IMPULSE_UNTIL_TICK.remove(player.getUuid());
            return;
        }

        // 人体 TNT 飞行期间不要改写速度/坐标，否则与界面飞升同类：会在首几 tick 抹掉掷出初速度。
        if (JinxPlayerData.get(player).isLaunchedAsHumanTnt()) {
            player.setNoGravity(false);
            OPEN_IMPULSE_UNTIL_TICK.remove(player.getUuid());
            return;
        }

        if (JinxPlayerData.get(player).isScreenOpen()) {
            if (JinxFeatureFlags.isScreenLiftEnabled()) {
                long now = player.getEntityWorld().getTime();
                long openImpulseUntil = OPEN_IMPULSE_UNTIL_TICK.getOrDefault(player.getUuid(), -1L);
                boolean inOpenImpulseWindow = openImpulseUntil >= now;
                Vec3d velocity = player.getVelocity();
                // 直接抬升坐标 + 无重力，规避交互期间速度被其他逻辑覆盖的问题。
                player.setNoGravity(true);
                double deltaY = inOpenImpulseWindow ? OPEN_IMPULSE_DELTA_Y : LIFT_DELTA_Y_PER_TICK;
                double upSpeed = inOpenImpulseWindow ? OPEN_IMPULSE_UP_SPEED : LIFT_UP_SPEED;
                player.requestTeleport(player.getX(), player.getY() + deltaY, player.getZ());
                player.setVelocity(velocity.x * 0.6D, upSpeed, velocity.z * 0.6D);
                player.fallDistance = 0.0F;
                return;
            }
            player.setNoGravity(false);
            OPEN_IMPULSE_UNTIL_TICK.remove(player.getUuid());
        } else {
            player.setNoGravity(false);
            OPEN_IMPULSE_UNTIL_TICK.remove(player.getUuid());
        }

        if (!player.isOnGround()
                && player.getVelocity().y < 0.0D
                && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, velocity.y - EXTRA_GRAVITY_PER_TICK, velocity.z);
        }
    }

    public static void onScreenStateSync(ServerPlayerEntity player, boolean oldState, boolean newState) {
        if (!JinxRuleHelper.isActiveJinx(player)) {
            return;
        }
        if (!oldState && newState) {
            if (JinxFeatureFlags.isScreenLiftEnabled()) {
                long now = player.getEntityWorld().getTime();
                OPEN_IMPULSE_UNTIL_TICK.put(player.getUuid(), now + OPEN_IMPULSE_DURATION_TICKS);
                Vec3d velocity = player.getVelocity();
                player.setVelocity(velocity.x * 0.6D, OPEN_IMPULSE_UP_SPEED, velocity.z * 0.6D);
                JinxFeedback.actionBar(player, Text.empty()
                        .append(JinxText.linePrefix("倒霉蛋"))
                        .append(JinxText.accent("界面飞升 "))
                        .append(JinxText.good("已激活")));
                JinxFeedback.playSound(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.35F, 1.5F);
            }
            return;
        }
        if (!oldState || newState) {
            return;
        }
    }
}
