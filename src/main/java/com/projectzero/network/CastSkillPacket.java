package com.projectzero.network;

import com.projectzero.skills.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: request casting the active skill in {@code slot} (0..3). Validated on server. */
public record CastSkillPacket(int slot) {
    public static void encode(CastSkillPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
    }

    public static CastSkillPacket decode(FriendlyByteBuf buf) {
        return new CastSkillPacket(buf.readVarInt());
    }

    public static void handle(CastSkillPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            SkillService.castSlot(player, msg.slot);
        }
        ctx.setPacketHandled(true);
    }
}
