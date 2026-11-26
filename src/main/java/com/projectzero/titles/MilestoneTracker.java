package com.projectzero.titles;

import com.projectzero.ProjectZero;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks cumulative player milestones in persistent NBT under {@code projectzero:milestones}
 * (DESIGN-DOCUMENT.md §7).
 */
public final class MilestoneTracker {
    public static final String DATA_KEY = ProjectZero.MOD_ID + ":milestones";

    private final Map<ResourceLocation, Integer> killsByType = new HashMap<>();
    private int totalKills;
    private int bossKills;
    private int deaths;
    private int highestLevel = 1;
    private int gatesCleared;
    private int rerolls;

    public Map<ResourceLocation, Integer> killsByType() { return Collections.unmodifiableMap(killsByType); }
    public int totalKills() { return totalKills; }
    public int bossKills() { return bossKills; }
    public int deaths() { return deaths; }
    public int highestLevel() { return highestLevel; }
    public int gatesCleared() { return gatesCleared; }
    public int rerolls() { return rerolls; }

    public int killsOf(ResourceLocation entityTypeId) {
        return killsByType.getOrDefault(entityTypeId, 0);
    }

    public int killsOfTag(TagKey<EntityType<?>> tag) {
        int sum = 0;
        for (Map.Entry<ResourceLocation, Integer> e : killsByType.entrySet()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(e.getKey());
            if (type != null) {
                Holder<EntityType<?>> holder = ForgeRegistries.ENTITY_TYPES.getHolder(type).orElse(null);
                if (holder != null && holder.is(tag)) {
                    sum += e.getValue();
                }
            }
        }
        return sum;
    }

    public void addEntityKills(ResourceLocation typeId, int amount, boolean isBoss) {
        if (amount <= 0) return;
        killsByType.merge(typeId, amount, Integer::sum);
        totalKills += amount;
        if (isBoss) bossKills += amount;
    }

    public void addTotalKills(int amount) { if (amount > 0) totalKills += amount; }
    public void addBossKills(int amount) { if (amount > 0) bossKills += amount; }
    public void addDeaths(int amount) { if (amount > 0) deaths += amount; }
    public void updateHighestLevel(int level) { if (level > highestLevel) highestLevel = level; }
    public void addGatesCleared(int amount) { if (amount > 0) gatesCleared += amount; }
    public void addRerolls(int amount) { if (amount > 0) rerolls += amount; }

    // ---- persistence ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("totalKills", totalKills);
        tag.putInt("bossKills", bossKills);
        tag.putInt("deaths", deaths);
        tag.putInt("highestLevel", highestLevel);
        tag.putInt("gatesCleared", gatesCleared);
        tag.putInt("rerolls", rerolls);

        CompoundTag killsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> e : killsByType.entrySet()) {
            if (e.getValue() > 0) killsTag.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("killsByType", killsTag);
        return tag;
    }

    public static MilestoneTracker load(CompoundTag tag) {
        MilestoneTracker m = new MilestoneTracker();
        m.totalKills = Math.max(0, tag.getInt("totalKills"));
        m.bossKills = Math.max(0, tag.getInt("bossKills"));
        m.deaths = Math.max(0, tag.getInt("deaths"));
        m.highestLevel = Math.max(1, tag.getInt("highestLevel"));
        m.gatesCleared = Math.max(0, tag.getInt("gatesCleared"));
        m.rerolls = Math.max(0, tag.getInt("rerolls"));

        CompoundTag killsTag = tag.getCompound("killsByType");
        for (String key : killsTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int val = killsTag.getInt(key);
            if (id != null && val > 0) m.killsByType.put(id, val);
        }
        return m;
    }

    // ---- static helpers ----

    public static MilestoneTracker get(Player player) {
        return load(player.getPersistentData().getCompound(DATA_KEY));
    }

    public static void set(ServerPlayer player, MilestoneTracker tracker) {
        player.getPersistentData().put(DATA_KEY, tracker.save());
        TitleService.checkUnlocks(player, tracker);
    }

    public static void recordKill(ServerPlayer player, LivingEntity killed) {
        if (killed instanceof Player) return;
        if (killed.getPersistentData().getBoolean(com.projectzero.skills.SkillEffects.SUMMON_TAG)) return;
        MilestoneTracker m = get(player);
        ResourceLocation id = EntityType.getKey(killed.getType());
        boolean boss = killed.getType().is(Tags.EntityTypes.BOSSES)
                || killed.getPersistentData().getBoolean(ProjectZero.MOD_ID + ":gate_boss");
        m.addEntityKills(id, 1, boss);
        set(player, m);
    }

    public static void recordDeath(ServerPlayer player) {
        MilestoneTracker m = get(player);
        m.addDeaths(1);
        set(player, m);
    }

    public static void recordLevel(ServerPlayer player, int newLevel) {
        MilestoneTracker m = get(player);
        m.updateHighestLevel(newLevel);
        set(player, m);
    }

    public static void recordReroll(ServerPlayer player) {
        MilestoneTracker m = get(player);
        m.addRerolls(1);
        set(player, m);
    }

    public static void recordGateCleared(ServerPlayer player) {
        MilestoneTracker m = get(player);
        m.addGatesCleared(1);
        set(player, m);
    }

    public static void reset(ServerPlayer player) {
        player.getPersistentData().put(DATA_KEY, new MilestoneTracker().save());
    }

    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
