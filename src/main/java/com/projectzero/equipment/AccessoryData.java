package com.projectzero.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.gates.GateRank;
import com.projectzero.jobs.JobRarity;
import com.projectzero.leveling.Stat;
import com.projectzero.registry.ModDataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Rolled stats stored as a MC 1.21 data component on an {@link AccessoryItem} stack:
 * rarity, gate rank (E..S), flat stat points, and at most one special affix line.
 */
public record AccessoryData(
        JobRarity rarity,
        GateRank rank,
        Map<Stat, Integer> stats,
        Optional<SpecialLine> special
) {
    public enum AffixType {
        CRIT_CHANCE("crit_chance"),
        MANA_REGEN("mana_regen"),
        LIFESTEAL("lifesteal"),
        XP_BONUS("xp_bonus"),
        DAMAGE_VS_UNDEAD("damage_vs_undead");

        private final String key;
        AffixType(String key) { this.key = key; }
        public String key() { return key; }

        public static AffixType byKey(String raw) {
            for (AffixType t : values()) {
                if (t.key.equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw)) return t;
            }
            return CRIT_CHANCE;
        }
    }

    public record SpecialLine(AffixType type, double value) {
        public static final Codec<SpecialLine> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.xmap(AffixType::byKey, AffixType::key).fieldOf("type").forGetter(SpecialLine::type),
                Codec.DOUBLE.fieldOf("value").forGetter(SpecialLine::value)
        ).apply(inst, SpecialLine::new));

        public Component formatTooltip() {
            return switch (type) {
                case CRIT_CHANCE -> Component.translatable("affix.projectzero.crit_chance",
                        String.format(Locale.ROOT, "%.0f", value * 100.0));
                case MANA_REGEN -> Component.translatable("affix.projectzero.mana_regen",
                        String.format(Locale.ROOT, "%.1f", value));
                case LIFESTEAL -> Component.translatable("affix.projectzero.lifesteal",
                        String.format(Locale.ROOT, "%.0f", value * 100.0));
                case XP_BONUS -> Component.translatable("affix.projectzero.xp_bonus",
                        String.format(Locale.ROOT, "%.0f", value * 100.0));
                case DAMAGE_VS_UNDEAD -> Component.translatable("affix.projectzero.damage_vs_undead",
                        String.format(Locale.ROOT, "%.0f", value * 100.0));
            };
        }
    }

    private static final Codec<JobRarity> RARITY_CODEC = Codec.STRING.xmap(
            s -> {
                for (JobRarity r : JobRarity.values()) {
                    if (r.name().equalsIgnoreCase(s)) return r;
                }
                return JobRarity.COMMON;
            },
            r -> r.name().toLowerCase(Locale.ROOT)
    );

    private static final Codec<Map<Stat, Integer>> STATS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
            raw -> {
                EnumMap<Stat, Integer> map = new EnumMap<>(Stat.class);
                for (Stat st : Stat.values()) {
                    Integer v = raw.get(st.key());
                    if (v != null && v != 0) map.put(st, v);
                }
                return Collections.unmodifiableMap(map);
            },
            map -> {
                Map<String, Integer> out = new HashMap<>();
                for (Map.Entry<Stat, Integer> e : map.entrySet()) {
                    if (e.getValue() != 0) out.put(e.getKey().key(), e.getValue());
                }
                return out;
            }
    );

    public static final Codec<AccessoryData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RARITY_CODEC.fieldOf("rarity").forGetter(AccessoryData::rarity),
            GateRank.CODEC.fieldOf("rank").forGetter(AccessoryData::rank),
            STATS_CODEC.fieldOf("stats").forGetter(AccessoryData::stats),
            SpecialLine.CODEC.optionalFieldOf("special").forGetter(AccessoryData::special)
    ).apply(inst, AccessoryData::new));

    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, AccessoryData> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of(AccessoryData::encode, AccessoryData::decode);

    public static void encode(FriendlyByteBuf buf, AccessoryData data) {
        buf.writeVarInt(data.rarity.ordinal());
        buf.writeVarInt(data.rank.ordinal());
        buf.writeVarInt(data.stats.size());
        for (Map.Entry<Stat, Integer> e : data.stats.entrySet()) {
            buf.writeVarInt(e.getKey().ordinal());
            buf.writeVarInt(e.getValue());
        }
        buf.writeBoolean(data.special.isPresent());
        if (data.special.isPresent()) {
            buf.writeVarInt(data.special.get().type().ordinal());
            buf.writeDouble(data.special.get().value());
        }
    }

    public static AccessoryData decode(FriendlyByteBuf buf) {
        JobRarity rarity = JobRarity.values()[Math.max(0, Math.min(JobRarity.values().length - 1, buf.readVarInt()))];
        GateRank rank = GateRank.values()[Math.max(0, Math.min(GateRank.values().length - 1, buf.readVarInt()))];
        int count = buf.readVarInt();
        EnumMap<Stat, Integer> stats = new EnumMap<>(Stat.class);
        for (int i = 0; i < count; i++) {
            Stat st = Stat.values()[Math.max(0, Math.min(Stat.values().length - 1, buf.readVarInt()))];
            stats.put(st, buf.readVarInt());
        }
        Optional<SpecialLine> special = Optional.empty();
        if (buf.readBoolean()) {
            AffixType type = AffixType.values()[Math.max(0, Math.min(AffixType.values().length - 1, buf.readVarInt()))];
            special = Optional.of(new SpecialLine(type, buf.readDouble()));
        }
        return new AccessoryData(rarity, rank, Collections.unmodifiableMap(stats), special);
    }

    public static @Nullable AccessoryData get(ItemStack stack) {
        return stack.get(ModDataComponents.ACCESSORY_DATA.get());
    }
}
