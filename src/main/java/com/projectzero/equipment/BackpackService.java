package com.projectzero.equipment;

import com.projectzero.ProjectZero;
import com.projectzero.client.ClientSkillVisuals;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SkillVisualPacket;
import com.projectzero.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

/** Server-owned persistence and menu access for the equipped backpack and its item data. */
public final class BackpackService {
    private BackpackService() {}

    public static final String DATA_KEY = ProjectZero.MOD_ID + ":backpack";

    public static PlayerBackpack get(Player player) {
        return PlayerBackpack.load(player.getPersistentData().getCompound(DATA_KEY), player.registryAccess());
    }

    public static void set(ServerPlayer player, PlayerBackpack backpack) {
        player.getPersistentData().put(DATA_KEY, backpack.save(player.registryAccess()));
    }

    public static void copy(Player original, Player clone) {
        clone.getPersistentData().put(DATA_KEY,
                original.getPersistentData().getCompound(DATA_KEY).copy());
    }

    public static boolean hasBackpack(Player player) {
        return get(player).item().getItem() instanceof BackpackItem;
    }

    public static void updateContents(ServerPlayer player, List<ItemStack> items) {
        PlayerBackpack worn = get(player);
        ItemStack pack = worn.item();
        if (!(pack.getItem() instanceof BackpackItem backpack)) return;
        NonNullList<ItemStack> contents = NonNullList.withSize(backpack.slots(), ItemStack.EMPTY);
        for (int i = 0; i < Math.min(contents.size(), items.size()); i++) {
            ItemStack stack = items.get(i);
            contents.set(i, stack.getItem() instanceof BackpackItem ? ItemStack.EMPTY : stack.copy());
        }
        pack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        set(player, worn);
    }

    public static void open(ServerPlayer player) {
        ItemStack pack = get(player).item();
        if (!(pack.getItem() instanceof BackpackItem backpack)) {
            player.displayClientMessage(Component.translatable("notice.projectzero.backpack.required"), true);
            return;
        }
        ModNetwork.sendToTrackingAndSelf(player, new SkillVisualPacket(player.getId(),
                ClientSkillVisuals.Kind.ANIM.ordinal(), com.projectzero.anim.PlayerAnim.BACKPACK_OPEN.ordinal(),
                com.projectzero.anim.PlayerAnim.BACKPACK_OPEN.ticks));
        player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                BackpackMenu.server(id, inventory, player, backpack.slots()),
                Component.translatable("screen.projectzero.backpack")),
                buf -> buf.writeVarInt(backpack.slots()));
    }

    public static void dropOnDeath(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        PlayerBackpack worn = get(player);
        if (worn.isEmpty()) return;
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), worn.item().copy());
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
        set(player, new PlayerBackpack());
    }

}
