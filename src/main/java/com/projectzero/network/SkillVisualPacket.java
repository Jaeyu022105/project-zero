package com.projectzero.network;

import com.projectzero.client.ClientSkillVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server to client: show a skill visual on an entity for {@code ticks} (shield emblem, aura bubble).
 * Sent to the caster and everyone tracking them. See {@link ClientSkillVisuals.Kind}.
 */
public record SkillVisualPacket(int entityId, int kind, int rgb, int ticks) {
    public static void encode(SkillVisualPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.kind);
        buf.writeInt(msg.rgb);
        buf.writeVarInt(msg.ticks);
    }

    public static SkillVisualPacket decode(FriendlyByteBuf buf) {
        return new SkillVisualPacket(buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readVarInt());
    }

    public static void handle(SkillVisualPacket msg, CustomPayloadEvent.Context ctx) {
        ClientSkillVisuals.add(msg.entityId, msg.kind, msg.rgb, msg.ticks);
        ctx.setPacketHandled(true);
    }
}
