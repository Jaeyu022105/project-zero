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
 * A player's mana, learned skills, skill presets (each 4 active slots; one preset is live), and
 * remaining skill cooldowns (NBT {@code projectzero:skills}). Cooldowns belong to skills, not
 * slots, so switching presets never resets a cooldown.
 */
public final class PlayerSkills {
    private double mana = -1.0; // -1 means uninitialized; filled to maxMana on first load
    private final LinkedHashSet<ResourceLocation> learned = new LinkedHashSet<>();
    private final ResourceLocation[][] presets = new ResourceLocation[SkillRules.PRESETS][SkillRules.ACTIVE_SLOTS];
    private int activePreset;
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

    public int activePreset() {
        return activePreset;
    }

    public void setActivePreset(int preset) {
        if (preset >= 0 && preset < presets.length) activePreset = preset;
    }

    private ResourceLocation[] slots() {
        return presets[activePreset];
    }

    public boolean addSkill(ResourceLocation id, boolean autoSlotActive) {
        boolean added = learned.add(id);
        if (added && autoSlotActive && slotOf(id) == -1) {
            ResourceLocation[] slots = slots();
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
            for (ResourceLocation[] slots : presets) {
                for (int i = 0; i < slots.length; i++) {
                    if (id.equals(slots[i])) slots[i] = null;
                }
            }
            cooldowns.remove(id);
        }
        return removed;
    }

    /** A slot of the live preset. */
    public @Nullable ResourceLocation slot(int index) {
        return slot(activePreset, index);
    }

    public @Nullable ResourceLocation slot(int preset, int index) {
        if (preset < 0 || preset >= presets.length || index < 0 || index >= SkillRules.ACTIVE_SLOTS) return null;
        return presets[preset][index];
    }

    public void setSlot(int index, @Nullable ResourceLocation skillId) {
        setSlot(activePreset, index, skillId);
    }

    /** Puts a skill in a preset slot; a skill appears at most once per preset (it moves). */
    public void setSlot(int preset, int index, @Nullable ResourceLocation skillId) {
        if (preset < 0 || preset >= presets.length || index < 0 || index >= SkillRules.ACTIVE_SLOTS) return;
        ResourceLocation[] slots = presets[preset];
        if (skillId != null) {
            for (int i = 0; i < slots.length; i++) {
                if (skillId.equals(slots[i])) slots[i] = null;
            }
        }
        slots[index] = skillId;
    }

    public int slotOf(ResourceLocation skillId) {
        ResourceLocation[] slots = slots();
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
        return !tickCooldownsReady().isEmpty();
    }

    /** Decrements all active cooldowns by 1 tick and returns the skill ids whose cooldown just hit 0. */
    public java.util.List<ResourceLocation> tickCooldownsReady() {
        if (cooldowns.isEmpty()) return java.util.List.of();
        java.util.List<ResourceLocation> ready = new java.util.ArrayList<>();
        var it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int next = e.getValue() - 1;
            if (next <= 0) {
                ready.add(e.getKey());
                it.remove();
            } else {
                e.setValue(next);
            }
        }
        return ready;
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

        ListTag presetList = new ListTag();
        for (ResourceLocation[] slots : presets) {
            CompoundTag slotTag = new CompoundTag();
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null) slotTag.putString("s" + i, slots[i].toString());
            }
            presetList.add(slotTag);
        }
        tag.put("presets", presetList);
        tag.putInt("activePreset", activePreset);

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
        // Older saves had one "slots" compound: it becomes preset 1.
        ListTag presetList = tag.getList("presets", Tag.TAG_COMPOUND);
        for (int p = 0; p < s.presets.length; p++) {
            CompoundTag slotTag = p < presetList.size() ? presetList.getCompound(p)
                    : (p == 0 && presetList.isEmpty() ? tag.getCompound("slots") : new CompoundTag());
            for (int i = 0; i < SkillRules.ACTIVE_SLOTS; i++) {
                String key = "s" + i;
                if (slotTag.contains(key)) {
                    ResourceLocation id = ResourceLocation.tryParse(slotTag.getString(key));
                    if (id != null && s.learned.contains(id)) s.presets[p][i] = id;
                }
            }
        }
        s.setActivePreset(tag.getInt("activePreset"));
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
        buf.writeVarInt(activePreset);
        for (ResourceLocation[] slots : presets) {
            for (ResourceLocation slot : slots) {
                buf.writeBoolean(slot != null);
                if (slot != null) buf.writeResourceLocation(slot);
            }
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
        s.activePreset = buf.readVarInt();
        for (ResourceLocation[] slots : s.presets) {
            for (int i = 0; i < slots.length; i++) {
                if (buf.readBoolean()) slots[i] = buf.readResourceLocation();
            }
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
