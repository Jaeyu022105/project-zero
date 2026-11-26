package com.projectzero.equipment;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Holds a player's equipped accessories in persistent NBT under {@code projectzero:accessories}.
 * Slots 0..7 are active; slot 8 is the spare disabled Relic slot (DESIGN-DOCUMENT.md §12).
 */
public final class PlayerAccessories {
    public static final AccessorySlotType[] SLOT_LAYOUT = {
            AccessorySlotType.GAUNTLETS, // 0
            AccessorySlotType.RING,      // 1
            AccessorySlotType.RING,      // 2
            AccessorySlotType.AMULET,    // 3
            AccessorySlotType.EARRINGS,  // 4
            AccessorySlotType.BELT,      // 5
            AccessorySlotType.CHARM,     // 6
            AccessorySlotType.CHARM,     // 7
            AccessorySlotType.RELIC      // 8 (disabled for now)
    };

    public static final int ACTIVE_SLOTS = 8;
    public static final int TOTAL_SLOTS = SLOT_LAYOUT.length;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public ItemStack get(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) return ItemStack.EMPTY;
        return stacks.get(index);
    }

    public void set(int index, ItemStack stack) {
        if (index < 0 || index >= TOTAL_SLOTS) return;
        stacks.set(index, stack == null ? ItemStack.EMPTY : stack);
    }

    public boolean isEmpty() {
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack s = stacks.get(i);
            if (!s.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Stack", s.save(registries));
                list.add(entry);
            }
        }
        root.put("Items", list);
        return root;
    }

    public static PlayerAccessories load(CompoundTag root, HolderLookup.Provider registries) {
        PlayerAccessories acc = new PlayerAccessories();
        ListTag list = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < TOTAL_SLOTS && entry.contains("Stack", Tag.TAG_COMPOUND)) {
                acc.set(slot, ItemStack.parseOptional(registries, entry.getCompound("Stack")));
            }
        }
        return acc;
    }
}
