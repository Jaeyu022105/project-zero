package com.projectzero.network;

import com.projectzero.ProjectZero;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    // 2: SyncJobPacket. 3: VisionOverlayPacket. 4: SyncSkillsPacket + CastSkillPacket.
    // 5: SyncTitlesPacket + CycleTitlePacket. 6: SyncStatBonusesPacket + OpenAccessoriesPacket.
    // 7: SyncWornGearPacket + AssignSkillSlotPacket + SkillVisualPacket.
    // Bump whenever the packet list or a packet's format changes.
    // 8: skill presets (AssignSkillSlotPacket preset field, SwitchPresetPacket, preset sync).
    // 9: SystemNoticePacket.
    private static final int PROTOCOL_VERSION = 9;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ProjectZero.id("main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncStatsPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatsPacket::encode)
                .decoder(SyncStatsPacket::decode)
                .consumerMainThread(SyncStatsPacket::handle)
                .add();

        CHANNEL.messageBuilder(AllocateStatPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AllocateStatPacket::encode)
                .decoder(AllocateStatPacket::decode)
                .consumerMainThread(AllocateStatPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncJobPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncJobPacket::encode)
                .decoder(SyncJobPacket::decode)
                .consumerMainThread(SyncJobPacket::handle)
                .add();

        CHANNEL.messageBuilder(VisionOverlayPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VisionOverlayPacket::encode)
                .decoder(VisionOverlayPacket::decode)
                .consumerMainThread(VisionOverlayPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncSkillsPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillsPacket::encode)
                .decoder(SyncSkillsPacket::decode)
                .consumerMainThread(SyncSkillsPacket::handle)
                .add();

        CHANNEL.messageBuilder(CastSkillPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastSkillPacket::encode)
                .decoder(CastSkillPacket::decode)
                .consumerMainThread(CastSkillPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncTitlesPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTitlesPacket::encode)
                .decoder(SyncTitlesPacket::decode)
                .consumerMainThread(SyncTitlesPacket::handle)
                .add();

        CHANNEL.messageBuilder(CycleTitlePacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CycleTitlePacket::encode)
                .decoder(CycleTitlePacket::decode)
                .consumerMainThread(CycleTitlePacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncStatBonusesPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatBonusesPacket::encode)
                .decoder(SyncStatBonusesPacket::decode)
                .consumerMainThread(SyncStatBonusesPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenAccessoriesPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenAccessoriesPacket::encode)
                .decoder(OpenAccessoriesPacket::decode)
                .consumerMainThread(OpenAccessoriesPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncWornGearPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWornGearPacket::encode)
                .decoder(SyncWornGearPacket::decode)
                .consumerMainThread(SyncWornGearPacket::handle)
                .add();

        CHANNEL.messageBuilder(SwitchPresetPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwitchPresetPacket::encode)
                .decoder(SwitchPresetPacket::decode)
                .consumerMainThread(SwitchPresetPacket::handle)
                .add();
        CHANNEL.messageBuilder(AssignSkillSlotPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AssignSkillSlotPacket::encode)
                .decoder(AssignSkillSlotPacket::decode)
                .consumerMainThread(AssignSkillSlotPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkillVisualPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillVisualPacket::encode)
                .decoder(SkillVisualPacket::decode)
                .consumerMainThread(SkillVisualPacket::handle)
                .add();

        CHANNEL.messageBuilder(SystemNoticePacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SystemNoticePacket::encode)
                .decoder(SystemNoticePacket::decode)
                .consumerMainThread(SystemNoticePacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    /** Sends to {@code player} and every client tracking them. */
    public static void sendToTrackingAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    /** Sends to every client tracking {@code entity} (and the entity itself if it is a player). */
    public static void sendToTrackingEntity(net.minecraft.world.entity.Entity entity, Object message) {
        CHANNEL.send(message, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity));
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }
}
