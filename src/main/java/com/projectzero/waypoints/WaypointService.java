package com.projectzero.waypoints;

import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncWaypointsPacket;
import com.projectzero.network.WaypointTravelPacket;
import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Server-authoritative waypoint registration, attunement, listing, and travel. */
public final class WaypointService {
    private WaypointService() {}

    public record Entry(ResourceLocation dimension, long pos, String name, boolean valid, boolean owned, double distance) {}

    public static void placed(ServerLevel level, BlockPos pos, ServerPlayer owner) {
        WaypointSavedData data = WaypointSavedData.get(level);
        String biome = level.getBiome(pos).unwrapKey()
                .map(key -> Component.translatable(Util.makeDescriptionId("biome", key.location())).getString())
                .orElseGet(() -> Component.translatable("biome.minecraft.plains").getString());
        data.add(pos, data.nextName(biome), owner.getUUID());
    }

    public static void applyPlacedCustomName(ServerLevel level, BlockPos pos, ServerPlayer owner, String name) {
        WaypointSavedData data = WaypointSavedData.get(level);
        WaypointSavedData.Waypoint waypoint = data.get(pos);
        if (waypoint != null && waypoint.owner().equals(owner.getUUID()) && name != null && !name.isBlank()) {
            data.rename(pos, name.strip());
        }
    }

    public static void onUse(ServerPlayer player, ServerLevel level, BlockPos pos, boolean rename) {
        WaypointSavedData data = WaypointSavedData.get(level);
        WaypointSavedData.Waypoint waypoint = data.get(pos);
        if (waypoint == null) {
            placed(level, pos, player);
            waypoint = data.get(pos);
        }
        if (rename) {
            if (!waypoint.owner().equals(player.getUUID())) {
                SystemNotices.system(player, NoticeType.WARNING, Component.translatable("notice.projectzero.waypoint.owner_only"));
                return;
            }
            ModNetwork.sendToPlayer(player, SyncWaypointsPacket.renameRequest(level.dimension().location(), pos.asLong(), waypoint.name()));
            return;
        }
        ResourceLocation dimension = level.dimension().location();
        if (!PlayerWaypoints.knows(player, dimension, pos.asLong())) {
            PlayerWaypoints.add(player, dimension, pos.asLong());
            SystemNotices.system(player, NoticeType.ITEM, Component.translatable("notice.projectzero.waypoint.attuned", waypoint.name()));
            sync(player, false);
        } else {
            sendList(player);
        }
    }

    public static void sendList(ServerPlayer player) { sync(player, true); }

    private static void sync(ServerPlayer player, boolean open) {
        List<Entry> entries = new ArrayList<>();
        var keys = PlayerWaypoints.get(player);
        for (PlayerWaypoints.Key key : keys) {
            ServerLevel level = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, key.dimension()));
            WaypointSavedData.Waypoint waypoint = level == null ? null : WaypointSavedData.get(level).get(BlockPos.of(key.pos()));
            boolean valid = level != null && waypoint != null && level.getBlockState(BlockPos.of(key.pos())).is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get());
            String name = waypoint == null ? Component.translatable("screen.projectzero.waypoint.unknown").getString() : waypoint.name();
            boolean owned = waypoint != null && waypoint.owner().equals(player.getUUID());
            double distance = level == player.serverLevel() ? player.blockPosition().distSqr(BlockPos.of(key.pos())) : -1;
            entries.add(new Entry(key.dimension(), key.pos(), name, valid, owned, distance));
            if (!valid) PlayerWaypoints.remove(player, key);
        }
        entries.sort(Comparator.comparing(Entry::name).thenComparing(e -> e.dimension().toString()).thenComparingLong(Entry::pos));
        ModNetwork.sendToPlayer(player, new SyncWaypointsPacket(entries, open, false, null, 0L, null));
    }

    public static void travel(ServerPlayer player, ResourceLocation dimension, long packedPos) {
        BlockPos pos = BlockPos.of(packedPos);
        PlayerWaypoints.Key key = new PlayerWaypoints.Key(dimension, packedPos);
        if (!PlayerWaypoints.knows(player, dimension, packedPos)) return;
        ServerLevel source = player.serverLevel();
        ServerLevel target = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (target == null || !WaypointRules.isOverworldLike(source) || !WaypointRules.isOverworldLike(target)) return;
        WaypointSavedData data = WaypointSavedData.get(target);
        WaypointSavedData.Waypoint waypoint = data.get(pos);
        if (waypoint == null || !target.getBlockState(pos).is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())) {
            PlayerWaypoints.remove(player, key);
            sync(player, true);
            return;
        }
        if (!nearKnownWaypoint(player, source)) return;
        long now = player.getServer().overworld().getGameTime();
        if (player.getPersistentData().getLong("projectzero:waypoint_cooldown_until") > now) {
            SystemNotices.system(player, NoticeType.WARNING, Component.translatable("notice.projectzero.waypoint.cooldown"));
            return;
        }
        BlockPos safe = safeSpot(target, pos);
        if (safe == null) {
            SystemNotices.system(player, NoticeType.WARNING, Component.translatable("notice.projectzero.waypoint.no_safe_spot"));
            return;
        }
        Vec3 from = player.position();
        source.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 24, .45, .7, .45, .15);
        source.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .8F, .7F);
        player.teleportTo(target, safe.getX() + .5, safe.getY(), safe.getZ() + .5, player.getYRot(), player.getXRot());
        player.getPersistentData().putLong("projectzero:waypoint_cooldown_until", now + WaypointRules.COOLDOWN_TICKS);
        target.sendParticles(ParticleTypes.PORTAL, safe.getX() + .5, safe.getY() + 1, safe.getZ() + .5, 24, .45, .7, .45, .15);
        target.playSound(null, safe, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .8F, 1.2F);
    }

    private static boolean nearKnownWaypoint(ServerPlayer player, ServerLevel source) {
        ResourceLocation dimension = source.dimension().location();
        for (PlayerWaypoints.Key known : PlayerWaypoints.get(player)) {
            if (!known.dimension().equals(dimension)) continue;
            BlockPos pos = BlockPos.of(known.pos());
            if (player.blockPosition().distSqr(pos) > WaypointRules.USE_DISTANCE * WaypointRules.USE_DISTANCE) continue;
            if (source.getBlockState(pos).is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())
                    && WaypointSavedData.get(source).get(pos) != null) return true;
        }
        return false;
    }

    private static @Nullable BlockPos safeSpot(ServerLevel level, BlockPos stone) {
        for (int dy = 0; dy <= WaypointRules.SAFE_SEARCH_HEIGHT; dy++) {
            for (int radius = 0; radius <= WaypointRules.SAFE_SEARCH_RADIUS; radius++) {
                for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    BlockPos feet = stone.offset(x, dy, z);
                    BlockPos head = feet.above();
                    BlockState floor = level.getBlockState(feet.below());
                    if (floor.isSolidRender(level, feet.below()) && level.getBlockState(feet).isAir()
                            && level.getBlockState(head).isAir() && !level.getFluidState(feet).isSource()) return feet;
                }
            }
        }
        return null;
    }

    public static void rename(ServerPlayer player, ResourceLocation dimension, long packedPos, String name) {
        if (name == null) return;
        String clean = name.strip();
        if (clean.isEmpty() || clean.length() > WaypointRules.DEFAULT_NAME_LIMIT || clean.codePoints().anyMatch(Character::isISOControl)) return;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
        ServerLevel level = player.getServer().getLevel(key);
        if (level == null || player.serverLevel() != level || !WaypointRules.isOverworldLike(level)) return;
        BlockPos pos = BlockPos.of(packedPos);
        if (player.blockPosition().distSqr(pos) > WaypointRules.USE_DISTANCE * WaypointRules.USE_DISTANCE) return;
        WaypointSavedData.Waypoint waypoint = WaypointSavedData.get(level).get(pos);
        if (waypoint == null || !waypoint.owner().equals(player.getUUID())
                || !level.getBlockState(pos).is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())) return;
        WaypointSavedData.get(level).rename(pos, clean);
        sync(player, true);
    }

    public static void syncOnJoin(ServerPlayer player) { sync(player, false); }
}
