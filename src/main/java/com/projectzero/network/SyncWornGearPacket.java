package com.projectzero.network;

import com.projectzero.client.ClientWornGear;
import com.projectzero.jobs.JobRarity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Server to client: the gauntlets a player wears, for the worn-gear render layer.
 * Sent to the player and everyone tracking them. {@code itemId} null means bare hands.
 */
public record SyncWornGearPacket(int entityId, @Nullable ResourceLocation itemId, int rarityOrdinal) {
    public static void encode(SyncWornGearPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.itemId != null);
        if (msg.itemId != null) buf.writeResourceLocation(msg.itemId);
        buf.writeVarInt(msg.rarityOrdinal + 1);
    }

    public static SyncWornGearPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        ResourceLocation item = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new SyncWornGearPacket(id, item, buf.readVarInt() - 1);
    }

    public static void handle(SyncWornGearPacket msg, CustomPayloadEvent.Context ctx) {
        ClientWornGear.setGauntlets(msg.entityId, msg.itemId == null ? null
                : new ClientWornGear.Worn(msg.itemId, JobRarity.byOrdinal(msg.rarityOrdinal)));
        ctx.setPacketHandled(true);
    }
}
