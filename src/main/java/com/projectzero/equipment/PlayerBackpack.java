package com.projectzero.equipment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Player's dedicated backpack equipment slot, independent of the backpack item's contents. */
public final class PlayerBackpack {
    private ItemStack item = ItemStack.EMPTY;

    public ItemStack item() { return item; }
    public void set(ItemStack value) { item = value == null ? ItemStack.EMPTY : value; }
    public boolean isEmpty() { return item.isEmpty(); }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!item.isEmpty()) tag.put("Item", item.save(registries));
        return tag;
    }

    public static PlayerBackpack load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerBackpack backpack = new PlayerBackpack();
        if (tag.contains("Item", CompoundTag.TAG_COMPOUND)) {
            backpack.set(ItemStack.parseOptional(registries, tag.getCompound("Item")));
        }
        if (!(backpack.item.getItem() instanceof BackpackItem)) backpack.set(ItemStack.EMPTY);
        return backpack;
    }
}
