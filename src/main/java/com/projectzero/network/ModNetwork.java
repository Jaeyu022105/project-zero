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
    // Bump whenever the packet list or a packet's format changes.
    private static final int PROTOCOL_VERSION = 6;

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
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }
}
