package com.projectzero.awakening;

import com.projectzero.client.ClientStatsCache;
import com.projectzero.leveling.LevelingService;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Holds one Awakening Shard and diverts half of the player's kill XP into charging it across
 * five tiers (DESIGN-DOCUMENT.md §6).
 *
 * Right-click with an Awakening Shard in the other hand to insert; sneak + right-click to extract.
 * Textures: shard_vessel (empty) and shard_vessel_t0..t5, picked by the projectzero:fill item property (ClientModEvents).
 */
public class ShardVesselItem extends Item {
    public ShardVesselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack vessel = player.getItemInHand(hand);
        ShardCharge charge = ShardCharge.get(vessel);

        if (player.isShiftKeyDown()) {
            if (!charge.hasShard()) {
                return InteractionResultHolder.pass(vessel);
            }
            if (!level.isClientSide()) {
                ItemStack extracted = ShardVesselService.createShardStack(1, charge.tier());
                vessel.remove(ModDataComponents.SHARD_CHARGE.get());
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(vessel, level.isClientSide());
        }

        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        if (!charge.hasShard() && other.is(ModItems.AWAKENING_SHARD.get())) {
            if (!level.isClientSide()) {
                int shardTier = ShardVesselService.getShardTier(other);
                int playerLevel = LevelingService.get(player).level();
                long initialXp = AwakeningRules.thresholdForTier(shardTier, playerLevel);
                vessel.set(ModDataComponents.SHARD_CHARGE.get(), new ShardCharge(true, initialXp, shardTier));
                other.shrink(1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 0.9F);
            }
            return InteractionResultHolder.sidedSuccess(vessel, level.isClientSide());
        }

        return InteractionResultHolder.pass(vessel);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ShardCharge charge = ShardCharge.get(stack);
        return (charge.hasShard() && charge.tier() > 0) || super.isFoil(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ShardCharge.get(stack).hasShard();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        ShardCharge charge = ShardCharge.get(stack);
        if (!charge.hasShard()) return 0;
        if (charge.tier() >= AwakeningRules.MAX_SHARD_TIER) return 13;
        int level = Math.max(1, ClientStatsCache.get().level());
        long prev = AwakeningRules.thresholdForTier(charge.tier(), level);
        long next = AwakeningRules.thresholdForTier(charge.tier() + 1, level);
        long span = Math.max(1L, next - prev);
        float progress = Mth.clamp((float) (charge.chargeXp() - prev) / span, 0.0F, 1.0F);
        return Math.round(progress * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int tier = ShardCharge.get(stack).tier();
        return tier >= AwakeningRules.MAX_SHARD_TIER ? 0xFFFFB020 : 0xFF3FD0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ShardCharge charge = ShardCharge.get(stack);
        if (!charge.hasShard()) {
            tooltip.add(Component.translatable("tooltip.projectzero.vessel.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        int level = Math.max(1, ClientStatsCache.get().level());
        int tier = charge.tier();
        tooltip.add(Component.translatable("tooltip.projectzero.vessel.tier",
                AwakeningRules.tierDisplayName(tier), tier, AwakeningRules.MAX_SHARD_TIER)
                .withStyle(ChatFormatting.GRAY));
        if (tier >= AwakeningRules.MAX_SHARD_TIER) {
            tooltip.add(Component.translatable("tooltip.projectzero.vessel.max", charge.chargeXp())
                    .withStyle(ChatFormatting.GOLD));
        } else {
            long nextThreshold = AwakeningRules.thresholdForTier(tier + 1, level);
            tooltip.add(Component.translatable("tooltip.projectzero.vessel.progress",
                    charge.chargeXp(), nextThreshold).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.projectzero.vessel.extract_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
