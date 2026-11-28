package com.projectzero.equipment;

import com.projectzero.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** A server-backed backpack inventory followed by the player's own inventory. */
public final class BackpackMenu extends AbstractContainerMenu {
    private static final int MAX_SLOTS = 27;
    private final int capacity;
    private final Player owner;
    private final BackpackContents contents;

    /** Client constructor; capacity is sent by the server when opening. */
    public BackpackMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, null, data.readVarInt(), new BackpackContents(null));
    }

    public static BackpackMenu server(int containerId, Inventory inventory, ServerPlayer player, int capacity) {
        ItemStack pack = BackpackService.get(player).item();
        BackpackContents contents = new BackpackContents(player);
        NonNullList<ItemStack> initial = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        ItemContainerContents stored = pack.get(DataComponents.CONTAINER);
        if (stored != null) stored.copyInto(initial);
        contents.load(initial);
        return new BackpackMenu(containerId, inventory, player, capacity, contents);
    }

    private BackpackMenu(int containerId, Inventory inventory, Player owner, int capacity, BackpackContents contents) {
        super(ModMenus.BACKPACK.get(), containerId);
        this.owner = owner;
        this.capacity = Math.max(0, Math.min(MAX_SLOTS, capacity));
        this.contents = contents;
        for (int i = 0; i < MAX_SLOTS; i++) {
            final int slotIndex = i;
            addSlot(new Slot(contents, i, 8 + (i % 9) * 18, 24 + (i / 9) * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return slotIndex < BackpackMenu.this.capacity && !(stack.getItem() instanceof BackpackItem);
                }
                @Override public boolean mayPickup(Player player) { return slotIndex < BackpackMenu.this.capacity; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 154));
    }

    public int capacity() { return capacity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MAX_SLOTS) {
            if (!moveItemStackTo(stack, MAX_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof BackpackItem
                    || !moveItemStackTo(stack, 0, capacity, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (owner instanceof ServerPlayer serverPlayer) BackpackService.updateContents(serverPlayer, contents.getItems());
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && (owner == null || owner == player && BackpackService.hasBackpack(player));
    }

    private static final class BackpackContents extends SimpleContainer {
        private final ServerPlayer player;
        private boolean loading = true;

        private BackpackContents(ServerPlayer player) {
            super(MAX_SLOTS);
            this.player = player;
        }

        private void load(NonNullList<ItemStack> initial) {
            for (int i = 0; i < MAX_SLOTS; i++) super.setItem(i, initial.get(i));
            loading = false;
        }

        @Override public void setChanged() {
            super.setChanged();
            if (!loading && player != null) BackpackService.updateContents(player, getItems());
        }

        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot >= 0 && slot < MAX_SLOTS && !(stack.getItem() instanceof BackpackItem);
        }
    }
}
