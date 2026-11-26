package com.projectzero.network;

import com.projectzero.titles.TitleService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: cycle the equipped title from the Status window. */
public record CycleTitlePacket() {
    public static void encode(CycleTitlePacket msg, FriendlyByteBuf buf) {}

    public static CycleTitlePacket decode(FriendlyByteBuf buf) {
        return new CycleTitlePacket();
    }

    public static void handle(CycleTitlePacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            TitleService.cycleEquipped(player);
        }
        ctx.setPacketHandled(true);
    }
}
