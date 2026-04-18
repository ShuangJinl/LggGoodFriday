package com.thelgg.thejinx.data;

import net.minecraft.entity.player.PlayerEntity;

public final class JinxPlayerData {
    private JinxPlayerData() {
    }

    public static JinxPlayerDataComponent get(PlayerEntity player) {
        return JinxEntityComponents.get(player);
    }
}
