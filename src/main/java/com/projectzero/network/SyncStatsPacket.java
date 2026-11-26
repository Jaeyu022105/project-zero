package com.projectzero.network;

import com.projectzero.client.ClientStatsCache;
import com.projectzero.leveling.PlayerStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server to client: the player's full leveling data. */
public record SyncStatsPacket(PlayerStats stats) {
    public static void encode(SyncStatsPacket msg, FriendlyByteBuf buf) {
        msg.stats.write(buf);
    }

    public static SyncStatsPacket decode(FriendlyByteBuf buf) {
        return new SyncStatsPacket(PlayerStats.read(buf));
    }

    public static void handle(SyncStatsPacket msg, CustomPayloadEvent.Context ctx) {
        // ClientStatsCache has no client-only imports, so this is safe to load on a server too.
        ClientStatsCache.set(msg.stats);
        ctx.setPacketHandled(true);
    }
}
