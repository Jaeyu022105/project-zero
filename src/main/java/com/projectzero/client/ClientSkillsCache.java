package com.projectzero.client;

import com.projectzero.network.SyncSkillsPacket;
import com.projectzero.skills.PlayerSkills;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Local player's synced skill state and mana. No client-only imports so packet handlers can
 * safely reference this class on a dedicated server.
 */
public final class ClientSkillsCache {
    private ClientSkillsCache() {}

    private static PlayerSkills skills = new PlayerSkills();
    private static int maxMana = 100;
    private static Map<ResourceLocation, SyncSkillsPacket.SkillMeta> meta = Map.of();

    public static PlayerSkills get() {
        return skills;
    }

    public static int currentMana() {
        skills.ensureInitialized(maxMana);
        return skills.currentMana();
    }

    public static int maxMana() {
        return maxMana;
    }

    public static @Nullable SyncSkillsPacket.SkillMeta meta(ResourceLocation id) {
        return meta.get(id);
    }

    public static void set(PlayerSkills newSkills, int newMaxMana, Map<ResourceLocation, SyncSkillsPacket.SkillMeta> newMeta) {
        skills = newSkills;
        maxMana = Math.max(1, newMaxMana);
        skills.ensureInitialized(maxMana);
        meta = Map.copyOf(newMeta);
    }

    public static void tickClient() {
        skills.tickCooldowns();
    }

    public static void reset() {
        skills = new PlayerSkills();
        maxMana = 100;
        meta = Map.of();
    }
}
