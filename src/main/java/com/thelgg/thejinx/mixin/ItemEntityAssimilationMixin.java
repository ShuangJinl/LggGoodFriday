package com.thelgg.thejinx.mixin;

import com.thelgg.thejinx.item.JinxItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityAssimilationMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void theJinx$discardAssimilationDrop(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        World world = self.getEntityWorld();
        if (world.isClient()) {
            return;
        }

        if (JinxItems.ASSIMILATION != null && self.getStack().isOf(JinxItems.ASSIMILATION)) {
            self.discard();
            ci.cancel();
        }
    }
}

