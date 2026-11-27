package com.projectzero.waypoints;

import com.projectzero.ProjectZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;

/** Balance and policy values for waypoint stones. */
public final class WaypointRules {
    private WaypointRules() {}

    public static final int COOLDOWN_TICKS = 30 * 20;
    public static final double USE_DISTANCE = 8.0;
    public static final double TRAVEL_DISTANCE = 8.0;
    public static final int SAFE_SEARCH_RADIUS = 8;
    public static final int SAFE_SEARCH_HEIGHT = 4;
    public static final int LIGHT_LEVEL = 7;
    public static final int DEFAULT_NAME_LIMIT = 40;
    public static final TagKey<DimensionType> FORBIDDEN_DIMENSIONS = TagKey.create(
            Registries.DIMENSION_TYPE, ProjectZero.id("no_waypoint_stones"));
    public static final List<String> FORBIDDEN_DIMENSION_IDS = List.of("projectzero:dungeon", "projectzero:library");

    public static boolean isForbidden(net.minecraft.server.level.ServerLevel level) {
        return FORBIDDEN_DIMENSION_IDS.contains(level.dimension().location().toString())
                || level.dimensionTypeRegistration().is(FORBIDDEN_DIMENSIONS);
    }

    public static boolean isOverworldLike(net.minecraft.server.level.ServerLevel level) {
        return !isForbidden(level) && !level.dimensionType().hasCeiling();
    }
}
