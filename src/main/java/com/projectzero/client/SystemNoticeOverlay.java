package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import com.projectzero.ui.NoticeRules;
import com.projectzero.ui.NoticeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Bottom-right System notice stack (DESIGN-DOCUMENT.md §11).
 *
 * <p>Panels slide in from the right ({@link NoticeRules#SLIDE_IN_TICKS}), hold ~4s, and fade out
 * ({@link NoticeRules#FADE_OUT_TICKS}), stacking upward with at most {@link NoticeRules#MAX_VISIBLE}
 * on screen (oldest leaves first when a 5th arrives).
 *
 * <p>TODO(claude-art): polish panel frame, header ornaments, and custom System chimes.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SystemNoticeOverlay {
    private SystemNoticeOverlay() {}

    private static final int PANEL_RGB = 0x0A1220;
    private static final int GLOW_RGB = 0x3FD0FF;
    private static final int BORDER_RGB = 0x3FD0FF;
    private static final int LABEL_RGB = 0x7FE8FF;
    private static final int VALUE_RGB = 0xFFFFFF;

    private static final int PANEL_W = 166;
    private static final int GAP = 4;
    private static final int MARGIN = 6;

    private static final class ActiveNotice {
        final NoticeType type;
        final String header;
        final Component body;
        int age = 0;
        float shownY = Float.NaN;

        ActiveNotice(NoticeType type, Component title, Component body) {
            this.type = type;
            this.header = title.getString().toUpperCase(Locale.ROOT);
            this.body = body;
        }
    }

    private static final List<ActiveNotice> ACTIVE = new ArrayList<>();
    private static boolean installed;
    private static long lastSoundTick = -10L;

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static List<String> activeBodies() {
        return ACTIVE.stream().map(n -> n.body.getString()).toList();
    }

    public static void clear() {
        ACTIVE.clear();
        ClientNoticeCache.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!installed && mc.gui != null) {
            installed = true;
            try {
                for (Field f : Gui.class.getDeclaredFields()) {
                    if (f.getType() == LayeredDraw.class) {
                        f.setAccessible(true);
                        ((LayeredDraw) f.get(mc.gui)).add(SystemNoticeOverlay::render);
                        break;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                ProjectZero.LOGGER.warn("SystemNoticeOverlay: could not attach to Gui LayeredDraw", e);
            }
        }

        if (mc.player == null) return;

        List<ClientNoticeCache.PendingNotice> incoming = ClientNoticeCache.drain();
        for (ClientNoticeCache.PendingNotice p : incoming) {
            while (ACTIVE.size() >= NoticeRules.MAX_VISIBLE) {
                // Oldest leaves first when a 5th arrives so at most 4 stack on screen.
                ACTIVE.remove(0);
            }
            ACTIVE.add(new ActiveNotice(p.type(), p.title(), p.body()));
            playChime(mc, p.type());

            if (p.chatLine() != null && ClientConfig.NOTICE_CHAT_LINES.get()) {
                mc.gui.getChat().addMessage(Component.literal("[System] ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(p.chatLine()));
            }
        }

        if (!mc.isPaused()) {
            Iterator<ActiveNotice> it = ACTIVE.iterator();
            while (it.hasNext()) {
                ActiveNotice n = it.next();
                n.age++;
                if (n.age >= NoticeRules.TOTAL_TICKS) {
                    it.remove();
                }
            }
        }
    }

    private static void playChime(Minecraft mc, NoticeType type) {
        if (mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now == lastSoundTick) return;
        lastSoundTick = now;

        // TODO(claude-art): replace placeholder vanilla chimes with custom soft System chimes.
        SoundEvent sound = switch (type) {
            case LEVEL_UP -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case SKILL -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case QUEST -> SoundEvents.BOOK_PAGE_TURN;
            case GATE -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case ITEM -> SoundEvents.AMETHYST_CLUSTER_STEP;
            case COOLDOWN -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case TITLE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case WARNING -> SoundEvents.NOTE_BLOCK_BASS.value();
        };
        float pitch = switch (type) {
            case LEVEL_UP -> 1.35F;
            case SKILL -> 1.4F;
            case QUEST -> 1.15F;
            case GATE -> 0.85F;
            case ITEM -> 1.3F;
            case COOLDOWN -> 1.6F;
            case TITLE -> 1.5F;
            case WARNING -> 0.75F;
        };
        mc.player.playSound(sound, 0.28F, pitch);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || ACTIVE.isEmpty() || VisionOverlayState.coverage() > 0.5F) {
            return;
        }

        float pt = mc.isPaused() ? 0F : delta.getGameTimeDeltaPartialTick(false);
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        int baseRightX = sw - MARGIN - PANEL_W;
        int cursorBottom = sh - MARGIN;

        // Render newest at bottom-right, older notices stacked upward above it.
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            ActiveNotice n = ACTIVE.get(i);
            List<FormattedCharSequence> lines = mc.font.split(n.body, PANEL_W - 12);
            int lineCount = Math.min(2, Math.max(1, lines.size()));
            int panelH = 16 + lineCount * 9 + 4;

            int targetY = cursorBottom - panelH;
            cursorBottom = targetY - GAP;
            if (Float.isNaN(n.shownY)) {
                n.shownY = targetY;
            } else {
                n.shownY += (targetY - n.shownY) * 0.35F;
            }

            float age = n.age + pt;
            float slideFrac = Mth.clamp(age / NoticeRules.SLIDE_IN_TICKS, 0F, 1F);
            float easeOut = 1F - (1F - slideFrac) * (1F - slideFrac);
            float slideOffsetX = (1F - easeOut) * (PANEL_W + MARGIN + 6);

            int fadeStart = NoticeRules.SLIDE_IN_TICKS + NoticeRules.HOLD_TICKS;
            float alpha = age <= fadeStart
                    ? Mth.clamp(0.35F + 0.65F * easeOut, 0F, 1F)
                    : Mth.clamp(1F - (age - fadeStart) / NoticeRules.FADE_OUT_TICKS, 0F, 1F);
            if (alpha <= 0.02F) continue;

            int drawY = Math.round(n.shownY);
            g.pose().pushPose();
            g.pose().translate(slideOffsetX, 0F, 0F);

            drawNoticePanel(g, mc, baseRightX, drawY, PANEL_W, panelH, n.type.accentArgb(), alpha, n.header, lines, lineCount);

            g.pose().popPose();
        }
    }

    private static void drawNoticePanel(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                                        int accentArgb, float alpha, String header,
                                        List<FormattedCharSequence> lines, int lineCount) {
        int glow = withAlpha(GLOW_RGB, Math.round(0x40 * alpha));
        int bg = withAlpha(PANEL_RGB, Math.round(0xDC * alpha));
        int border = withAlpha(BORDER_RGB, Math.round(0xFF * alpha));
        int divider = withAlpha(BORDER_RGB, Math.round(0x66 * alpha));
        int accent = withAlpha(accentArgb & 0xFFFFFF, Math.round(0xFF * alpha));
        int labelCol = withAlpha(LABEL_RGB, Math.round(0xFF * alpha));
        int textCol = withAlpha(VALUE_RGB, Math.round(0xFF * alpha));

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, glow);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        // Left per-type accent stripe
        g.fill(x + 1, y + 1, x + 3, y + h - 1, accent);
        g.fill(x + 5, y + 13, x + w - 4, y + 14, divider);

        g.pose().pushPose();
        g.pose().translate(x + 6, y + 4, 0F);
        g.pose().scale(0.85F, 0.85F, 1F);
        g.drawString(mc.font, header, 0, 0, labelCol, false);
        g.pose().popPose();

        for (int l = 0; l < lineCount; l++) {
            g.drawString(mc.font, lines.get(l), x + 6, y + 16 + l * 9, textCol, false);
        }
    }

    private static int withAlpha(int rgb, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
