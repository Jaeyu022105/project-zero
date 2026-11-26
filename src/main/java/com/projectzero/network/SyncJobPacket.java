package com.projectzero.network;

import com.projectzero.client.ClientJobCache;
import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server to client: the player's job. The rarity is sent along because job definitions
 * are server data the client does not have. {@code rarityOrdinal} is -1 for no job or
 * a job missing from the datapack.
 */
public record SyncJobPacket(PlayerJob job, int rarityOrdinal) {
    public static void encode(SyncJobPacket msg, FriendlyByteBuf buf) {
        msg.job.write(buf);
        buf.writeVarInt(msg.rarityOrdinal + 1);
    }

    public static SyncJobPacket decode(FriendlyByteBuf buf) {
        PlayerJob job = PlayerJob.read(buf);
        return new SyncJobPacket(job, buf.readVarInt() - 1);
    }

    public static void handle(SyncJobPacket msg, CustomPayloadEvent.Context ctx) {
        // ClientJobCache has no client-only imports, so this is safe to load on a server too.
        ClientJobCache.set(msg.job, JobRarity.byOrdinal(msg.rarityOrdinal));
        ctx.setPacketHandled(true);
    }
}
