package com.projectzero.awakening;

import com.projectzero.leveling.LevelingRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Balance numbers and tier helpers for Awakening Stones and the Shard Vessel (DESIGN-DOCUMENT.md §6). */
public final class AwakeningRules {
    private AwakeningRules() {}

    /** Fraction of kill XP diverted into the first active Shard Vessel in the killer's inventory. */
    public static final double VESSEL_DIVERT = 0.5;

    /** Cumulative XP multiplier per tier relative to {@link LevelingRules#xpToNextLevel(int)}. */
    public static final double VESSEL_TIER_FRACTION = 0.6;

    /** Maximum charge tier on an Awakening Shard (0 = Uncharged .. 5 = Resplendent). */
    public static final int MAX_SHARD_TIER = 5;

    public static int clampTier(int tier) {
        return Math.max(0, Math.min(MAX_SHARD_TIER, tier));
    }

    /** Cumulative XP needed to reach {@code tier} (1..5) at {@code playerLevel}. Tier 0 needs 0 XP. */
    public static long thresholdForTier(int tier, int playerLevel) {
        int clamped = clampTier(tier);
        if (clamped <= 0) return 0L;
        return Math.max(1L, Math.round(clamped * VESSEL_TIER_FRACTION * LevelingRules.xpToNextLevel(playerLevel)));
    }

    /** Highest tier unlocked by {@code chargeXp} at {@code playerLevel}. */
    public static int tierForXp(long chargeXp, int playerLevel) {
        if (chargeXp <= 0) return 0;
        for (int t = MAX_SHARD_TIER; t >= 1; t--) {
            if (chargeXp >= thresholdForTier(t, playerLevel)) return t;
        }
        return 0;
    }

    public static ChatFormatting tierColor(int tier) {
        return switch (clampTier(tier)) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.AQUA;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GOLD;
        };
    }

    public static MutableComponent tierDisplayName(int tier) {
        int clamped = clampTier(tier);
        return Component.translatable("shard_tier.projectzero." + clamped).withStyle(tierColor(clamped));
    }
}
