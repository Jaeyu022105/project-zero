package com.projectzero.network;

import com.projectzero.waypoints.WaypointService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record WaypointTravelPacket(ResourceLocation dimension, long pos) {
    public static void encode(WaypointTravelPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension()); buf.writeLong(packet.pos());
    }
    public static WaypointTravelPacket decode(FriendlyByteBuf buf) {
        return new WaypointTravelPacket(buf.readResourceLocation(), buf.readLong());
    }
    public static void handle(WaypointTravelPacket packet, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() instanceof ServerPlayer player) WaypointService.travel(player, packet.dimension(), packet.pos());
        ctx.setPacketHandled(true);
    }
}
