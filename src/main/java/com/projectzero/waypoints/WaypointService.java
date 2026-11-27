package com.projectzero.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;

/** Server-side placement and registration for waypoint stones. */
public final class WaypointService {
    private WaypointService() {}

    public static void placed(ServerLevel level, BlockPos pos, ServerPlayer owner) {
        String biome = level.getBiome(pos).unwrapKey()
                .map(key -> Component.translatable(Util.makeDescriptionId("biome", key.location())).getString())
                .orElseGet(() -> Component.translatable("biome.minecraft.plains").getString());
        WaypointSavedData data = WaypointSavedData.get(level);
        data.add(pos, data.nextName(biome), owner.getUUID());
    }

    public static void applyPlacedCustomName(ServerLevel level, BlockPos pos, ServerPlayer owner, String name) {
        WaypointSavedData data = WaypointSavedData.get(level);
        WaypointSavedData.Waypoint waypoint = data.get(pos);
        if (waypoint != null && waypoint.owner().equals(owner.getUUID()) && name != null && !name.isBlank()) {
            data.rename(pos, name.strip());
        }
    }
}
