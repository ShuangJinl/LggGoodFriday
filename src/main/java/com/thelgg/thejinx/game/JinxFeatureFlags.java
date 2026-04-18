package com.thelgg.thejinx.game;

/**
 * 与游戏是否开启无关的全局功能开关，可在任意时刻由管理员指令调整。
 */
public final class JinxFeatureFlags {
    private static volatile boolean screenLiftEnabled = true;

    private JinxFeatureFlags() {
    }

    public static boolean isScreenLiftEnabled() {
        return screenLiftEnabled;
    }

    public static void setScreenLiftEnabled(boolean enabled) {
        screenLiftEnabled = enabled;
    }
}
