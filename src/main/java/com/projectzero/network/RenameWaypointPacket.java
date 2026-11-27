package com.projectzero.network;

import com.projectzero.waypoints.WaypointService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record RenameWaypointPacket(ResourceLocation dimension, long pos, String name) {
    public static void encode(RenameWaypointPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension()); buf.writeLong(packet.pos()); buf.writeUtf(packet.name(), 128);
    }
    public static RenameWaypointPacket decode(FriendlyByteBuf buf) {
        return new RenameWaypointPacket(buf.readResourceLocation(), buf.readLong(), buf.readUtf(128));
    }
    public static void handle(RenameWaypointPacket packet, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() instanceof ServerPlayer player) WaypointService.rename(player, packet.dimension(), packet.pos(), packet.name());
        ctx.setPacketHandled(true);
    }
}
