package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.data.JinxPlayerDataComponent;
import com.thelgg.thejinx.game.JinxGameState;
import com.thelgg.thejinx.item.JinxItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public final class AssimilationMechanic {
    private static final int ASSIMILATION_DEATH_THRESHOLD = 100;

    private AssimilationMechanic() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register(AssimilationMechanic::onAttackEntity);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onPlayerDeath(player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onAfterRespawn(newPlayer));
    }

    private static ActionResult onAttackEntity(PlayerEntity attacker, World world, net.minecraft.util.Hand hand, net.minecraft.entity.Entity entity, net.minecraft.util.hit.EntityHitResult hitResult) {
        if (world.isClient()
                || !(attacker instanceof ServerPlayerEntity sourcePlayer)
                || !(entity instanceof ServerPlayerEntity targetPlayer)) {
            return ActionResult.PASS;
        }

        ItemStack stack = sourcePlayer.getMainHandStack();
        if (!stack.isOf(JinxItems.ASSIMILATION)) {
            return ActionResult.PASS;
        }

        if (sourcePlayer.getUuid().equals(targetPlayer.getUuid())) {
            return ActionResult.PASS;
        }

        if (!JinxGameState.isStarted() || !JinxRuleHelper.isActiveJinx(sourcePlayer)) {
            return ActionResult.PASS;
        }

        MinecraftServer server = ((net.minecraft.server.world.ServerWorld) sourcePlayer.getEntityWorld()).getServer();

        if (JinxPlayerData.get(targetPlayer).isJinx()) {
            sourcePlayer.sendMessage(
                    Text.empty()
                            .append(JinxText.linePrefix("同化"))
                            .append(JinxText.player(targetPlayer))
                            .append(JinxText.muted(" 已经是倒霉蛋了。")),
                    false
            );
            return ActionResult.SUCCESS;
        }

        JinxGameState.promoteToJinx(server, targetPlayer);
        targetPlayer.removeStatusEffect(StatusEffects.MINING_FATIGUE);

        JinxPlayerDataComponent sourceData = JinxPlayerData.get(sourcePlayer);
        sourceData.setUsedAssimilation(true);
        sourceData.setShouldRegrantAssimilation(false);

        stack.decrement(1);

        sourcePlayer.sendMessage(
                Text.empty()
                        .append(JinxText.linePrefix("同化"))
                        .append(JinxText.good("成功 "))
                        .append(JinxText.player(targetPlayer))
                        .append(JinxText.muted(" 已成为倒霉蛋；你们 "))
                        .append(JinxText.accent("同时"))
                        .append(JinxText.muted(" 承受倒霉蛋规则。")),
                false
        );
        targetPlayer.sendMessage(
                Text.empty()
                        .append(JinxText.linePrefix("同化"))
                        .append(JinxText.warn("你已成为倒霉蛋！"))
                        .append(JinxText.muted("（与 "))
                        .append(JinxText.player(sourcePlayer))
                        .append(JinxText.muted(" 状态同步）")),
                false
        );
        JinxFeedback.playSound(sourcePlayer, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 0.45F, 1.15F);
        JinxFeedback.playSound(targetPlayer, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 0.55F, 1.05F);
        return ActionResult.SUCCESS;
    }

    private static void onPlayerDeath(ServerPlayerEntity player) {
        if (!JinxRuleHelper.isActiveJinx(player)) {
            return;
        }

        JinxPlayerDataComponent data = JinxPlayerData.get(player);
        data.incrementJinxDeaths();
        if (data.getJinxDeaths() >= ASSIMILATION_DEATH_THRESHOLD && !data.hasUsedAssimilation()) {
            data.setShouldRegrantAssimilation(true);
            ensureAssimilationItem(player);
        }
    }

    private static void onAfterRespawn(ServerPlayerEntity player) {
        if (!JinxRuleHelper.isActiveJinx(player)) {
            return;
        }

        JinxPlayerDataComponent data = JinxPlayerData.get(player);
        if (data.getJinxDeaths() >= ASSIMILATION_DEATH_THRESHOLD
                && !data.hasUsedAssimilation()
                && data.shouldRegrantAssimilation()) {
            ensureAssimilationItem(player);
        }
    }

    private static void ensureAssimilationItem(ServerPlayerEntity player) {
        if (player.getInventory().containsAny(stack -> stack.isOf(JinxItems.ASSIMILATION))) {
            return;
        }

        ItemStack assimilationStack = new ItemStack(JinxItems.ASSIMILATION);
        if (!player.giveItemStack(assimilationStack)) {
            player.dropItem(assimilationStack, false);
        }
        JinxFeedback.chat(
                player,
                Text.empty()
                        .append(JinxText.linePrefix("倒霉蛋"))
                        .append(JinxText.gold("同化之卵 "))
                        .append(JinxText.muted("— 用其 "))
                        .append(JinxText.accent("攻击一名玩家"))
                        .append(JinxText.muted(" 可将其 "))
                        .append(JinxText.warn("同步"))
                        .append(JinxText.muted(" 为倒霉蛋（你不会失去身份）；丢弃会消失。"))
        );
        JinxFeedback.playSound(player, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.35F, 1.25F);
    }
}
