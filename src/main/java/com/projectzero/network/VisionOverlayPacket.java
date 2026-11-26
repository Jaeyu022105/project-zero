package com.projectzero.network;

import com.projectzero.client.VisionOverlayState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server to client: darken the screen to {@code coverage} (0 = clear, 1 = black) over
 * {@code ticks}. With {@code eyelids} the darkness closes from the top and bottom like eyelids;
 * without, the whole screen fades. Used by the Reawakening vision.
 */
public record VisionOverlayPacket(float coverage, int ticks, boolean eyelids) {
    public static void encode(VisionOverlayPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.coverage);
        buf.writeVarInt(msg.ticks);
        buf.writeBoolean(msg.eyelids);
    }

    public static VisionOverlayPacket decode(FriendlyByteBuf buf) {
        return new VisionOverlayPacket(buf.readFloat(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(VisionOverlayPacket msg, CustomPayloadEvent.Context ctx) {
        // VisionOverlayState has no client-only imports, so this is safe to load on a server too.
        VisionOverlayState.set(msg.coverage, msg.ticks, msg.eyelids);
        ctx.setPacketHandled(true);
    }
}
