package com.projectzero.titles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Player's unlocked titles and single currently equipped title (NBT {@code projectzero:titles}).
 */
public final class PlayerTitles {
    private final LinkedHashSet<ResourceLocation> unlocked = new LinkedHashSet<>();
    private @Nullable ResourceLocation equipped;

    public Set<ResourceLocation> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    public List<ResourceLocation> unlockedList() {
        return new ArrayList<>(unlocked);
    }

    public boolean hasUnlocked(ResourceLocation id) {
        return unlocked.contains(id);
    }

    public boolean unlock(ResourceLocation id) {
        return unlocked.add(id);
    }

    public @Nullable ResourceLocation equipped() {
        return equipped;
    }

    public boolean equip(@Nullable ResourceLocation id) {
        if (id != null && !unlocked.contains(id)) return false;
        this.equipped = id;
        return true;
    }

    /** Cycles None -> title_0 -> title_1 -> ... -> None. */
    public @Nullable ResourceLocation cycleNext() {
        if (unlocked.isEmpty()) {
            equipped = null;
            return null;
        }
        List<ResourceLocation> list = unlockedList();
        if (equipped == null) {
            equipped = list.get(0);
        } else {
            int idx = list.indexOf(equipped);
            if (idx < 0 || idx + 1 >= list.size()) {
                equipped = null;
            } else {
                equipped = list.get(idx + 1);
            }
        }
        return equipped;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ResourceLocation id : unlocked) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("unlocked", list);
        if (equipped != null) {
            tag.putString("equipped", equipped.toString());
        }
        return tag;
    }

    public static PlayerTitles load(CompoundTag tag) {
        PlayerTitles t = new PlayerTitles();
        ListTag list = tag.getList("unlocked", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) t.unlocked.add(id);
        }
        if (tag.contains("equipped")) {
            ResourceLocation eq = ResourceLocation.tryParse(tag.getString("equipped"));
            if (eq != null && t.unlocked.contains(eq)) t.equipped = eq;
        }
        return t;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(unlocked.size());
        for (ResourceLocation id : unlocked) {
            buf.writeResourceLocation(id);
        }
        buf.writeBoolean(equipped != null);
        if (equipped != null) buf.writeResourceLocation(equipped);
    }

    public static PlayerTitles read(FriendlyByteBuf buf) {
        PlayerTitles t = new PlayerTitles();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            t.unlocked.add(buf.readResourceLocation());
        }
        if (buf.readBoolean()) {
            t.equipped = buf.readResourceLocation();
        }
        return t;
    }
}
