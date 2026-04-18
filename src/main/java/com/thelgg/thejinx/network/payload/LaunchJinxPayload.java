package com.thelgg.thejinx.network.payload;

import com.thelgg.thejinx.TheJinxMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LaunchJinxPayload() implements CustomPayload {
    public static final Id<LaunchJinxPayload> ID = new Id<>(Identifier.of(TheJinxMod.MOD_ID, "launch_jinx"));
    public static final PacketCodec<RegistryByteBuf, LaunchJinxPayload> CODEC = PacketCodec.unit(new LaunchJinxPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
