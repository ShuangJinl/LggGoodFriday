package com.thelgg.thejinx.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import com.thelgg.thejinx.game.JinxDeathScoreboard;
import com.thelgg.thejinx.game.JinxFeatureFlags;
import com.thelgg.thejinx.game.JinxGameState;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class JinxCommand {
    private static final String DEFAULT_JINX_NAME = "TheLgg_";

    private JinxCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("jinx")
                .requires(JinxCommand::isCommandAdmin)
                .then(CommandManager.literal("start")
                        .executes(context -> executeStart(context.getSource())))
                .then(CommandManager.literal("stop")
                        .executes(context -> executeStop(context.getSource())))
                .then(CommandManager.literal("status")
                        .executes(context -> executeStatus(context.getSource())))
                .then(CommandManager.literal("screenlift")
                        .executes(context -> executeScreenLiftQuery(context.getSource()))
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeScreenLiftSet(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled")
                                ))))
                .then(CommandManager.literal("set")
                        .executes(context -> executeSetWithDefault(context.getSource()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> executeSetByPlayer(context.getSource(), EntityArgumentType.getPlayer(context, "player"))))));
    }

    private static boolean isCommandAdmin(ServerCommandSource source) {
        if (source.getPermissions() == net.minecraft.command.permission.PermissionPredicate.ALL) {
            return true;
        }
        if (source.getPermissions() instanceof LeveledPermissionPredicate leveled) {
            return leveled.getLevel().isAtLeast(PermissionLevel.GAMEMASTERS);
        }
        return false;
    }

    private static int executeStart(ServerCommandSource source) {
        if (JinxGameState.isStarted()) {
            source.sendFeedback(() -> Text.empty()
                    .append(JinxText.game("倒霉蛋"))
                    .append(JinxText.muted(" 游戏已经处于 "))
                    .append(JinxText.accent("开启"))
                    .append(JinxText.muted(" 状态。")), false);
            return 1;
        }

        JinxGameState.setStarted(true);

        MinecraftServer server = source.getServer();
        if (!JinxGameState.hasAnyJinx(server)) {
            ServerPlayerEntity chosen = JinxGameState.pickDefaultOrRandom(server, DEFAULT_JINX_NAME);
            if (chosen != null) {
                JinxGameState.assignJinx(server, chosen);
                source.sendFeedback(() -> Text.empty()
                        .append(JinxText.good("游戏已开启 "))
                        .append(JinxText.muted("当前倒霉蛋："))
                        .append(JinxText.player(chosen)), true);
                announceGameStarted(server);
                JinxDeathScoreboard.show(server);
                return 1;
            }
        }

        source.sendFeedback(() -> Text.empty().append(JinxText.good("游戏已开启。")), true);
        announceGameStarted(server);
        JinxDeathScoreboard.show(server);
        return 1;
    }

    private static int executeSetWithDefault(ServerCommandSource source) {
        ServerPlayerEntity chosen = JinxGameState.pickDefaultOrRandom(source.getServer(), DEFAULT_JINX_NAME);
        if (chosen == null) {
            source.sendError(Text.empty().append(JinxText.warn("当前没有在线玩家，无法设置倒霉蛋。")));
            return 0;
        }

        JinxGameState.assignJinx(source.getServer(), chosen);
        source.sendFeedback(() -> Text.empty()
                .append(JinxText.accent("已设置唯一倒霉蛋："))
                .append(JinxText.player(chosen)), true);
        JinxFeedback.actionBar(
                chosen,
                Text.empty()
                        .append(JinxText.game("管理员"))
                        .append(JinxText.muted(" 将你设为 "))
                        .append(JinxText.warn("唯一倒霉蛋"))
                        .append(JinxText.muted("！"))
        );
        JinxFeedback.playSound(chosen, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 0.8F);
        return 1;
    }

    private static int executeStop(ServerCommandSource source) {
        if (!JinxGameState.isStarted()) {
            source.sendFeedback(() -> Text.empty()
                    .append(JinxText.muted("倒霉蛋游戏当前 "))
                    .append(JinxText.warn("未开启"))
                    .append(JinxText.muted("。")), false);
            return 1;
        }

        MinecraftServer server = source.getServer();
        JinxDeathScoreboard.hide(server);
        JinxGameState.setStarted(false);
        JinxGameState.clearJinx(server);
        source.sendFeedback(() -> Text.empty()
                .append(JinxText.good("倒霉蛋游戏已停止"))
                .append(JinxText.muted("，已清除所有倒霉蛋身份。")), true);
        JinxFeedback.broadcastTitle(
                server,
                JinxText.game("倒霉蛋游戏"),
                Text.empty().append(JinxText.muted("游戏 ")).append(JinxText.accent("已结束")),
                10,
                60,
                20
        );
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            JinxFeedback.playSound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.35F, 1.2F);
        }
        return 1;
    }

    private static void announceGameStarted(MinecraftServer server) {
        List<ServerPlayerEntity> jinxes = JinxGameState.getJinxPlayers(server);
        MutableText subtitle;
        if (jinxes.isEmpty()) {
            subtitle = Text.empty()
                    .append(JinxText.muted("游戏开始！"))
                    .append(JinxText.warn(" 当前未指定倒霉蛋。"));
        } else if (jinxes.size() == 1) {
            subtitle = Text.empty()
                    .append(JinxText.muted("当前倒霉蛋："))
                    .append(JinxText.player(jinxes.getFirst()));
        } else {
            subtitle = Text.empty()
                    .append(JinxText.muted("当前倒霉蛋（"))
                    .append(JinxText.gold(String.valueOf(jinxes.size())))
                    .append(JinxText.muted(" 人）："));
            for (int i = 0; i < jinxes.size(); i++) {
                if (i > 0) {
                    subtitle.append(JinxText.muted("、"));
                }
                subtitle.append(JinxText.player(jinxes.get(i)));
            }
        }

        JinxFeedback.broadcastTitle(
                server,
                JinxText.game("倒霉蛋游戏"),
                subtitle,
                10,
                70,
                20
        );
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            JinxFeedback.playSound(p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.15F, 1.6F);
        }
    }

    private static int executeStatus(ServerCommandSource source) {
        MutableText stateLine = Text.empty()
                .append(JinxText.accent("状态 "))
                .append(JinxText.muted("│ "))
                .append(JinxText.game("游戏 "))
                .append(JinxGameState.isStarted() ? JinxText.good("已开启") : JinxText.warn("未开启"))
                .append(JinxText.muted(" │ "))
                .append(JinxText.game("倒霉蛋 "));

        List<ServerPlayerEntity> jinxes = JinxGameState.getJinxPlayers(source.getServer());
        if (jinxes.isEmpty()) {
            stateLine.append(JinxText.muted("无"));
        } else {
            for (int i = 0; i < jinxes.size(); i++) {
                if (i > 0) {
                    stateLine.append(JinxText.muted("、"));
                }
                stateLine.append(JinxText.player(jinxes.get(i)));
            }
        }

        stateLine.append(JinxText.muted(" │ "))
                .append(JinxText.game("交互飞升 "))
                .append(JinxFeatureFlags.isScreenLiftEnabled() ? JinxText.good("开") : JinxText.muted("关"));

        MutableText finalLine = stateLine;
        source.sendFeedback(() -> finalLine, false);
        return 1;
    }

    private static int executeScreenLiftQuery(ServerCommandSource source) {
        boolean on = JinxFeatureFlags.isScreenLiftEnabled();
        source.sendFeedback(() -> Text.empty()
                .append(JinxText.linePrefix("倒霉蛋"))
                .append(JinxText.muted("交互飞升："))
                .append(on ? JinxText.good("开启 ") : JinxText.warn("关闭 "))
                .append(JinxText.muted("("))
                .append(JinxText.accent(Boolean.toString(on)))
                .append(JinxText.muted(")")), false);
        return 1;
    }

    private static int executeScreenLiftSet(ServerCommandSource source, boolean enabled) {
        JinxFeatureFlags.setScreenLiftEnabled(enabled);
        source.sendFeedback(() -> Text.empty()
                .append(JinxText.good("已" + (enabled ? "开启" : "关闭") + " "))
                .append(JinxText.accent("交互飞升"))
                .append(JinxText.muted("（倒霉蛋打开容器界面时的抬升效果）。")), true);
        return enabled ? 1 : 0;
    }

    private static int executeSetByPlayer(ServerCommandSource source, ServerPlayerEntity target) {
        JinxGameState.assignJinx(source.getServer(), target);
        source.sendFeedback(() -> Text.empty()
                .append(JinxText.accent("已设置唯一倒霉蛋："))
                .append(JinxText.player(target)), true);
        JinxFeedback.actionBar(
                target,
                Text.empty()
                        .append(JinxText.game("管理员"))
                        .append(JinxText.muted(" 将你设为 "))
                        .append(JinxText.warn("唯一倒霉蛋"))
                        .append(JinxText.muted("！"))
        );
        JinxFeedback.playSound(target, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 0.8F);
        return 1;
    }
}
