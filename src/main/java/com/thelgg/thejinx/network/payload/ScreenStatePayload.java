package com.thelgg.thejinx.network.payload;

import com.thelgg.thejinx.TheJinxMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ScreenStatePayload(boolean screenOpen) implements CustomPayload {
    public static final CustomPayload.Id<ScreenStatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(TheJinxMod.MOD_ID, "screen_state")
    );

    public static final PacketCodec<RegistryByteBuf, ScreenStatePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOLEAN, ScreenStatePayload::screenOpen, ScreenStatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
