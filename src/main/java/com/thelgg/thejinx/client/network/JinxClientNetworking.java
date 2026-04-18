package com.thelgg.thejinx.client.network;

import com.thelgg.thejinx.network.payload.ScreenStatePayload;
import com.thelgg.thejinx.network.payload.LaunchJinxPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class JinxClientNetworking {
    private static Boolean lastScreenOpen = null;
    private static boolean lastAttackPressed = false;

    private JinxClientNetworking() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JinxClientNetworking::syncScreenStateIfNeeded);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastScreenOpen = null;
            lastAttackPressed = false;
        });
    }

    private static void syncScreenStateIfNeeded(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        boolean screenOpen = client.currentScreen != null;
        if (lastScreenOpen == null || lastScreenOpen != screenOpen) {
            ClientPlayNetworking.send(new ScreenStatePayload(screenOpen));
            lastScreenOpen = screenOpen;
        }

        boolean attackPressed = client.options.attackKey.isPressed();
        if (attackPressed && !lastAttackPressed && client.currentScreen == null) {
            ClientPlayNetworking.send(new LaunchJinxPayload());
        }
        lastAttackPressed = attackPressed;
    }
}
