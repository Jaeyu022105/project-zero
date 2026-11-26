package com.projectzero.awakening;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Awakening Shard item with optional charge tier (0..5). Charged shards stack only with shards
 * of the same tier and shift Awakening Stone job rerolls toward rarer jobs.
 */
public class AwakeningShardItem extends Item {
    public AwakeningShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ShardVesselService.getShardTier(stack) > 0 || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int tier = ShardVesselService.getShardTier(stack);
        tooltip.add(Component.translatable("tooltip.projectzero.shard.tier",
                AwakeningRules.tierDisplayName(tier), tier, AwakeningRules.MAX_SHARD_TIER)
                .withStyle(ChatFormatting.GRAY));
        if (tier > 0) {
            tooltip.add(Component.translatable("tooltip.projectzero.shard.charged_hint")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
