package com.projectzero.network;

import com.projectzero.companions.CompanionService;
import com.projectzero.leveling.Stat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/** Client request to allocate one point on an owned companion. */
public record SpendCompanionPointPacket(UUID companionId, int statOrdinal) {
    public static void encode(SpendCompanionPointPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.companionId);
        buf.writeVarInt(msg.statOrdinal);
    }

    public static SpendCompanionPointPacket decode(FriendlyByteBuf buf) {
        return new SpendCompanionPointPacket(buf.readUUID(), buf.readVarInt());
    }

    public static void handle(SpendCompanionPointPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        Stat stat = Stat.byOrdinal(msg.statOrdinal);
        if (player != null && stat != null) CompanionService.spendPoint(player, msg.companionId, stat);
        ctx.setPacketHandled(true);
    }
}
