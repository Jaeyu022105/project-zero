package com.projectzero.equipment;

import com.projectzero.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu displaying the 8 active accessory slots (+ 1 locked Relic slot) above the player's inventory.
 */
public class AccessoryMenu extends AbstractContainerMenu {
    public static final int BACKPACK_SLOT = PlayerAccessories.TOTAL_SLOTS;
    public static final int EQUIPMENT_SLOTS = PlayerAccessories.TOTAL_SLOTS + 1;
    private final Container accessoryContainer;
    private final Container backpackContainer;

    /** Client constructor used by {@link net.minecraft.world.inventory.MenuType}. */
    public AccessoryMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(PlayerAccessories.TOTAL_SLOTS), new SimpleContainer(1));
    }

    /** Server factory backed directly by {@link EquipmentService}. */
    public static AccessoryMenu server(int containerId, Inventory playerInv, ServerPlayer player) {
        PlayerAccessories initial = EquipmentService.get(player);
        SimpleContainer backing = new SimpleContainer(PlayerAccessories.TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                PlayerAccessories updated = new PlayerAccessories();
                for (int i = 0; i < PlayerAccessories.TOTAL_SLOTS; i++) {
                    updated.set(i, getItem(i));
                }
                EquipmentService.set(player, updated);
            }
        };
        for (int i = 0; i < PlayerAccessories.TOTAL_SLOTS; i++) {
            backing.setItem(i, initial.get(i));
        }
        SimpleContainer backpack = new SimpleContainer(1) {
            private boolean loading = true;
            { super.setItem(0, BackpackService.get(player).item()); loading = false; }

            @Override public void setChanged() {
                super.setChanged();
                if (!loading) {
                    PlayerBackpack updated = new PlayerBackpack();
                    ItemStack stack = getItem(0);
                    if (stack.getItem() instanceof BackpackItem) updated.set(stack.copy());
                    BackpackService.set(player, updated);
                }
            }

            @Override public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof BackpackItem;
            }
        };
        return new AccessoryMenu(containerId, playerInv, backing, backpack);
    }

    private AccessoryMenu(int containerId, Inventory playerInv, Container accessoryContainer, Container backpackContainer) {
        super(ModMenus.ACCESSORIES.get(), containerId);
        this.accessoryContainer = accessoryContainer;
        this.backpackContainer = backpackContainer;

        // 8 active slots in 2 rows of 4 (x = 26 + col*26, y = 22 + row*24), plus locked Relic slot at index 8
        for (int i = 0; i < PlayerAccessories.ACTIVE_SLOTS; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = 26 + col * 26;
            int y = 22 + row * 24;
            AccessorySlotType expected = PlayerAccessories.SLOT_LAYOUT[i];
            addSlot(new Slot(accessoryContainer, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof AccessoryItem acc && acc.slotType() == expected;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Slot 8: spare Relic slot (disabled for now)
        addSlot(new Slot(accessoryContainer, 8, 138, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new Slot(backpackContainer, 0, 138, 58) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BackpackItem; }
            @Override public boolean mayPickup(Player player) {
                ItemStack pack = getItem();
                var contents = pack.get(net.minecraft.core.component.DataComponents.CONTAINER);
                return contents == null || contents.nonEmptyStream().findAny().isEmpty();
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // Player inventory follows all accessory and backpack equipment slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar starting at y = 142
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < EQUIPMENT_SLOTS) {
            // Accessory slot -> player inventory
            if (!moveItemStackTo(stack, EQUIPMENT_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> matching active accessory slot
            if (stack.getItem() instanceof BackpackItem && !slots.get(BACKPACK_SLOT).hasItem()) {
                if (!moveItemStackTo(stack, BACKPACK_SLOT, BACKPACK_SLOT + 1, false)) return ItemStack.EMPTY;
            } else {
            if (!(stack.getItem() instanceof AccessoryItem accItem)
                    || accItem.slotType() == AccessorySlotType.RELIC) {
                return ItemStack.EMPTY;
            }
            boolean moved = false;
            for (int i = 0; i < PlayerAccessories.ACTIVE_SLOTS; i++) {
                if (PlayerAccessories.SLOT_LAYOUT[i] == accItem.slotType() && !slots.get(i).hasItem()) {
                    if (moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
