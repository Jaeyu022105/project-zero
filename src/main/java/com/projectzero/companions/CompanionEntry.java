package com.projectzero.companions;

import com.projectzero.leveling.Stat;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent roster entry; live entities are temporary representations of this data. */
public final class CompanionEntry {
    private final UUID id;
    private final CompanionKind kind;
    private final ResourceLocation entityType;
    private String name;
    private int level;
    private long xp;
    private final EnumMap<Stat, Integer> stats = new EnumMap<>(Stat.class);
    private int unspentPoints;
    private final LinkedHashMap<String, ItemStack> gear = new LinkedHashMap<>();
    private UUID liveEntityId;

    public CompanionEntry(UUID id, CompanionKind kind, ResourceLocation entityType, String name) {
        this.id = id;
        this.kind = kind;
        this.entityType = entityType;
        this.name = name;
        this.level = 1;
        for (Stat stat : Stat.values()) stats.put(stat, CompanionRules.BASE_STAT);
        for (String slot : CompanionRules.GEAR_SLOTS) gear.put(slot, ItemStack.EMPTY);
    }

    public UUID id() { return id; }
    public CompanionKind kind() { return kind; }
    public ResourceLocation entityType() { return entityType; }
    public String name() { return name; }
    public void setName(String value) { name = value; }
    public int level() { return level; }
    public long xp() { return xp; }
    public int unspentPoints() { return unspentPoints; }
    public int stat(Stat stat) { return stats.getOrDefault(stat, CompanionRules.BASE_STAT); }
    public Map<Stat, Integer> stats() { return Map.copyOf(stats); }
    public Map<String, ItemStack> gear() { return Map.copyOf(gear); }
    public UUID liveEntityId() { return liveEntityId; }
    public void setLiveEntityId(UUID value) { liveEntityId = value; }

    public int addXp(long amount) {
        if (amount <= 0 || level >= CompanionRules.MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < CompanionRules.MAX_LEVEL && xp >= CompanionRules.xpToNext(level)) {
            xp -= CompanionRules.xpToNext(level);
            level++;
            unspentPoints += CompanionRules.POINTS_PER_LEVEL;
            gained++;
        }
        return gained;
    }

    public boolean spendPoint(Stat stat) {
        if (unspentPoints <= 0 || stat(stat) >= CompanionRules.MAX_STAT_VALUE) return false;
        stats.put(stat, stat(stat) + 1);
        unspentPoints--;
        return true;
    }

    public int refundSpentPoints() {
        int refunded = 0;
        for (Stat stat : Stat.values()) {
            int spent = Math.max(0, stat(stat) - CompanionRules.BASE_STAT);
            refunded += spent;
            stats.put(stat, CompanionRules.BASE_STAT);
        }
        unspentPoints += refunded;
        return refunded;
    }

    public int gearBonus(Stat stat) {
        int total = 0;
        for (ItemStack stack : gear.values()) {
            if (!stack.isEmpty()) {
                com.projectzero.equipment.AccessoryData data = com.projectzero.equipment.AccessoryData.get(stack);
                if (data != null) total += data.stats().getOrDefault(stat, 0);
            }
        }
        return total;
    }

    public void setGear(String slot, ItemStack stack) {
        if (gear.containsKey(slot)) gear.put(slot, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putInt("Kind", kind.ordinal());
        tag.putString("EntityType", entityType.toString());
        tag.putString("Name", name);
        tag.putInt("Level", level);
        tag.putLong("Xp", xp);
        tag.putInt("Unspent", unspentPoints);
        CompoundTag statsTag = new CompoundTag();
        for (Stat stat : Stat.values()) statsTag.putInt(stat.key(), stat(stat));
        tag.put("Stats", statsTag);
        ListTag gearTag = new ListTag();
        for (Map.Entry<String, ItemStack> e : gear.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag item = new CompoundTag();
            item.putString("Slot", e.getKey());
            item.put("Stack", e.getValue().save(registries));
            gearTag.add(item);
        }
        tag.put("Gear", gearTag);
        if (liveEntityId != null) tag.putUUID("LiveEntity", liveEntityId);
        return tag;
    }

    public static CompanionEntry load(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("EntityType"));
        if (type == null) return null;
        CompanionEntry entry = new CompanionEntry(tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
                CompanionKind.byOrdinal(tag.getInt("Kind")), type,
                tag.getString("Name").isBlank() ? type.getPath() : tag.getString("Name"));
        entry.level = Math.max(1, Math.min(CompanionRules.MAX_LEVEL, tag.getInt("Level")));
        entry.xp = Math.max(0, tag.getLong("Xp"));
        entry.unspentPoints = Math.max(0, tag.getInt("Unspent"));
        CompoundTag statsTag = tag.getCompound("Stats");
        for (Stat stat : Stat.values()) {
            entry.stats.put(stat, Math.max(CompanionRules.BASE_STAT,
                    Math.min(CompanionRules.MAX_STAT_VALUE, statsTag.getInt(stat.key()))));
        }
        ListTag gearTag = tag.getList("Gear", Tag.TAG_COMPOUND);
        for (int i = 0; i < gearTag.size(); i++) {
            CompoundTag item = gearTag.getCompound(i);
            String slot = item.getString("Slot");
            if (entry.gear.containsKey(slot) && item.contains("Stack", Tag.TAG_COMPOUND)) {
                entry.setGear(slot, ItemStack.parseOptional(registries, item.getCompound("Stack")));
            }
        }
        if (tag.hasUUID("LiveEntity")) entry.liveEntityId = tag.getUUID("LiveEntity");
        return entry;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeVarInt(kind.ordinal());
        buf.writeResourceLocation(entityType);
        buf.writeUtf(name, CompanionRules.MAX_NAME_LENGTH);
        buf.writeVarInt(level);
        buf.writeLong(xp);
        buf.writeVarInt(unspentPoints);
        for (Stat stat : Stat.values()) buf.writeVarInt(stat(stat));
        buf.writeVarInt(gear.size());
        for (Map.Entry<String, ItemStack> e : gear.entrySet()) {
            buf.writeUtf(e.getKey(), 32);
            ItemStack stack = e.getValue();
            buf.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                buf.writeVarInt(stack.getCount());
            }
        }
        buf.writeBoolean(liveEntityId != null);
        if (liveEntityId != null) buf.writeUUID(liveEntityId);
    }

    public static CompanionEntry read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        CompanionKind kind = CompanionKind.byOrdinal(buf.readVarInt());
        ResourceLocation type = buf.readResourceLocation();
        CompanionEntry entry = new CompanionEntry(id, kind, type, buf.readUtf(CompanionRules.MAX_NAME_LENGTH));
        entry.level = Math.max(1, Math.min(CompanionRules.MAX_LEVEL, buf.readVarInt()));
        entry.xp = Math.max(0, buf.readLong());
        entry.unspentPoints = Math.max(0, buf.readVarInt());
        for (Stat stat : Stat.values()) {
            entry.stats.put(stat, Math.max(CompanionRules.BASE_STAT,
                    Math.min(CompanionRules.MAX_STAT_VALUE, buf.readVarInt())));
        }
        int gearCount = Math.min(CompanionRules.MAX_GEAR_SLOTS, Math.max(0, buf.readVarInt()));
        for (int i = 0; i < gearCount; i++) {
            String slot = buf.readUtf(32);
            if (!buf.readBoolean()) continue;
            ResourceLocation itemId = buf.readResourceLocation();
            int count = Math.max(1, Math.min(64, buf.readVarInt()));
            if (entry.gear.containsKey(slot) && BuiltInRegistries.ITEM.containsKey(itemId)) {
                entry.setGear(slot, new ItemStack(BuiltInRegistries.ITEM.get(itemId), count));
            }
        }
        if (buf.readBoolean()) entry.liveEntityId = buf.readUUID();
        return entry;
    }
}
