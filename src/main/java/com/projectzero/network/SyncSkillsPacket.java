package com.projectzero.network;

import com.projectzero.client.ClientSkillsCache;
import com.projectzero.skills.PlayerSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;

/** Server to client: full skill state, mana, max mana, and lightweight metadata for learned skills. */
public record SyncSkillsPacket(PlayerSkills skills, int maxMana, Map<ResourceLocation, SkillMeta> meta) {
    public record SkillMeta(boolean active, int manaCost) {}

    public static void encode(SyncSkillsPacket msg, FriendlyByteBuf buf) {
        msg.skills.write(buf);
        buf.writeVarInt(msg.maxMana);
        buf.writeVarInt(msg.meta.size());
        for (Map.Entry<ResourceLocation, SkillMeta> e : msg.meta.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeBoolean(e.getValue().active());
            buf.writeVarInt(e.getValue().manaCost());
        }
    }

    public static SyncSkillsPacket decode(FriendlyByteBuf buf) {
        PlayerSkills skills = PlayerSkills.read(buf);
        int maxMana = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, SkillMeta> meta = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            meta.put(id, new SkillMeta(buf.readBoolean(), buf.readVarInt()));
        }
        return new SyncSkillsPacket(skills, maxMana, meta);
    }

    public static void handle(SyncSkillsPacket msg, CustomPayloadEvent.Context ctx) {
        ClientSkillsCache.set(msg.skills, msg.maxMana, msg.meta);
        ctx.setPacketHandled(true);
    }
}
