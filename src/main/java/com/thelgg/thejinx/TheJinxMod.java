package com.thelgg.thejinx;

import com.thelgg.thejinx.command.JinxCommand;
import com.thelgg.thejinx.item.JinxItems;
import com.thelgg.thejinx.mechanic.AssimilationMechanic;
import com.thelgg.thejinx.mechanic.GlobalFatigueAndDeathFiestaMechanic;
import com.thelgg.thejinx.mechanic.HumanTntMechanic;
import com.thelgg.thejinx.mechanic.OreTripleDropMechanic;
import com.thelgg.thejinx.mechanic.ScreenLiftAndGravityMechanic;
import com.thelgg.thejinx.mechanic.SprintLimbLossMechanic;
import com.thelgg.thejinx.network.JinxNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheJinxMod implements ModInitializer {
    public static final String MOD_ID = "thejinx";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 模组初始化入口：先注册网络协议与服务端接收器。
        JinxItems.register();
        JinxNetworking.registerCommonPayloads();
        JinxNetworking.registerServerReceivers();
        JinxCommand.register();
        OreTripleDropMechanic.register();
        GlobalFatigueAndDeathFiestaMechanic.register();
        HumanTntMechanic.register();
        ScreenLiftAndGravityMechanic.register();
        AssimilationMechanic.register();
        SprintLimbLossMechanic.register();
        LOGGER.info("The Jinx initialized.");
    }
}
