package com.projectzero.network;

import com.projectzero.ProjectZero;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    // 2: added SyncJobPacket. Bump whenever the packet list or a packet's format changes.
    private static final int PROTOCOL_VERSION = 2;

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
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }
}
