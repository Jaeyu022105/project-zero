package com.projectzero.gates;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Gate and monster rank ladder E..S (DESIGN-DOCUMENT.md §9, §12).
 * Each rank doubles health/damage multipliers and defines a target player level range.
 */
public enum GateRank implements StringRepresentable {
    E(1.0, 1, 15, ChatFormatting.BLUE, 0xFF4CA8FF),
    D(2.0, 15, 30, ChatFormatting.GREEN, 0xFF55E075),
    C(4.0, 30, 50, ChatFormatting.YELLOW, 0xFFFFE04D),
    B(8.0, 50, 70, ChatFormatting.GOLD, 0xFFFF9A3C),
    A(16.0, 70, 90, ChatFormatting.DARK_PURPLE, 0xFFC060FF),
    S(32.0, 90, 100, ChatFormatting.RED, 0xFFFF4545);

    public static final Codec<GateRank> CODEC = Codec.STRING.xmap(
            s -> {
                GateRank r = byName(s);
                return r != null ? r : E;
            },
            r -> r.name().toLowerCase(Locale.ROOT)
    );

    public static final StreamCodec<ByteBuf, GateRank> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            ord -> values()[Math.max(0, Math.min(values().length - 1, ord))],
            Enum::ordinal
    );

    private final double multiplier;
    private final int minLevel;
    private final int maxLevel;
    private final ChatFormatting chatColor;
    private final int argb;

    GateRank(double multiplier, int minLevel, int maxLevel, ChatFormatting chatColor, int argb) {
        this.multiplier = multiplier;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.chatColor = chatColor;
        this.argb = argb;
    }

    /** Health, damage, and XP rank multiplier (x1 for E up to x32 for S). */
    public double multiplier() { return multiplier; }
    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public ChatFormatting chatColor() { return chatColor; }
    public int argb() { return argb; }

    public int randomLevel(RandomSource random) {
        return minLevel + random.nextInt(Math.max(1, maxLevel - minLevel + 1));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable GateRank byName(String raw) {
        if (raw == null) return null;
        for (GateRank r : values()) {
            if (r.name().equalsIgnoreCase(raw)) return r;
        }
        return null;
    }
}
