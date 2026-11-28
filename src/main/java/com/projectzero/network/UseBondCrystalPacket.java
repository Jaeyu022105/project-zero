package com.projectzero.network;

import com.projectzero.companions.CompanionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/** Client request to consume a Bond Crystal on an owned companion. */
public record UseBondCrystalPacket(UUID companionId) {
    public static void encode(UseBondCrystalPacket msg, FriendlyByteBuf buf) { buf.writeUUID(msg.companionId); }
    public static UseBondCrystalPacket decode(FriendlyByteBuf buf) { return new UseBondCrystalPacket(buf.readUUID()); }

    public static void handle(UseBondCrystalPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) CompanionService.useBondCrystal(player, msg.companionId);
        ctx.setPacketHandled(true);
    }
}
