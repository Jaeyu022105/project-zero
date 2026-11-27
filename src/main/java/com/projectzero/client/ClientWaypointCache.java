package com.projectzero.client;

import com.projectzero.network.ModNetwork;
import com.projectzero.network.RenameWaypointPacket;
import com.projectzero.network.SyncWaypointsPacket;
import com.projectzero.waypoints.WaypointService;

import java.util.List;

/** Synchronized waypoint view model, intentionally free of client-only imports. */
public final class ClientWaypointCache {
    private ClientWaypointCache() {}
    private static List<WaypointService.Entry> entries = List.of();

    public static List<WaypointService.Entry> entries() { return entries; }

    public static void receive(SyncWaypointsPacket packet) {
        entries = packet.entries();
    }
}
