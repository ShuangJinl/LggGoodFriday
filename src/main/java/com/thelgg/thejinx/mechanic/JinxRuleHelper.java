package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.game.JinxGameState;
import net.minecraft.server.network.ServerPlayerEntity;

public final class JinxRuleHelper {
    private JinxRuleHelper() {
    }

    public static boolean isActiveJinx(ServerPlayerEntity player) {
        return JinxGameState.isStarted() && JinxGameState.isJinx(player);
    }
}
