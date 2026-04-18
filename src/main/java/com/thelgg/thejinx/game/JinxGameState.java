package com.thelgg.thejinx.game;

import com.thelgg.thejinx.data.JinxPlayerData;
import com.thelgg.thejinx.data.JinxPlayerDataComponent;
import com.thelgg.thejinx.data.LimbPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class JinxGameState {
    private static boolean started = false;

    private JinxGameState() {
    }

    public static boolean isStarted() {
        return started;
    }

    public static void setStarted(boolean value) {
        started = value;
    }

    /** 倒霉蛋身份以各玩家 {@link JinxPlayerDataComponent#isJinx()} 为准，可多人同时成立。 */
    public static boolean isJinx(ServerPlayerEntity player) {
        return JinxPlayerData.get(player).isJinx();
    }

    /** 是否至少有一名在线倒霉蛋（用于开局选人等）。 */
    public static boolean hasAnyJinx(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (isJinx(p)) {
                return true;
            }
        }
        return false;
    }

    /** 所有在线倒霉蛋（顺序为玩家列表顺序）。 */
    public static List<ServerPlayerEntity> getJinxPlayers(MinecraftServer server) {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (isJinx(p)) {
                list.add(p);
            }
        }
        return list;
    }

    /**
     * 兼容旧逻辑：返回一名在线倒霉蛋（列表首位），用于标题等只需展示一人的场合；
     * 若无则 {@code null}。
     */
    public static ServerPlayerEntity getJinxPlayer(MinecraftServer server) {
        List<ServerPlayerEntity> list = getJinxPlayers(server);
        return list.isEmpty() ? null : list.getFirst();
    }

    /**
     * 管理员指定唯一倒霉蛋：清除全员倒霉蛋状态后，仅指定玩家成为倒霉蛋（并重置其倒霉蛋相关进度）。
     */
    public static void assignJinx(MinecraftServer server, ServerPlayerEntity newJinx) {
        if (newJinx == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            JinxPlayerDataComponent data = JinxPlayerData.get(player);
            data.setJinx(false);
        }

        applyFreshJinxState(JinxPlayerData.get(newJinx));
        newJinx.calculateDimensions();
    }

    /**
     * 同化等：在不剥夺其他倒霉蛋的前提下，使一名玩家也成为倒霉蛋（若已是则不变）。
     */
    public static void promoteToJinx(MinecraftServer server, ServerPlayerEntity newJinx) {
        if (newJinx == null || !isStarted()) {
            return;
        }

        JinxPlayerDataComponent data = JinxPlayerData.get(newJinx);
        if (data.isJinx()) {
            return;
        }

        applyFreshJinxState(data);
        newJinx.calculateDimensions();
    }

    private static void applyFreshJinxState(JinxPlayerDataComponent data) {
        data.setJinx(true);
        data.setJinxDeaths(0);
        data.setSprintProgressTicks(0);
        data.setRecoveryProgressTicks(0);
        for (LimbPart part : LimbPart.values()) {
            data.setMissingPart(part, false);
        }
    }

    public static void clearJinx(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            JinxPlayerDataComponent data = JinxPlayerData.get(player);
            data.setJinx(false);
        }
    }

    public static ServerPlayerEntity pickRandomOnlinePlayer(MinecraftServer server) {
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>(server.getPlayerManager().getPlayerList());
        if (onlinePlayers.isEmpty()) {
            return null;
        }
        int index = server.getOverworld().random.nextInt(onlinePlayers.size());
        return onlinePlayers.get(index);
    }

    public static ServerPlayerEntity pickDefaultOrRandom(MinecraftServer server, String defaultName) {
        ServerPlayerEntity namedPlayer = server.getPlayerManager().getPlayer(defaultName);
        if (namedPlayer != null) {
            return namedPlayer;
        }
        return pickRandomOnlinePlayer(server);
    }
}
