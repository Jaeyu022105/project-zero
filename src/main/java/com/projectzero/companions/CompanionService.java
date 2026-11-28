package com.projectzero.companions;

import com.projectzero.ProjectZero;
import com.projectzero.leveling.Stat;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncCompanionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Server-side API for the persistent companion roster. */
public final class CompanionService {
    private CompanionService() {}

    public static final String DATA_KEY = ProjectZero.MOD_ID + ":companions";
    public static final String OWNER_TAG = ProjectZero.MOD_ID + ":companion_owner";
    public static final String ENTRY_TAG = ProjectZero.MOD_ID + ":companion_entry";
    public static final String HIT_TIME_TAG = ProjectZero.MOD_ID + ":companion_hit_time";
    public static final String HIT_OWNER_TAG = ProjectZero.MOD_ID + ":companion_hit_owner";
    public static final String HIT_ENTRY_TAG = ProjectZero.MOD_ID + ":companion_hit_entry";

    public static PlayerCompanions get(Player player) {
        return PlayerCompanions.load(player.getPersistentData().getCompound(DATA_KEY), player.registryAccess());
    }

    public static void set(ServerPlayer player, PlayerCompanions roster) {
        player.getPersistentData().put(DATA_KEY, roster.save(player.registryAccess()));
        sync(player, roster);
    }

    public static void sync(ServerPlayer player) {
        sync(player, get(player));
    }

    private static void sync(ServerPlayer player, PlayerCompanions roster) {
        ModNetwork.sendToPlayer(player, new SyncCompanionsPacket(roster));
    }

    public static void copy(Player from, Player to) {
        CompoundTag tag = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, tag.copy());
    }

    public static CompanionEntry add(ServerPlayer player, CompanionKind kind, ResourceLocation entityType, String name) {
        PlayerCompanions roster = get(player);
        String cleanName = name == null ? "" : name.strip();
        if (cleanName.isEmpty()) cleanName = entityType.getPath();
        cleanName = cleanName.substring(0, Math.min(cleanName.length(), CompanionRules.MAX_NAME_LENGTH));
        CompanionEntry entry = new CompanionEntry(UUID.randomUUID(), kind, entityType, cleanName);
        roster.add(entry);
        set(player, roster);
        return entry;
    }

    public static @Nullable CompanionEntry find(PlayerCompanions roster, UUID id) {
        return roster.get(id);
    }

    public static @Nullable CompanionEntry findByName(PlayerCompanions roster, String name) {
        return roster.entries().stream().filter(e -> e.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static @Nullable CompanionEntry spiritHound(ServerPlayer player, boolean create) {
        PlayerCompanions roster = get(player);
        ResourceLocation wolfId = ResourceLocation.withDefaultNamespace("wolf");
        CompanionEntry existing = roster.entries().stream()
                .filter(e -> e.kind() == CompanionKind.SUMMON && e.entityType().equals(wolfId)
                        && e.name().equals("Spirit Hound"))
                .findFirst().orElse(null);
        if (existing != null || !create) return existing;
        CompanionEntry entry = new CompanionEntry(UUID.randomUUID(), CompanionKind.SUMMON, wolfId, "Spirit Hound");
        roster.add(entry);
        set(player, roster);
        return entry;
    }

    public static void markSpawned(ServerPlayer player, CompanionEntry entry, Entity entity, boolean primary) {
        CompoundTag data = entity.getPersistentData();
        data.putUUID(OWNER_TAG, player.getUUID());
        data.putUUID(ENTRY_TAG, entry.id());
        if (primary) {
            PlayerCompanions roster = get(player);
            CompanionEntry current = roster.get(entry.id());
            if (current != null) {
                current.setLiveEntityId(entity.getUUID());
                set(player, roster);
            }
        }
    }

    public static void applySpiritHoundStats(Wolf wolf, CompanionEntry entry) {
        var health = wolf.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(CompanionRules.houndHealth(entry) + entry.gearBonus(Stat.VITALITY));
        var damage = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(CompanionRules.houndDamage(entry) + entry.gearBonus(Stat.STRENGTH));
        wolf.setHealth(wolf.getMaxHealth());
    }

    public static void markHit(Entity attacker, Entity target) {
        CompoundTag source = attacker.getPersistentData();
        if (!source.hasUUID(OWNER_TAG) || !source.hasUUID(ENTRY_TAG)) return;
        CompoundTag data = target.getPersistentData();
        data.putLong(HIT_TIME_TAG, target.level().getGameTime());
        data.putUUID(HIT_OWNER_TAG, source.getUUID(OWNER_TAG));
        data.putUUID(HIT_ENTRY_TAG, source.getUUID(ENTRY_TAG));
    }

    public static void awardKill(Entity killer, Entity victim) {
        if (victim instanceof Player || victim.getPersistentData().getBoolean(com.projectzero.skills.SkillEffects.SUMMON_TAG)) return;
        CompoundTag source = killer.getPersistentData();
        UUID ownerId = source.hasUUID(OWNER_TAG) ? source.getUUID(OWNER_TAG) : null;
        UUID entryId = source.hasUUID(ENTRY_TAG) ? source.getUUID(ENTRY_TAG) : null;
        if (ownerId == null || entryId == null) {
            CompoundTag victimData = victim.getPersistentData();
            long elapsed = victim.level().getGameTime() - victimData.getLong(HIT_TIME_TAG);
            if (!victimData.hasUUID(HIT_OWNER_TAG) || !victimData.hasUUID(HIT_ENTRY_TAG)
                    || elapsed < 0 || elapsed > CompanionRules.OWNER_ASSIST_TICKS) return;
            ownerId = victimData.getUUID(HIT_OWNER_TAG);
            entryId = victimData.getUUID(HIT_ENTRY_TAG);
        }
        net.minecraft.server.MinecraftServer server = ((ServerLevel) victim.level()).getServer();
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) gainXp(owner, entryId, CompanionRules.XP_PER_KILL);
    }

    public static boolean gainXp(ServerPlayer player, UUID id, long amount) {
        PlayerCompanions roster = get(player);
        CompanionEntry entry = roster.get(id);
        if (entry == null || amount <= 0) return false;
        int levels = entry.addXp(amount);
        if (levels > 0) {
            for (int i = 0; i < levels * CompanionRules.POINTS_PER_LEVEL; i++) {
                Stat stat = Stat.values()[player.getRandom().nextInt(Stat.values().length)];
                while (entry.stat(stat) >= CompanionRules.MAX_STAT_VALUE) {
                    stat = Stat.values()[player.getRandom().nextInt(Stat.values().length)];
                }
                entry.spendPoint(stat);
            }
        }
        set(player, roster);
        return true;
    }

    public static void clearLiveId(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(OWNER_TAG) || !data.hasUUID(ENTRY_TAG)) return;
        net.minecraft.server.MinecraftServer server = entity.level() instanceof ServerLevel level ? level.getServer() : null;
        if (server == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(data.getUUID(OWNER_TAG));
        if (owner == null) return;
        PlayerCompanions roster = get(owner);
        CompanionEntry entry = roster.get(data.getUUID(ENTRY_TAG));
        if (entry != null && entity.getUUID().equals(entry.liveEntityId())) {
            entry.setLiveEntityId(null);
            set(owner, roster);
        }
    }
}
