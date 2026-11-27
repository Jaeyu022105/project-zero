package com.projectzero.client;

import com.projectzero.ui.NoticeType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-safe staging queue for incoming {@link com.projectzero.network.SystemNoticePacket}s.
 * Contains zero {@code net.minecraft.client} imports so packet handlers stay dedicated-server safe.
 */
public final class ClientNoticeCache {
    private ClientNoticeCache() {}

    public record PendingNotice(NoticeType type, Component title, Component body, @Nullable Component chatLine) {}

    private static final ArrayDeque<PendingNotice> PENDING = new ArrayDeque<>();

    public static synchronized void enqueue(NoticeType type, Component title, Component body, @Nullable Component chatLine) {
        PENDING.addLast(new PendingNotice(type, title, body, chatLine));
    }

    public static synchronized List<PendingNotice> drain() {
        if (PENDING.isEmpty()) return List.of();
        List<PendingNotice> out = new ArrayList<>(PENDING);
        PENDING.clear();
        return out;
    }

    public static synchronized void clear() {
        PENDING.clear();
    }
}
