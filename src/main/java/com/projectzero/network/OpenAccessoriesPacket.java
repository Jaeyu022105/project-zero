package com.projectzero.network;

import com.projectzero.equipment.EquipmentService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: open the player's 8-slot Accessory container menu. */
public record OpenAccessoriesPacket() {
    public static void encode(OpenAccessoriesPacket msg, FriendlyByteBuf buf) {}

    public static OpenAccessoriesPacket decode(FriendlyByteBuf buf) {
        return new OpenAccessoriesPacket();
    }

    public static void handle(OpenAccessoriesPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            EquipmentService.openMenu(player);
        }
        ctx.setPacketHandled(true);
    }
}
