package com.projectzero.network;

import com.projectzero.skills.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client to server: make a skill preset live (0-based), or {@code -1} for the next one. */
public record SwitchPresetPacket(int preset) {
    public static void encode(SwitchPresetPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.preset + 1);
    }

    public static SwitchPresetPacket decode(FriendlyByteBuf buf) {
        return new SwitchPresetPacket(buf.readVarInt() - 1);
    }

    public static void handle(SwitchPresetPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) SkillService.switchPreset(player, msg.preset);
        ctx.setPacketHandled(true);
    }
}
