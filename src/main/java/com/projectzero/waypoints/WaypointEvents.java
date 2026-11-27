package com.projectzero.waypoints;

import com.projectzero.ProjectZero;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID)
public final class WaypointEvents {
    private WaypointEvents() {}

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !event.getPlacedBlock().is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        WaypointService.placed(level, event.getPos(), player);
    }

    @SubscribeEvent
    public static void onBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !event.getState().is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())) return;
        WaypointSavedData.get(level).remove(event.getPos());
    }

    @SubscribeEvent
    public static void onExploded(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (level.getBlockState(pos).is(com.projectzero.registry.ModBlocks.WAYPOINT_STONE.get())) {
                WaypointSavedData.get(level).remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WaypointService.syncOnJoin(player);
    }
}
