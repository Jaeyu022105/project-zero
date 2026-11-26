package com.projectzero.network;

import com.projectzero.client.ClientBonusCache;
import com.projectzero.leveling.Stat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.EnumMap;
import java.util.Map;

/** Server to client: total bonus points per stat from equipped Title and Accessories. */
public record SyncStatBonusesPacket(Map<Stat, Integer> bonuses) {
    public static void encode(SyncStatBonusesPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.bonuses.size());
        for (Map.Entry<Stat, Integer> e : msg.bonuses.entrySet()) {
            buf.writeVarInt(e.getKey().ordinal());
            buf.writeVarInt(e.getValue());
        }
    }

    public static SyncStatBonusesPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        EnumMap<Stat, Integer> map = new EnumMap<>(Stat.class);
        for (int i = 0; i < n; i++) {
            Stat s = Stat.values()[Math.max(0, Math.min(Stat.values().length - 1, buf.readVarInt()))];
            map.put(s, buf.readVarInt());
        }
        return new SyncStatBonusesPacket(map);
    }

    public static void handle(SyncStatBonusesPacket msg, CustomPayloadEvent.Context ctx) {
        ClientBonusCache.set(msg.bonuses);
        ctx.setPacketHandled(true);
    }
}
