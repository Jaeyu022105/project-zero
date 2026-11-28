package com.projectzero.companions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Saved player companion roster; entries travel with the player across dimensions. */
public final class PlayerCompanions {
    private final LinkedHashMap<UUID, CompanionEntry> entries = new LinkedHashMap<>();

    public Collection<CompanionEntry> entries() { return java.util.List.copyOf(entries.values()); }
    public CompanionEntry get(UUID id) { return entries.get(id); }
    public void add(CompanionEntry entry) { entries.put(entry.id(), entry); }
    public boolean remove(UUID id) { return entries.remove(id) != null; }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (CompanionEntry entry : entries.values()) list.add(entry.save(registries));
        root.put("Entries", list);
        return root;
    }

    public static PlayerCompanions load(CompoundTag root, HolderLookup.Provider registries) {
        PlayerCompanions roster = new PlayerCompanions();
        ListTag list = root.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompanionEntry entry = CompanionEntry.load(list.getCompound(i), registries);
            if (entry != null) roster.add(entry);
        }
        return roster;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (CompanionEntry entry : entries.values()) entry.write(buf);
    }

    public static PlayerCompanions read(FriendlyByteBuf buf) {
        PlayerCompanions roster = new PlayerCompanions();
        int count = Math.min(256, Math.max(0, buf.readVarInt()));
        for (int i = 0; i < count; i++) {
            CompanionEntry entry = CompanionEntry.read(buf);
            if (entry != null) roster.add(entry);
        }
        return roster;
    }
}
