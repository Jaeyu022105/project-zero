package com.projectzero.equipment;

import com.projectzero.ProjectZero;
import com.projectzero.leveling.LevelingService;
import com.projectzero.leveling.Stat;
import com.projectzero.leveling.StatEffects;
import com.projectzero.skills.SkillService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side API for player accessories (DESIGN-DOCUMENT.md §12).
 */
public final class EquipmentService {
    private EquipmentService() {}

    public static final String DATA_KEY = ProjectZero.MOD_ID + ":accessories";

    public static PlayerAccessories get(Player player) {
        return PlayerAccessories.load(
                player.getPersistentData().getCompound(DATA_KEY),
                player.registryAccess());
    }

    public static void set(ServerPlayer player, PlayerAccessories accessories) {
        player.getPersistentData().put(DATA_KEY, accessories.save(player.registryAccess()));
        refreshEffects(player);
    }

    public static void refreshEffects(ServerPlayer player) {
        StatEffects.apply(player, LevelingService.get(player));
        SkillService.sync(player);
    }

    public static void clearAll(ServerPlayer player) {
        set(player, new PlayerAccessories());
    }

    public static void openMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> AccessoryMenu.server(containerId, playerInv, player),
                Component.translatable("screen.projectzero.accessories")));
    }

    /** Equips {@code stack} into the first matching empty active slot (or swaps slot 0 of that type). Returns old stack, or null if incompatible. */
    public static @Nullable ItemStack quickEquip(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof AccessoryItem accItem)) return null;
        AccessorySlotType type = accItem.slotType();
        if (type == AccessorySlotType.RELIC) return null;

        PlayerAccessories acc = get(player);
        int firstMatch = -1;
        for (int i = 0; i < PlayerAccessories.ACTIVE_SLOTS; i++) {
            if (PlayerAccessories.SLOT_LAYOUT[i] == type) {
                if (firstMatch < 0) firstMatch = i;
                if (acc.get(i).isEmpty()) {
                    acc.set(i, stack.copyWithCount(1));
                    set(player, acc);
                    return ItemStack.EMPTY;
                }
            }
        }
        if (firstMatch >= 0) {
            ItemStack prev = acc.get(firstMatch);
            acc.set(firstMatch, stack.copyWithCount(1));
            set(player, acc);
            return prev;
        }
        return null;
    }

    /** Sum of flat stat points across all equipped active accessories. */
    public static int statBonus(Player player, Stat stat) {
        PlayerAccessories acc = get(player);
        int total = 0;
        for (int i = 0; i < PlayerAccessories.ACTIVE_SLOTS; i++) {
            ItemStack s = acc.get(i);
            if (s.isEmpty()) continue;
            AccessoryData data = AccessoryData.get(s);
            if (data != null) {
                total += data.stats().getOrDefault(stat, 0);
            }
        }
        return total;
    }

    /** Sum of a special affix value across all equipped active accessories. */
    public static double affixTotal(Player player, AccessoryData.AffixType type) {
        PlayerAccessories acc = get(player);
        double sum = 0.0;
        for (int i = 0; i < PlayerAccessories.ACTIVE_SLOTS; i++) {
            ItemStack s = acc.get(i);
            if (s.isEmpty()) continue;
            AccessoryData data = AccessoryData.get(s);
            if (data != null && data.special().isPresent() && data.special().get().type() == type) {
                sum += data.special().get().value();
            }
        }
        return sum;
    }

    public static double xpBonusMultiplier(ServerPlayer player) {
        return affixTotal(player, AccessoryData.AffixType.XP_BONUS);
    }

    public static double manaRegenBonus(ServerPlayer player) {
        return affixTotal(player, AccessoryData.AffixType.MANA_REGEN);
    }

    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
