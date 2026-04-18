package com.thelgg.thejinx.item;

import com.thelgg.thejinx.TheJinxMod;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class JinxItems {
    public static Item ASSIMILATION;

    private JinxItems() {
    }

    public static void register() {
        Identifier id = Identifier.of(TheJinxMod.MOD_ID, "assimilation");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        ASSIMILATION = Registry.register(
                Registries.ITEM,
                id,
                new AssimilationItem(new Item.Settings().maxCount(1).registryKey(key))
        );
    }
}
