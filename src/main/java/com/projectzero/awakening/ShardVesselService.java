package com.projectzero.awakening;

import com.projectzero.leveling.LevelingService;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModItems;
import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side logic for Awakening Shard tiers and the Shard Vessel (DESIGN-DOCUMENT.md §6).
 */
public final class ShardVesselService {
    private ShardVesselService() {}

    /** Reads the charge tier (0..5) from an Awakening Shard stack. */
    public static int getShardTier(ItemStack shard) {
        if (shard.isEmpty()) return 0;
        Integer tier = shard.get(ModDataComponents.SHARD_CHARGE_TIER.get());
        return tier == null ? 0 : AwakeningRules.clampTier(tier);
    }

    /** Sets the charge tier on an Awakening Shard stack. Tier 0 removes the component so default shards stack. */
    public static void setShardTier(ItemStack shard, int tier) {
        int clamped = AwakeningRules.clampTier(tier);
        if (clamped == 0) {
            shard.remove(ModDataComponents.SHARD_CHARGE_TIER.get());
        } else {
            shard.set(ModDataComponents.SHARD_CHARGE_TIER.get(), clamped);
        }
    }

    /** Creates an Awakening Shard stack of the given count and charge tier. */
    public static ItemStack createShardStack(int count, int tier) {
        ItemStack stack = new ItemStack(ModItems.AWAKENING_SHARD.get(), count);
        setShardTier(stack, tier);
        return stack;
    }

    /** Finds the first Shard Vessel holding a shard in the player's inventory (main inventory, then offhand). */
    public static @Nullable ItemStack findActiveVessel(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (isHoldingShard(stack)) return stack;
        }
        for (ItemStack stack : inv.offhand) {
            if (isHoldingShard(stack)) return stack;
        }
        return null;
    }

    public static boolean isHoldingShard(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SHARD_VESSEL.get()) && ShardCharge.get(stack).hasShard();
    }

    /**
     * Diverts {@link AwakeningRules#VESSEL_DIVERT} of {@code killXp} into the first Shard Vessel
     * holding a non-maxed shard in the killer's inventory, and returns the remaining XP for the player.
     */
    public static long divertKillXp(ServerPlayer player, long killXp) {
        if (killXp <= 1) return killXp;
        ItemStack vessel = findChargingVessel(player);
        if (vessel == null) return killXp;

        long diverted = (long) Math.floor(killXp * AwakeningRules.VESSEL_DIVERT);
        if (diverted <= 0) return killXp;

        addCharge(player, vessel, diverted);
        return killXp - diverted;
    }

    private static @Nullable ItemStack findChargingVessel(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (isCharging(stack)) return stack;
        }
        for (ItemStack stack : inv.offhand) {
            if (isCharging(stack)) return stack;
        }
        return null;
    }

    private static boolean isCharging(ItemStack stack) {
        if (!isHoldingShard(stack)) return false;
        return ShardCharge.get(stack).tier() < AwakeningRules.MAX_SHARD_TIER;
    }

    /** Adds {@code xp} charge to a Shard Vessel holding a shard, updating its tier and notifying on tier-up. */
    public static ShardCharge addCharge(ServerPlayer player, ItemStack vessel, long xp) {
        ShardCharge current = ShardCharge.get(vessel);
        if (!current.hasShard() || xp <= 0) return current;

        int level = LevelingService.get(player).level();
        long updatedXp = Math.max(0L, current.chargeXp() + xp);
        int computedTier = AwakeningRules.tierForXp(updatedXp, level);
        int updatedTier = Math.max(current.tier(), computedTier);

        ShardCharge next = new ShardCharge(true, updatedXp, updatedTier);
        vessel.set(ModDataComponents.SHARD_CHARGE.get(), next);

        if (updatedTier > current.tier()) {
            Component body = AwakeningRules.tierDisplayName(updatedTier).copy()
                    .append(Component.literal(" (Tier " + updatedTier + "/" + AwakeningRules.MAX_SHARD_TIER + ")")
                            .withStyle(ChatFormatting.GRAY));
            Component chat = Component.literal("Your Awakening Shard has reached ").withStyle(ChatFormatting.WHITE)
                    .append(body);
            SystemNotices.system(player, NoticeType.ITEM, Component.translatable("notice.projectzero.vessel_tier"), body, chat);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F + updatedTier * 0.12F);
        }
        return next;
    }
}
