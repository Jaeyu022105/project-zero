package com.projectzero.network;

import com.projectzero.skills.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client to server: put a learned active skill in a hotbar skill slot (0-based), or clear it
 * with a null id. Sent by clicking a slot in the Status window. The server checks everything.
 */
public record AssignSkillSlotPacket(int slot, @Nullable ResourceLocation skillId) {
    public static void encode(AssignSkillSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
        buf.writeBoolean(msg.skillId != null);
        if (msg.skillId != null) buf.writeResourceLocation(msg.skillId);
    }

    public static AssignSkillSlotPacket decode(FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        return new AssignSkillSlotPacket(slot, buf.readBoolean() ? buf.readResourceLocation() : null);
    }

    public static void handle(AssignSkillSlotPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            SkillService.assignSlot(player, msg.slot, msg.skillId);
        }
        ctx.setPacketHandled(true);
    }
}
