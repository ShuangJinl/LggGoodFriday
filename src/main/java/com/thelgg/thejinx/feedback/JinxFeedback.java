package com.thelgg.thejinx.feedback;

import com.thelgg.thejinx.data.LimbPart;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class JinxFeedback {
    private JinxFeedback() {
    }

    public static void actionBar(ServerPlayerEntity player, Text message) {
        player.sendMessageToClient(message, true);
    }

    public static void chat(ServerPlayerEntity player, Text message) {
        player.sendMessage(message, false);
    }

    public static void broadcastChat(MinecraftServer server, Text message) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(message, false);
        }
    }

    public static void playSound(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void playSoundAt(ServerWorld world, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, x, y, z, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void showTitle(ServerPlayerEntity player, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    public static void broadcastTitle(MinecraftServer server, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            showTitle(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public static void clearTitle(ServerPlayerEntity player, boolean reset) {
        player.networkHandler.sendPacket(new ClearTitleS2CPacket(reset));
    }

    public static Text limbDisplayName(LimbPart part) {
        return switch (part) {
            case HEAD -> Text.literal("头部").formatted(Formatting.RED, Formatting.BOLD);
            case LEFT_ARM -> Text.literal("左臂").formatted(Formatting.GOLD);
            case RIGHT_ARM -> Text.literal("右臂").formatted(Formatting.YELLOW);
            case LEFT_LEG -> Text.literal("左腿").formatted(Formatting.AQUA);
            case RIGHT_LEG -> Text.literal("右腿").formatted(Formatting.DARK_AQUA);
        };
    }
}
