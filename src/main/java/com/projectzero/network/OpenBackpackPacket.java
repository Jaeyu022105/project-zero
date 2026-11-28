package com.projectzero.network;

import com.projectzero.equipment.BackpackService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client request to open the currently equipped backpack; the server selects the actual item and size. */
public record OpenBackpackPacket() {
    public static void encode(OpenBackpackPacket msg, FriendlyByteBuf buf) {}
    public static OpenBackpackPacket decode(FriendlyByteBuf buf) { return new OpenBackpackPacket(); }
    public static void handle(OpenBackpackPacket msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() instanceof ServerPlayer player) BackpackService.open(player);
        ctx.setPacketHandled(true);
    }
}
