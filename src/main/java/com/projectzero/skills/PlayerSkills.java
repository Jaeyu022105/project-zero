package com.projectzero.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A player's mana, learned skills, 4 active slots, and remaining skill cooldowns (NBT {@code projectzero:skills}).
 */
public final class PlayerSkills {
    private double mana = -1.0; // -1 means uninitialized; filled to maxMana on first load
    private final LinkedHashSet<ResourceLocation> learned = new LinkedHashSet<>();
    private final ResourceLocation[] slots = new ResourceLocation[SkillRules.ACTIVE_SLOTS];
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();

    public double rawMana() { return mana; }
    public int currentMana() { return Math.max(0, (int) Math.floor(mana)); }
    public void setMana(double value, int maxMana) {
        this.mana = Math.max(0.0, Math.min(maxMana, value));
    }

    public void ensureInitialized(int maxMana) {
        if (mana < 0.0) {
            mana = maxMana;
        } else if (mana > maxMana) {
            mana = maxMana;
        }
    }

    public Set<ResourceLocation> learned() {
        return Collections.unmodifiableSet(learned);
    }

    public boolean hasLearned(ResourceLocation id) {
        return learned.contains(id);
    }

    public boolean addSkill(ResourceLocation id, boolean autoSlotActive) {
        boolean added = learned.add(id);
        if (added && autoSlotActive && slotOf(id) == -1) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == null) {
                    slots[i] = id;
                    break;
                }
            }
        }
        return added;
    }

    public boolean removeSkill(ResourceLocation id) {
        boolean removed = learned.remove(id);
        if (removed) {
            for (int i = 0; i < slots.length; i++) {
                if (id.equals(slots[i])) slots[i] = null;
            }
            cooldowns.remove(id);
        }
        return removed;
    }

    public @Nullable ResourceLocation slot(int index) {
        if (index < 0 || index >= slots.length) return null;
        return slots[index];
    }

    public void setSlot(int index, @Nullable ResourceLocation skillId) {
        if (index < 0 || index >= slots.length) return;
        if (skillId != null) {
            for (int i = 0; i < slots.length; i++) {
                if (skillId.equals(slots[i])) slots[i] = null;
            }
        }
        slots[index] = skillId;
    }

    public int slotOf(ResourceLocation skillId) {
        for (int i = 0; i < slots.length; i++) {
            if (skillId.equals(slots[i])) return i;
        }
        return -1;
    }

    public int cooldownRemaining(ResourceLocation skillId) {
        return cooldowns.getOrDefault(skillId, 0);
    }

    public void setCooldown(ResourceLocation skillId, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(skillId);
        } else {
            cooldowns.put(skillId, ticks);
        }
    }

    /** Decrements all active cooldowns by 1 tick. Returns true if any cooldown reached 0. */
    public boolean tickCooldowns() {
        if (cooldowns.isEmpty()) return false;
        boolean finishedAny = false;
        var it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int next = e.getValue() - 1;
            if (next <= 0) {
                it.remove();
                finishedAny = true;
            } else {
                e.setValue(next);
            }
        }
        return finishedAny;
    }

    public Map<ResourceLocation, Integer> cooldownsView() {
        return Collections.unmodifiableMap(cooldowns);
    }

    // ---- persistence ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("mana", mana);

        ListTag learnedList = new ListTag();
        for (ResourceLocation id : learned) {
            learnedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("learned", learnedList);

        CompoundTag slotTag = new CompoundTag();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) slotTag.putString("s" + i, slots[i].toString());
        }
        tag.put("slots", slotTag);

        CompoundTag cdTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> e : cooldowns.entrySet()) {
            if (e.getValue() > 0) cdTag.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("cooldowns", cdTag);
        return tag;
    }

    public static PlayerSkills load(CompoundTag tag) {
        PlayerSkills s = new PlayerSkills();
        if (tag.contains("mana")) {
            s.mana = tag.getDouble("mana");
        }
        ListTag learnedList = tag.getList("learned", Tag.TAG_STRING);
        for (int i = 0; i < learnedList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(learnedList.getString(i));
            if (id != null) s.learned.add(id);
        }
        CompoundTag slotTag = tag.getCompound("slots");
        for (int i = 0; i < s.slots.length; i++) {
            String key = "s" + i;
            if (slotTag.contains(key)) {
                ResourceLocation id = ResourceLocation.tryParse(slotTag.getString(key));
                if (id != null && s.learned.contains(id)) s.slots[i] = id;
            }
        }
        CompoundTag cdTag = tag.getCompound("cooldowns");
        for (String key : cdTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int ticks = cdTag.getInt(key);
            if (id != null && ticks > 0) s.cooldowns.put(id, ticks);
        }
        return s;
    }

    // ---- networking ----

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(mana);
        buf.writeVarInt(learned.size());
        for (ResourceLocation id : learned) {
            buf.writeResourceLocation(id);
        }
        for (ResourceLocation slot : slots) {
            buf.writeBoolean(slot != null);
            if (slot != null) buf.writeResourceLocation(slot);
        }
        buf.writeVarInt(cooldowns.size());
        for (Map.Entry<ResourceLocation, Integer> e : cooldowns.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    public static PlayerSkills read(FriendlyByteBuf buf) {
        PlayerSkills s = new PlayerSkills();
        s.mana = buf.readDouble();
        int learnedCount = buf.readVarInt();
        for (int i = 0; i < learnedCount; i++) {
            s.learned.add(buf.readResourceLocation());
        }
        for (int i = 0; i < s.slots.length; i++) {
            if (buf.readBoolean()) s.slots[i] = buf.readResourceLocation();
        }
        int cdCount = buf.readVarInt();
        for (int i = 0; i < cdCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int rem = buf.readVarInt();
            if (rem > 0) s.cooldowns.put(id, rem);
        }
        return s;
    }
}
