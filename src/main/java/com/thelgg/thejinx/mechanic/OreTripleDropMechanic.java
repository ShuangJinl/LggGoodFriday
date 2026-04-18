package com.thelgg.thejinx.mechanic;

import com.thelgg.thejinx.feedback.JinxFeedback;
import com.thelgg.thejinx.feedback.JinxText;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class OreTripleDropMechanic {
    private OreTripleDropMechanic() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            if (!JinxRuleHelper.isActiveJinx(serverPlayer)) {
                return;
            }

            if (!isOre(state)) {
                return;
            }

            spawnExtraDrops((ServerWorld) world, serverPlayer, pos, state, blockEntity);
            JinxFeedback.actionBar(serverPlayer, Text.empty()
                    .append(JinxText.linePrefix("倒霉蛋"))
                    .append(JinxText.gold("矿物三倍掉落 "))
                    .append(JinxText.good("生效"))
                    .append(JinxText.muted("（额外×2）")));
            JinxFeedback.playSound(serverPlayer, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6F, 1.25F);
        });
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(ConventionalBlockTags.ORES);
    }

    private static void spawnExtraDrops(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        // 原版已掉落 1 倍，这里额外掉 2 倍，最终达到 3 倍掉落。
        for (ItemStack stack : Block.getDroppedStacks(state, world, pos, blockEntity, player, player.getMainHandStack())) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack extra = stack.copy();
            extra.setCount(stack.getCount() * 2);
            Block.dropStack(world, pos, extra);
        }
    }
}
