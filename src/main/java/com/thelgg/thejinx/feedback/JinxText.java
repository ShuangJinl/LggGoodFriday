package com.thelgg.thejinx.feedback;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 统一的中文提示配色与样式（动作栏 / 聊天 / 标题片段）。
 */
public final class JinxText {
    public static final Style STYLE_GAME = Style.EMPTY.withColor(Formatting.LIGHT_PURPLE).withBold(true);
    public static final Style STYLE_ACCENT = Style.EMPTY.withColor(Formatting.AQUA);
    public static final Style STYLE_WARN = Style.EMPTY.withColor(Formatting.RED);
    public static final Style STYLE_GOOD = Style.EMPTY.withColor(Formatting.GREEN);
    public static final Style STYLE_MUTED = Style.EMPTY.withColor(Formatting.GRAY);
    public static final Style STYLE_GOLD = Style.EMPTY.withColor(Formatting.GOLD);
    public static final Style STYLE_YELLOW = Style.EMPTY.withColor(Formatting.YELLOW);

    private JinxText() {
    }

    public static MutableText game(String content) {
        return Text.literal(content).setStyle(STYLE_GAME);
    }

    public static MutableText accent(String content) {
        return Text.literal(content).setStyle(STYLE_ACCENT);
    }

    public static MutableText warn(String content) {
        return Text.literal(content).setStyle(STYLE_WARN);
    }

    public static MutableText good(String content) {
        return Text.literal(content).setStyle(STYLE_GOOD);
    }

    public static MutableText muted(String content) {
        return Text.literal(content).setStyle(STYLE_MUTED);
    }

    public static MutableText gold(String content) {
        return Text.literal(content).setStyle(STYLE_GOLD);
    }

    /** 玩家显示名（保留队伍前缀等）。 */
    public static Text player(ServerPlayerEntity player) {
        return player.getDisplayName().copy();
    }

    public static MutableText linePrefix(String label) {
        return Text.empty()
                .append(Text.literal("「").setStyle(STYLE_MUTED))
                .append(Text.literal(label).setStyle(STYLE_GAME))
                .append(Text.literal("」 ").setStyle(STYLE_MUTED));
    }
}
