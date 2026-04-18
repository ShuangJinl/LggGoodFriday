package com.thelgg.thejinx.network;

import com.thelgg.thejinx.TheJinxMod;
import com.thelgg.thejinx.data.JinxEntityComponents;
import com.thelgg.thejinx.mechanic.HumanTntMechanic;
import com.thelgg.thejinx.mechanic.ScreenLiftAndGravityMechanic;
import com.thelgg.thejinx.network.payload.LaunchJinxPayload;
import com.thelgg.thejinx.network.payload.ScreenStatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class JinxNetworking {
    private JinxNetworking() {
    }

    public static void registerCommonPayloads() {
        PayloadTypeRegistry.playC2S().register(ScreenStatePayload.ID, ScreenStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LaunchJinxPayload.ID, LaunchJinxPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ScreenStatePayload.ID, (payload, context) -> context.server().execute(() -> {
            boolean oldState = JinxEntityComponents.get(context.player()).isScreenOpen();
            JinxEntityComponents.get(context.player()).setScreenOpen(payload.screenOpen());
            ScreenLiftAndGravityMechanic.onScreenStateSync(context.player(), oldState, payload.screenOpen());
            TheJinxMod.LOGGER.debug("Screen state synced: player={}, open={}", context.player().getName().getString(), payload.screenOpen());
        }));
        ServerPlayNetworking.registerGlobalReceiver(LaunchJinxPayload.ID, (payload, context) -> context.server().execute(() -> {
            HumanTntMechanic.tryLaunchPassengerJinx(context.player());
        }));
    }
}
