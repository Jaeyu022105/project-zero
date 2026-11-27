package com.projectzero.waypoints;

import com.projectzero.ProjectZero;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.Set;

/** Player-owned list of known waypoints, persisted on player NBT. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID)
public final class PlayerWaypoints {
    private PlayerWaypoints() {}
    private static final String ROOT = ProjectZero.MOD_ID + ":waypoints";
    private static final String KEY = "Known";

    public record Key(ResourceLocation dimension, long pos) {
        public String serialized() { return dimension + "|" + pos; }
        public static Key parse(String value) {
            int split = value.lastIndexOf('|');
            if (split < 1) return null;
            try { return new Key(ResourceLocation.parse(value.substring(0, split)), Long.parseLong(value.substring(split + 1))); }
            catch (RuntimeException ignored) { return null; }
        }
    }

    public static Set<Key> get(Player player) {
        Set<Key> result = new LinkedHashSet<>();
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        ListTag list = root.getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Key key = Key.parse(list.getString(i));
            if (key != null) result.add(key);
        }
        return result;
    }

    public static boolean knows(Player player, ResourceLocation dimension, long pos) {
        return get(player).contains(new Key(dimension, pos));
    }

    public static void add(Player player, ResourceLocation dimension, long pos) {
        Set<Key> keys = get(player);
        keys.add(new Key(dimension, pos));
        write(player, keys);
    }

    public static void remove(Player player, Key key) {
        Set<Key> keys = get(player);
        if (keys.remove(key)) write(player, keys);
    }

    private static void write(Player player, Set<Key> keys) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Key key : keys) list.add(net.minecraft.nbt.StringTag.valueOf(key.serialized()));
        root.put(KEY, list);
        player.getPersistentData().put(ROOT, root);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        copy(event.getOriginal(), event.getEntity());
    }

    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(ROOT).copy();
        if (!data.isEmpty()) to.getPersistentData().put(ROOT, data);
    }
}
