package com.thelgg.thejinx.data;

import com.thelgg.thejinx.TheJinxMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class JinxEntityComponents implements EntityComponentInitializer {
    public static final ComponentKey<JinxPlayerDataComponent> JINX_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(
            Identifier.of(TheJinxMod.MOD_ID, "jinx_data"),
            JinxPlayerDataComponent.class
    );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(JINX_DATA, JinxPlayerDataComponentImpl::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    public static JinxPlayerDataComponent get(PlayerEntity player) {
        return JINX_DATA.get(player);
    }
}
