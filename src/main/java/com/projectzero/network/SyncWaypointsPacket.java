package com.projectzero.network;

import com.projectzero.client.ClientWaypointCache;
import com.projectzero.client.ClientWaypointScreenOpener;
import com.projectzero.waypoints.WaypointService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncWaypointsPacket(List<WaypointService.Entry> entries, boolean open, boolean renameRequest,
                                  ResourceLocation renameDimension, long renamePos, String renameName) {
    public SyncWaypointsPacket { entries = List.copyOf(entries); }

    public static SyncWaypointsPacket renameRequest(ResourceLocation dimension, long pos, String name) {
        return new SyncWaypointsPacket(List.of(), false, true, dimension, pos, name);
    }

    public static void encode(SyncWaypointsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.open());
        buf.writeBoolean(packet.renameRequest());
        buf.writeVarInt(packet.entries().size());
        for (WaypointService.Entry entry : packet.entries()) {
            buf.writeResourceLocation(entry.dimension());
            buf.writeLong(entry.pos());
            buf.writeUtf(entry.name(), 128);
            buf.writeBoolean(entry.valid());
            buf.writeBoolean(entry.owned());
            buf.writeDouble(entry.distance());
        }
        buf.writeBoolean(packet.renameDimension() != null);
        if (packet.renameDimension() != null) {
            buf.writeResourceLocation(packet.renameDimension());
            buf.writeLong(packet.renamePos());
            buf.writeUtf(packet.renameName() == null ? "" : packet.renameName(), 128);
        }
    }

    public static SyncWaypointsPacket decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean(), rename = buf.readBoolean();
        int count = Math.min(buf.readVarInt(), 512);
        List<WaypointService.Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new WaypointService.Entry(buf.readResourceLocation(), buf.readLong(),
                buf.readUtf(128), buf.readBoolean(), buf.readBoolean(), buf.readDouble()));
        ResourceLocation dimension = null;
        long pos = 0;
        String name = null;
        if (buf.readBoolean()) { dimension = buf.readResourceLocation(); pos = buf.readLong(); name = buf.readUtf(128); }
        return new SyncWaypointsPacket(entries, open, rename, dimension, pos, name);
    }

    public static void handle(SyncWaypointsPacket packet, CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientWaypointCache.receive(packet);
            if (packet.renameRequest()) ClientWaypointScreenOpener.rename(packet.renameDimension(), packet.renamePos(), packet.renameName());
            else if (packet.open()) ClientWaypointScreenOpener.list();
        });
        ctx.setPacketHandled(true);
    }
}
