package com.projectzero.network;

import com.projectzero.skills.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client to server: put a learned active skill in a slot (0-based) of a preset, or clear it with
 * a null id. Sent from the Skills window. The server checks everything.
 */
public record AssignSkillSlotPacket(int preset, int slot, @Nullable ResourceLocation skillId) {
    public static void encode(AssignSkillSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.preset);
        buf.writeVarInt(msg.slot);
        buf.writeBoolean(msg.skillId != null);
        if (msg.skillId != null) buf.writeResourceLocation(msg.skillId);
    }

    public static AssignSkillSlotPacket decode(FriendlyByteBuf buf) {
        int preset = buf.readVarInt();
        int slot = buf.readVarInt();
        return new AssignSkillSlotPacket(preset, slot, buf.readBoolean() ? buf.readResourceLocation() : null);
    }

    public static void handle(AssignSkillSlotPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            SkillService.assignSlot(player, msg.preset, msg.slot, msg.skillId);
        }
        ctx.setPacketHandled(true);
    }
}
