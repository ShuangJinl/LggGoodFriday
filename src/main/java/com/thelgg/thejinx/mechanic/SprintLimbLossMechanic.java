package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.data.JinxPlayerDataComponent;
import com.thelgg.thejinx.data.LimbPart;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public final class SprintLimbLossMechanic {
    private static final int LIMB_STEP_TICKS = 20 * 5;

    private SprintLimbLossMechanic() {
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
            return;
        }

        JinxPlayerDataComponent data = JinxPlayerData.get(player);
        if (player.isSprinting()) {
            data.setRecoveryProgressTicks(0);
            long sprintTicks = data.getSprintProgressTicks() + 1;
            if (sprintTicks >= LIMB_STEP_TICKS) {
                sprintTicks = 0;
                loseRandomLimb(player, data);
            }
            data.setSprintProgressTicks(sprintTicks);
        } else {
            data.setSprintProgressTicks(0);
            long recoveryTicks = data.getRecoveryProgressTicks() + 1;
            if (recoveryTicks >= LIMB_STEP_TICKS) {
                recoveryTicks = 0;
                recoverRandomLimb(player, data);
            }
            data.setRecoveryProgressTicks(recoveryTicks);
        }

        applyDebuffs(player, data);
    }

    private static void loseRandomLimb(ServerPlayerEntity player, JinxPlayerDataComponent data) {
        List<LimbPart> candidates = new ArrayList<>();
        for (LimbPart part : LimbPart.values()) {
            if (!data.hasMissingPart(part)) {
                candidates.add(part);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        LimbPart chosen = candidates.get(((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).random.nextInt(candidates.size()));
        data.setMissingPart(chosen, true);
        player.calculateDimensions();
        JinxFeedback.actionBar(player, Text.empty()
                .append(JinxText.linePrefix("断肢"))
                .append(JinxText.warn("失去 "))
                .append(JinxFeedback.limbDisplayName(chosen)));
        JinxFeedback.playSound(player, SoundEvents.ENTITY_PLAYER_HURT, 0.55F, 0.85F);
    }

    private static void recoverRandomLimb(ServerPlayerEntity player, JinxPlayerDataComponent data) {
        List<LimbPart> candidates = new ArrayList<>(data.getMissingParts());
        if (candidates.isEmpty()) {
            return;
        }

        LimbPart chosen = candidates.get(((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).random.nextInt(candidates.size()));
        data.setMissingPart(chosen, false);
        player.calculateDimensions();
        JinxFeedback.actionBar(player, Text.empty()
                .append(JinxText.linePrefix("断肢"))
                .append(JinxText.good("恢复 "))
                .append(JinxFeedback.limbDisplayName(chosen)));
        JinxFeedback.playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.15F);
    }

    private static void applyDebuffs(ServerPlayerEntity player, JinxPlayerDataComponent data) {
        int missingArms = 0;
        int missingLegs = 0;
        boolean missingHead = data.hasMissingPart(LimbPart.HEAD);

        if (data.hasMissingPart(LimbPart.LEFT_ARM)) {
            missingArms++;
        }
        if (data.hasMissingPart(LimbPart.RIGHT_ARM)) {
            missingArms++;
        }
        if (data.hasMissingPart(LimbPart.LEFT_LEG)) {
            missingLegs++;
        }
        if (data.hasMissingPart(LimbPart.RIGHT_LEG)) {
            missingLegs++;
        }

        if (missingHead) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 40, 0, true, false, true));
        } else {
            player.removeStatusEffect(StatusEffects.NAUSEA);
        }

        if (missingArms <= 0) {
            player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            player.removeStatusEffect(StatusEffects.WEAKNESS);
        } else {
            int armAmplifier = missingArms >= 2 ? 1 : 0;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, armAmplifier, true, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, armAmplifier, true, false, true));
        }

        if (missingLegs <= 0) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        } else {
            int legAmplifier = missingLegs >= 2 ? 2 : 0;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, legAmplifier, true, false, true));
        }
    }
}
