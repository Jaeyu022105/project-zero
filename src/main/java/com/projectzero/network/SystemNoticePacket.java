package com.projectzero.network;

import com.projectzero.client.ClientNoticeCache;
import com.projectzero.ui.NoticeType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/** Server to client: pushes a bottom-right System notice and an optional {@code [System]} chat line. */
public record SystemNoticePacket(int typeOrdinal, Component title, Component body, @Nullable Component chatLine) {
    public static void encode(SystemNoticePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.typeOrdinal);
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, msg.title);
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, msg.body);
        buf.writeBoolean(msg.chatLine != null);
        if (msg.chatLine != null) {
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, msg.chatLine);
        }
    }

    public static SystemNoticePacket decode(FriendlyByteBuf buf) {
        int ord = buf.readVarInt();
        Component title = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf);
        Component body = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf);
        Component chat = buf.readBoolean() ? ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf) : null;
        return new SystemNoticePacket(ord, title, body, chat);
    }

    public static void handle(SystemNoticePacket msg, CustomPayloadEvent.Context ctx) {
        ClientNoticeCache.enqueue(NoticeType.byOrdinal(msg.typeOrdinal), msg.title, msg.body, msg.chatLine);
        ctx.setPacketHandled(true);
    }
}
