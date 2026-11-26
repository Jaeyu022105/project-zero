package com.projectzero.network;

import com.projectzero.client.ClientTitlesCache;
import com.projectzero.titles.PlayerTitles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server to client: unlocked titles and currently equipped title. */
public record SyncTitlesPacket(PlayerTitles titles) {
    public static void encode(SyncTitlesPacket msg, FriendlyByteBuf buf) {
        msg.titles.write(buf);
    }

    public static SyncTitlesPacket decode(FriendlyByteBuf buf) {
        return new SyncTitlesPacket(PlayerTitles.read(buf));
    }

    public static void handle(SyncTitlesPacket msg, CustomPayloadEvent.Context ctx) {
        ClientTitlesCache.set(msg.titles);
        ctx.setPacketHandled(true);
    }
}
