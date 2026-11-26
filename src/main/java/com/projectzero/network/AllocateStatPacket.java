package com.projectzero.network;

import com.projectzero.leveling.LevelingService;
import com.projectzero.leveling.Stat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: spend one point on a stat. The server checks the player has points. */
public record AllocateStatPacket(int statOrdinal) {
    public AllocateStatPacket(Stat stat) {
        this(stat.ordinal());
    }

    public static void encode(AllocateStatPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.statOrdinal);
    }

    public static AllocateStatPacket decode(FriendlyByteBuf buf) {
        return new AllocateStatPacket(buf.readVarInt());
    }

    public static void handle(AllocateStatPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        Stat stat = Stat.byOrdinal(msg.statOrdinal);
        if (player != null && stat != null) {
            LevelingService.allocate(player, stat);
        }
        ctx.setPacketHandled(true);
    }
}
