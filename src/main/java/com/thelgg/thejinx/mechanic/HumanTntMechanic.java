package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class HumanTntMechanic {
    private static final float TNT_POWER = 4.0F;
    /**
     * 原版爆炸对“爆炸中心实体”常不结算伤害；在 {@link ServerWorld#createExplosion} 之后对倒霉蛋补一次同类型伤害，
     * 数值按 TNT 威力近似，仍走护甲/抗性/伤害事件。
     */
    private static final float HUMAN_TNT_SELF_EXPLOSION_DAMAGE = TNT_POWER * 3.5F;
    private static final double CARRY_HEIGHT_OFFSET = 2.05D;
    private static final long LAUNCH_COLLISION_GRACE_TICKS = 4L;
    private static final double LAUNCH_HORIZONTAL_SPEED = 7.5D;
    private static final double LAUNCH_VERTICAL_BOOST = 1.75D;
    private static final double MIN_HORIZONTAL_LOOK_LEN = 0.12D;
    private static final double LAUNCH_SPAWN_FORWARD_OFFSET = 2.4D;
    private static final double LAUNCH_SPAWN_UP_OFFSET = 2.0D;
    private static final long LAUNCH_REINFORCE_TICKS = 10L;
    private static final double LAUNCH_REINFORCE_FORWARD_NUDGE = 0.45D;
    private static final double LAUNCH_REINFORCE_UP_NUDGE = 0.08D;
    private static final Map<UUID, UUID> CARRIED_JINX_TO_CARRIER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAUNCHED_AT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> PENDING_REAPPLY_VELOCITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PENDING_REAPPLY_UNTIL_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> PENDING_REAPPLY_DIRECTION = new ConcurrentHashMap<>();

    private HumanTntMechanic() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(HumanTntMechanic::allowDamageWhileCarried);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> onUseEntity(player, world, entity));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                followCarrierIfNeeded(player);
                reapplyLaunchVelocityIfNeeded(player);
                checkLaunchedCollision(player);
            }
        });
    }

    private static boolean allowDamageWhileCarried(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }
        if (!JinxRuleHelper.isActiveJinx(player) || !isJinxBeingCarried(player)) {
            return true;
        }
        return !source.isIn(DamageTypeTags.IS_FALL);
    }

    private static ActionResult onUseEntity(net.minecraft.entity.player.PlayerEntity player, World world, Entity entity) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity actor)) {
            return ActionResult.PASS;
        }

        if (!(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.PASS;
        }

        if (!JinxRuleHelper.isActiveJinx(target)) {
            return ActionResult.PASS;
        }

        if (actor.equals(target) || isJinxBeingCarried(target)) {
            return ActionResult.PASS;
        }

        boolean mounted = target.startRiding(actor, true, true);
        if (!mounted) {
            // 部分场景下玩家不能稳定挂载，兜底为“跟随抬起”。
            CARRIED_JINX_TO_CARRIER.put(target.getUuid(), actor.getUuid());
        }
        JinxFeedback.actionBar(actor, Text.empty()
                .append(JinxText.linePrefix("人体TNT"))
                .append(JinxText.good("你已举起 "))
                .append(JinxText.player(target)));
        JinxFeedback.actionBar(target, Text.empty()
                .append(JinxText.linePrefix("人体TNT"))
                .append(JinxText.warn("你已被 "))
                .append(JinxText.player(actor))
                .append(JinxText.muted(" 举起")));
        JinxFeedback.playSound(actor, SoundEvents.ENTITY_SLIME_SQUISH, 0.55F, 0.9F);
        JinxFeedback.playSound(target, SoundEvents.ENTITY_SLIME_SQUISH, 0.55F, 1.1F);
        return ActionResult.SUCCESS;
    }

    public static void tryLaunchPassengerJinx(ServerPlayerEntity actor) {
        for (Entity passenger : actor.getPassengerList()) {
            if (passenger instanceof ServerPlayerEntity jinx && JinxRuleHelper.isActiveJinx(jinx)) {
                jinx.stopRiding();
                launchJinx(actor, jinx);
                break;
            }
        }

        for (Map.Entry<UUID, UUID> entry : CARRIED_JINX_TO_CARRIER.entrySet()) {
            if (!entry.getValue().equals(actor.getUuid())) {
                continue;
            }

            ServerPlayerEntity jinx = actor.getEntityWorld().getServer().getPlayerManager().getPlayer(entry.getKey());
            if (jinx == null || !JinxRuleHelper.isActiveJinx(jinx)) {
                CARRIED_JINX_TO_CARRIER.remove(entry.getKey());
                continue;
            }

            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            launchJinx(actor, jinx);
            break;
        }
    }

    public static boolean isJinxBeingCarried(ServerPlayerEntity jinx) {
        if (jinx.hasVehicle()) {
            return true;
        }

        UUID carrierUuid = CARRIED_JINX_TO_CARRIER.get(jinx.getUuid());
        if (carrierUuid == null) {
            return false;
        }

        ServerPlayerEntity carrier = jinx.getEntityWorld().getServer().getPlayerManager().getPlayer(carrierUuid);
        if (carrier == null || !carrier.isAlive() || !jinx.isAlive() || carrier.getEntityWorld() != jinx.getEntityWorld()) {
            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            return false;
        }

        // 兜底抓举超过范围视为失效，防止状态残留导致飞升一直被禁用。
        if (carrier.squaredDistanceTo(jinx) > 16.0D) {
            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            return false;
        }
        return true;
    }

    private static Vec3d computeLaunchVelocity(ServerPlayerEntity actor) {
        Vec3d horizontal = computeHorizontalLaunchDirection(actor);
        horizontal = horizontal.normalize().multiply(LAUNCH_HORIZONTAL_SPEED);
        return new Vec3d(horizontal.x, LAUNCH_VERTICAL_BOOST, horizontal.z);
    }

    private static Vec3d computeHorizontalLaunchDirection(ServerPlayerEntity actor) {
        Vec3d look = actor.getRotationVector();
        Vec3d horizontal = new Vec3d(look.x, 0.0D, look.z);
        if (horizontal.lengthSquared() < MIN_HORIZONTAL_LOOK_LEN * MIN_HORIZONTAL_LOOK_LEN) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return horizontal.normalize();
    }

    private static void launchJinx(ServerPlayerEntity actor, ServerPlayerEntity jinx) {
        Vec3d horizontalDir = computeHorizontalLaunchDirection(actor);
        Vec3d spawnPos = new Vec3d(actor.getX(), actor.getY(), actor.getZ())
                .add(horizontalDir.multiply(LAUNCH_SPAWN_FORWARD_OFFSET))
                .add(0.0D, LAUNCH_SPAWN_UP_OFFSET, 0.0D);

        // 先前置到玩家前上方，避免“发射后落脚下”。
        jinx.requestTeleport(spawnPos.x, spawnPos.y, spawnPos.z);
        Vec3d launchVelocity = computeLaunchVelocity(actor);
        jinx.setVelocity(launchVelocity);
        jinx.fallDistance = 0.0F;
        JinxPlayerData.get(jinx).setLaunchedAsHumanTnt(true);
        JinxFeedback.actionBar(actor, Text.empty()
                .append(JinxText.linePrefix("人体TNT"))
                .append(JinxText.accent("已掷出 "))
                .append(JinxText.player(jinx)));
        JinxFeedback.actionBar(jinx, Text.empty()
                .append(JinxText.linePrefix("人体TNT"))
                .append(JinxText.warn("你被掷出！"))
                .append(JinxText.muted(" 撞方块将 "))
                .append(JinxText.gold("爆炸")));
        JinxFeedback.playSound(actor, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.45F, 1.0F);
        JinxFeedback.playSound(jinx, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.55F, 1.05F);
        long now = actor.getEntityWorld().getTime();
        LAUNCHED_AT_TICK.put(jinx.getUuid(), now);
        // 发射后短窗口内持续强化，减少“偶发没飞出去”的情况。
        PENDING_REAPPLY_VELOCITY.put(jinx.getUuid(), launchVelocity);
        PENDING_REAPPLY_DIRECTION.put(jinx.getUuid(), horizontalDir);
        PENDING_REAPPLY_UNTIL_TICK.put(jinx.getUuid(), now + LAUNCH_REINFORCE_TICKS);
    }

    private static void reapplyLaunchVelocityIfNeeded(ServerPlayerEntity player) {
        Long applyUntil = PENDING_REAPPLY_UNTIL_TICK.get(player.getUuid());
        if (applyUntil == null) {
            return;
        }

        long now = player.getEntityWorld().getTime();
        if (now > applyUntil) {
            PENDING_REAPPLY_VELOCITY.remove(player.getUuid());
            PENDING_REAPPLY_DIRECTION.remove(player.getUuid());
            PENDING_REAPPLY_UNTIL_TICK.remove(player.getUuid());
            return;
        }

        Vec3d velocity = PENDING_REAPPLY_VELOCITY.get(player.getUuid());
        Vec3d direction = PENDING_REAPPLY_DIRECTION.get(player.getUuid());
        if (velocity == null || !JinxPlayerData.get(player).isLaunchedAsHumanTnt()) {
            PENDING_REAPPLY_VELOCITY.remove(player.getUuid());
            PENDING_REAPPLY_DIRECTION.remove(player.getUuid());
            PENDING_REAPPLY_UNTIL_TICK.remove(player.getUuid());
            return;
        }
        player.setVelocity(velocity);
        if (direction != null) {
            player.requestTeleport(
                    player.getX() + direction.x * LAUNCH_REINFORCE_FORWARD_NUDGE,
                    player.getY() + LAUNCH_REINFORCE_UP_NUDGE,
                    player.getZ() + direction.z * LAUNCH_REINFORCE_FORWARD_NUDGE
            );
        }
    }

    private static void followCarrierIfNeeded(ServerPlayerEntity jinx) {
        UUID carrierUuid = CARRIED_JINX_TO_CARRIER.get(jinx.getUuid());
        if (carrierUuid == null) {
            return;
        }

        if (!JinxRuleHelper.isActiveJinx(jinx)) {
            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            return;
        }

        ServerPlayerEntity carrier = jinx.getEntityWorld().getServer().getPlayerManager().getPlayer(carrierUuid);
        if (carrier == null || !carrier.isAlive() || !jinx.isAlive()) {
            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            return;
        }

        if (jinx.hasVehicle()) {
            CARRIED_JINX_TO_CARRIER.remove(jinx.getUuid());
            return;
        }

        jinx.requestTeleport(carrier.getX(), carrier.getY() + CARRY_HEIGHT_OFFSET, carrier.getZ());
        jinx.setVelocity(Vec3d.ZERO);
        jinx.fallDistance = 0.0F;
    }

    private static void checkLaunchedCollision(ServerPlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();

        if (!JinxPlayerData.get(player).isLaunchedAsHumanTnt()) {
            LAUNCHED_AT_TICK.remove(player.getUuid());
            PENDING_REAPPLY_VELOCITY.remove(player.getUuid());
            PENDING_REAPPLY_DIRECTION.remove(player.getUuid());
            PENDING_REAPPLY_UNTIL_TICK.remove(player.getUuid());
            return;
        }

        long launchedAt = LAUNCHED_AT_TICK.getOrDefault(player.getUuid(), -1L);
        if (launchedAt >= 0L && serverWorld.getTime() - launchedAt < LAUNCH_COLLISION_GRACE_TICKS) {
            return;
        }

        if (player.horizontalCollision || player.verticalCollision) {
            JinxFeedback.actionBar(player, Text.empty()
                    .append(JinxText.linePrefix("人体TNT"))
                    .append(JinxText.warn("撞击 "))
                    .append(JinxText.gold("爆炸！")));
            // 不以倒霉蛋为爆炸实体：否则新版下常出现“爆炸源不吃伤害”，且易连带影响他人判定。
            DamageSource explosionDamage = serverWorld.getDamageSources().explosion(null, null);
            serverWorld.createExplosion(
                    null,
                    explosionDamage,
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    TNT_POWER,
                    false,
                    World.ExplosionSourceType.TNT
            );
            if (player.isAlive()) {
                player.damage(serverWorld, explosionDamage, HUMAN_TNT_SELF_EXPLOSION_DAMAGE);
            }
            JinxPlayerData.get(player).setLaunchedAsHumanTnt(false);
            LAUNCHED_AT_TICK.remove(player.getUuid());
            PENDING_REAPPLY_VELOCITY.remove(player.getUuid());
            PENDING_REAPPLY_DIRECTION.remove(player.getUuid());
            PENDING_REAPPLY_UNTIL_TICK.remove(player.getUuid());
        }
    }
}
