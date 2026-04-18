package com.thelgg.thejinx.client;

import com.thelgg.thejinx.client.network.JinxClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class TheJinxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JinxClientNetworking.register();
    }
}
