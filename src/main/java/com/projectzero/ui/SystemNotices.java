package com.projectzero.ui;

import com.projectzero.network.ModNetwork;
import com.projectzero.network.SystemNoticePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side API for sending bottom-right System notices (DESIGN-DOCUMENT.md §11).
 *
 * <p>Other systems call {@link #send} for notice-only alerts (such as cooldown-ready pings) or
 * {@link #system} to send both the notice and an optional {@code [System]} chat line (which the
 * receiving client only prints when {@code ClientConfig.NOTICE_CHAT_LINES} is enabled).
 */
public final class SystemNotices {
    private SystemNotices() {}

    /** Sends a bottom-right System notice with no accompanying chat line. */
    public static void send(ServerPlayer player, NoticeType type, Component title, Component body) {
        ModNetwork.sendToPlayer(player, new SystemNoticePacket(type.ordinal(), title, body, null));
    }

    /**
     * Sends a bottom-right System notice using {@code type.defaultTitle()} and attaches {@code message}
     * as the optional {@code [System]} chat line (controlled on the client by {@code ui.noticeChatLines}).
     */
    public static void system(ServerPlayer player, NoticeType type, Component message) {
        system(player, type, type.defaultTitle(), message, message);
    }

    /**
     * Sends a bottom-right System notice with a custom {@code title} and {@code body}, plus an
     * optional {@code [System]} chat line controlled by {@code ui.noticeChatLines}.
     */
    public static void system(ServerPlayer player, NoticeType type, Component title, Component body,
                              @Nullable Component chatLine) {
        ModNetwork.sendToPlayer(player, new SystemNoticePacket(type.ordinal(), title, body, chatLine));
    }
}
