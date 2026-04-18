package com.thelgg.thejinx.game;

import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 游戏开始时在侧边栏显示“死亡次数”榜，游戏结束时移除。
 */
public final class JinxDeathScoreboard {
    public static final String OBJECTIVE_ID = "thejinx_deaths";

    private JinxDeathScoreboard() {
    }

    public static void show(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        unsetOurSidebarIfPresent(scoreboard);
        ScoreboardObjective objective = scoreboard.addObjective(
                OBJECTIVE_ID,
                ScoreboardCriterion.DEATH_COUNT,
                Text.literal("死亡榜").formatted(Formatting.RED, Formatting.BOLD),
                ScoreboardCriterion.DEATH_COUNT.getDefaultRenderType(),
                false,
                null
        );
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
    }

    public static void hide(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        unsetOurSidebarIfPresent(scoreboard);
        ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE_ID);
        if (objective != null) {
            scoreboard.removeObjective(objective);
        }
    }

    private static void unsetOurSidebarIfPresent(ServerScoreboard scoreboard) {
        ScoreboardObjective slotObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (slotObjective != null && OBJECTIVE_ID.equals(slotObjective.getName())) {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
        }
    }
}
