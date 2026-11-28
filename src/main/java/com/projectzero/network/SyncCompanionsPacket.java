package com.projectzero.network;

import com.projectzero.client.ClientCompanionsCache;
import com.projectzero.companions.PlayerCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record SyncCompanionsPacket(PlayerCompanions companions) {
    public static void encode(SyncCompanionsPacket msg, FriendlyByteBuf buf) {
        msg.companions.write(buf);
    }

    public static SyncCompanionsPacket decode(FriendlyByteBuf buf) {
        return new SyncCompanionsPacket(PlayerCompanions.read(buf));
    }

    public static void handle(SyncCompanionsPacket msg, CustomPayloadEvent.Context ctx) {
        ClientCompanionsCache.set(msg.companions);
        ctx.setPacketHandled(true);
    }
}
