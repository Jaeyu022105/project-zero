package com.projectzero.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import com.projectzero.leveling.PlayerStats;
import com.projectzero.leveling.Stat;
import com.projectzero.network.AllocateStatPacket;
import com.projectzero.network.CycleTitlePacket;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.OpenAccessoriesPacket;
import com.projectzero.network.SyncSkillsPacket;
import com.projectzero.skills.PlayerSkills;
import com.projectzero.skills.Skill;
import com.projectzero.titles.PlayerTitles;
import com.projectzero.titles.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The "System" status window. Opens with K by default.
 *
 * Landscape layout: a hologram of the player on the left, identity, stats and the skill
 * loadout on the right. Everything is drawn in a fixed 372x224 logical space and scaled down
 * to fit small windows or big GUI scales, so nothing spills off screen. Because of that
 * scaling it uses no vanilla widgets: buttons are drawn and hit-tested here.
 */
public class StatusScreen extends Screen {
    private static final int W = 372;
    private static final int H = 224;

    private static final int PANEL = 0xE00A1220;
    private static final int PANEL_DEEP = 0xF0060C16;
    private static final int BORDER = 0xFF3FD0FF;
    private static final int BORDER_DIM = 0xFF1F5A78;
    private static final int GLOW = 0x553FD0FF;
    private static final int DIVIDER = 0x803FD0FF;
    private static final int LABEL = 0xFF7FE8FF;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8090A0;
    private static final int HIGHLIGHT = 0xFFFFD34D;
    private static final int BAR_BG = 0xFF1A2A3A;
    private static final int XP_FILL = 0xFF3FD0FF;
    private static final int HP_FILL = 0xFFE04848;
    private static final int MP_FILL = 0xFF4F7CFF;

    // Hologram bay (left column).
    private static final int HX = 10, HY = 30, HW = 112, HH = 184;
    // Info column (right).
    private static final int RX = 134, RW = W - RX - 10;
    private static final int STAT_TOP = 104, STAT_ROW = 14;
    private static final int SKILL_Y = 180, SKILL_SLOT = 22;
    private static final int GEAR_X = W - 58, GEAR_Y = 6, GEAR_W = 48, GEAR_H = 14;

    private float scale = 1F;
    private int left;
    private int top;
    private float spin;

    public StatusScreen() {
        super(Component.translatable("screen.projectzero.status"));
    }

    @Override
    protected void init() {
        scale = Math.min(1F, Math.min((width - 8F) / W, (height - 8F) / H));
        left = Math.round((width - W * scale) / 2F);
        top = Math.round((height - H * scale) / 2F);
    }

    private double lx(double mouseX) { return (mouseX - left) / scale; }
    private double ly(double mouseY) { return (mouseY - top) / scale; }

    private static boolean in(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        spin = (System.currentTimeMillis() % 3_600_000L) * 0.03F; // degrees, ~30 per second
        double mx = lx(mouseX), my = ly(mouseY);

        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1F);
        renderPanel(g);
        renderHologramBay(g);
        g.pose().popPose();

        // The player model is drawn in real screen space (it sets its own scissor and depth).
        renderHologramEntity(g);

        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1F);
        renderHologramOverlay(g);
        List<FormattedCharSequence> tooltip = renderInfo(g, mx, my);
        g.pose().popPose();

        if (tooltip != null) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private void renderPanel(GuiGraphics g) {
        g.fill(-2, -2, W + 2, H + 2, GLOW);
        g.fill(0, 0, W, H, PANEL);
        g.renderOutline(0, 0, W, H, BORDER);
        // Corner ticks.
        int t = 7;
        g.fill(0, 0, t, 1, VALUE);         g.fill(0, 0, 1, t, VALUE);
        g.fill(W - t, 0, W, 1, VALUE);     g.fill(W - 1, 0, W, t, VALUE);
        g.fill(0, H - 1, t, H, VALUE);     g.fill(0, H - t, 1, H, VALUE);
        g.fill(W - t, H - 1, W, H, VALUE); g.fill(W - 1, H - t, W, H, VALUE);
        g.drawCenteredString(font, Component.literal("STATUS"), W / 2, 10, LABEL);
        g.fill(10, 24, W - 10, 25, DIVIDER);
    }

    private void renderHologramBay(GuiGraphics g) {
        g.fill(HX, HY, HX + HW, HY + HH, PANEL_DEEP);
        g.renderOutline(HX, HY, HW, HH, BORDER_DIM);
        // Faint grid.
        for (int x = HX + 8; x < HX + HW; x += 8) g.fill(x, HY + 1, x + 1, HY + HH - 1, 0x103FD0FF);
        for (int y = HY + 8; y < HY + HH; y += 8) g.fill(HX + 1, y, HX + HW - 1, y + 1, 0x103FD0FF);
        // Projector beam: a soft cone rising from the emitter.
        int cx = HX + HW / 2, base = HY + HH - 20;
        for (int i = 0; i < 120; i++) {
            int half = 10 + i * 30 / 120;
            int alpha = Math.max(0, 0x30 - i / 4);
            g.fill(cx - half, base - i, cx + half, base - i + 1, (alpha << 24) | 0x3FD0FF);
        }
        // Emitter disc (stacked ellipse rows), pulsing.
        float pulse = 0.6F + 0.4F * (float) Math.sin(spin * 0.08F);
        for (int r = 0; r < 6; r++) {
            int half = 34 - r * 4;
            int a = (int) ((0x40 + r * 0x20) * pulse);
            g.fill(cx - half, base + r, cx + half, base + r + 1, (Math.min(255, a) << 24) | 0x3FD0FF);
        }
        g.fill(cx - 36, base + 6, cx + 36, base + 7, BORDER);
        g.drawCenteredString(font, Component.literal("PLAYER"), cx, HY + HH - 11, MUTED);
    }

    private void renderHologramEntity(GuiGraphics g) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        float cx = left + (HX + HW / 2F) * scale;
        float cy = top + (HY + HH / 2F - 6) * scale;
        float size = 62F * scale;

        float bodyRot = player.yBodyRot, yRot = player.getYRot(), xRot = player.getXRot();
        float headO = player.yHeadRotO, head = player.yHeadRot;
        player.yBodyRot = 180F;
        player.setYRot(180F);
        player.setXRot(0F);
        player.yHeadRot = 180F;
        player.yHeadRotO = 180F;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(spin));
        // Cyan wash with a faint flicker: hologram, not a mirror.
        float flicker = 0.92F + 0.08F * (float) Math.sin(spin * 0.9F) * (float) Math.sin(spin * 0.37F);
        RenderSystem.setShaderColor(0.42F * flicker, 0.9F * flicker, 1F * flicker, 1F);
        InventoryScreen.renderEntityInInventory(g, cx, cy, size,
                new Vector3f(0F, player.getBbHeight() / 2F, 0F), pose, null, player);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headO;
        player.yHeadRot = head;
    }

    private void renderHologramOverlay(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        // Scanlines over the model, and one bright band sweeping upward.
        for (int y = HY + 2; y < HY + HH - 24; y += 3) g.fill(HX + 1, y, HX + HW - 1, y + 1, 0x1A0A1220);
        int band = HY + HH - 24 - (int) ((spin * 0.9F) % (HH - 26));
        g.fill(HX + 1, band, HX + HW - 1, band + 2, 0x303FD0FF);
        g.pose().popPose();
    }

    /** Draws the right column. Returns the tooltip to show, if any. */
    private List<FormattedCharSequence> renderInfo(GuiGraphics g, double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStats s = ClientStatsCache.get();
        Player player = mc.player;
        List<FormattedCharSequence> tip = null;

        // GEAR tab.
        boolean gearHover = in(mx, my, GEAR_X, GEAR_Y, GEAR_W, GEAR_H);
        button(g, GEAR_X, GEAR_Y, GEAR_W, GEAR_H, "GEAR", true, gearHover);

        int col1 = RX, col2 = RX + RW / 2 + 4;
        String name = player != null ? player.getGameProfile().getName() : "?";
        field(g, "NAME", font.plainSubstrByWidth(name, RW / 2 - 40), col1, 32);
        field(g, "LEVEL", String.valueOf(s.level()), col2, 32);

        // JOB
        PlayerJob job = ClientJobCache.get();
        g.drawString(font, "JOB: ", col1, 46, LABEL, false);
        int jobX = col1 + font.width("JOB: ");
        if (!job.hasJob()) {
            g.drawString(font, Component.translatable("projectzero.job.none"), jobX, 46, VALUE, false);
        } else {
            JobRarity rarity = ClientJobCache.rarity();
            Component jobName = Job.displayName(job.jobId());
            g.drawString(font, jobName, jobX, 46, rarity != null ? rarity.argb() : MUTED, false);
            if (in(mx, my, jobX, 45, font.width(jobName), 10)) {
                tip = wrap(Component.translatable("projectzero.job.tooltip",
                        rarity != null ? rarity.displayName() : Component.literal("?"), job.jobLevel()));
            }
        }

        // TITLE (click to cycle)
        PlayerTitles titles = ClientTitlesCache.get();
        g.drawString(font, "TITLE: ", col2, 46, LABEL, false);
        int titleX = col2 + font.width("TITLE: ");
        ResourceLocation eq = titles.equipped();
        String titleText = font.plainSubstrByWidth(eq != null ? Title.displayName(eq).getString() : "None",
                RX + RW - titleX);
        g.drawString(font, titleText, titleX, 46, eq != null ? HIGHLIGHT : (titles.unlocked().isEmpty() ? MUTED : VALUE), false);
        if (in(mx, my, col2, 44, RX + RW - col2, 12)) {
            List<Component> lines = new ArrayList<>();
            if (eq != null) {
                lines.add(Title.displayName(eq).copy().withStyle(ChatFormatting.GOLD));
                lines.add(Title.description(eq).copy().withStyle(ChatFormatting.WHITE));
            }
            lines.add(Component.translatable("projectzero.title.cycle_hint", titles.unlocked().size())
                    .withStyle(ChatFormatting.GRAY));
            tip = wrap(lines.toArray(Component[]::new));
        }

        // HP / MP / XP bars.
        if (player != null) {
            bar(g, col1, 60, RW / 2 - 6, "HP", Math.round(player.getHealth()), Math.round(player.getMaxHealth()), HP_FILL);
        }
        bar(g, col2, 60, RW / 2 - 4, "MP", ClientSkillsCache.currentMana(), ClientSkillsCache.maxMana(), MP_FILL);
        float progress = s.isMaxLevel() ? 1F : (float) s.xp() / s.xpToNext();
        g.fill(col1, 80, RX + RW, 84, BAR_BG);
        g.fill(col1, 80, col1 + Math.round(RW * Math.min(1F, progress)), 84, XP_FILL);
        String xpText = s.isMaxLevel() ? "MAX LEVEL" : "XP " + s.xp() + " / " + s.xpToNext();
        g.drawCenteredString(font, xpText, RX + RW / 2, 87, MUTED);

        g.fill(RX, 98, RX + RW, 99, DIVIDER);

        // Stats.
        boolean hasPoints = s.unspentPoints() > 0;
        for (Stat stat : Stat.values()) {
            int y = STAT_TOP + stat.ordinal() * STAT_ROW;
            g.drawString(font, stat.displayName().getString().toUpperCase(), col1, y, LABEL, false);
            String baseStr = String.valueOf(s.get(stat));
            int valX = RX + RW - 92;
            g.drawString(font, baseStr, valX, y, VALUE, false);
            int bonus = ClientBonusCache.get(stat);
            if (bonus != 0) {
                g.drawString(font, " (" + (bonus > 0 ? "+" : "") + bonus + ")", valX + font.width(baseStr), y,
                        bonus > 0 ? 0xFF55E075 : 0xFFFF6666, false);
            }
            int bx = RX + RW - 13, by = y - 2;
            button(g, bx, by, 12, 12, "+", hasPoints, in(mx, my, bx, by, 12, 12));
            if (in(mx, my, col1, y - 2, RW - 16, STAT_ROW)) tip = wrap(stat.description());
        }
        String pts = String.valueOf(s.unspentPoints());
        int ptsW = font.width("POINTS: " + pts);
        g.drawString(font, "POINTS: ", RX + RW - ptsW, 32, LABEL, false);
        g.drawString(font, pts, RX + RW - font.width(pts), 32, hasPoints ? HIGHLIGHT : MUTED, false);

        g.fill(RX, SKILL_Y - 8, RX + RW, SKILL_Y - 7, DIVIDER);

        // Skill loadout: the live preset's 4 slots. Click opens the Skills window.
        PlayerSkills skills = ClientSkillsCache.get();
        int slotStride = RW / 4;
        for (int i = 0; i < ModKeyBindings.SKILL_KEYS.length && i < 4; i++) {
            int sx = RX + i * slotStride, sy = SKILL_Y;
            ResourceLocation id = skills.slot(i);
            boolean hover = in(mx, my, sx, sy, slotStride - 4, SKILL_SLOT);
            g.fill(sx, sy, sx + SKILL_SLOT, sy + SKILL_SLOT, PANEL_DEEP);
            g.renderOutline(sx, sy, SKILL_SLOT, SKILL_SLOT, hover ? VALUE : (id != null ? BORDER : BORDER_DIM));
            if (id != null) {
                if (SkillHudOverlay.hasIcon(mc, id)) {
                    g.blit(SkillHudOverlay.iconOf(id), sx + 3, sy + 3, 0, 0, 16, 16, 16, 16);
                } else {
                    String ini = SkillHudOverlay.initials(Skill.displayName(id).getString());
                    g.drawString(font, ini, sx + (SKILL_SLOT - font.width(ini)) / 2, sy + 7, LABEL, false);
                }
            }
            String key = ModKeyBindings.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase();
            g.drawString(font, font.plainSubstrByWidth(key, slotStride - SKILL_SLOT - 6), sx + SKILL_SLOT + 4, sy + 1, LABEL, false);
            String skillName = id != null ? Skill.displayName(id).getString() : "Empty";
            g.pose().pushPose();
            g.pose().translate(sx + SKILL_SLOT + 4, sy + 12, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            g.drawString(font, font.plainSubstrByWidth(skillName, (int) ((slotStride - SKILL_SLOT - 8) / 0.75F)),
                    0, 0, id != null ? VALUE : MUTED, false);
            g.pose().popPose();
            if (hover) {
                List<Component> lines = new ArrayList<>();
                if (id != null) {
                    lines.add(Skill.displayName(id).copy().withStyle(ChatFormatting.AQUA));
                    SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
                    if (meta != null) lines.add(Component.literal(meta.manaCost() + " MP").withStyle(ChatFormatting.BLUE));
                }
                lines.add(Component.literal("Click to open the Skills window ("
                        + ModKeyBindings.OPEN_SKILLS.getTranslatedKeyMessage().getString().toUpperCase() + ").")
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal(activeLearned(skills).size() + " active skills learned.")
                        .withStyle(ChatFormatting.DARK_GRAY));
                tip = wrap(lines.toArray(Component[]::new));
            }
        }
        return tip;
    }

    private List<FormattedCharSequence> wrap(Component... lines) {
        List<FormattedCharSequence> out = new ArrayList<>();
        int max = Math.max(120, Math.min(220, width / 2 - 10));
        for (Component c : lines) out.addAll(font.split(c, max));
        return out;
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String text, boolean active, boolean hover) {
        int border = !active ? BORDER_DIM : hover ? VALUE : BORDER;
        g.fill(x, y, x + w, y + h, hover && active ? 0x603FD0FF : PANEL_DEEP);
        g.renderOutline(x, y, w, h, border);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, active ? (hover ? VALUE : LABEL) : MUTED);
    }

    private void bar(GuiGraphics g, int x, int y, int w, String label, int value, int max, int fill) {
        g.drawString(font, label, x, y, LABEL, false);
        String num = value + " / " + max;
        g.drawString(font, num, x + w - font.width(num), y, VALUE, false);
        g.fill(x, y + 10, x + w, y + 13, BAR_BG);
        float f = max <= 0 ? 0F : Math.min(1F, value / (float) max);
        g.fill(x, y + 10, x + Math.round(w * f), y + 13, fill);
    }

    private void field(GuiGraphics g, String label, String value, int x, int y) {
        g.drawString(font, label + ": ", x, y, LABEL, false);
        g.drawString(font, value, x + font.width(label + ": "), y, VALUE, false);
    }

    // ------------------------------------------------------------------ input

    private static List<ResourceLocation> activeLearned(PlayerSkills skills) {
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation id : skills.learned()) {
            SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
            if (meta != null && meta.active()) out.add(id);
        }
        out.sort(null);
        return out;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = lx(mouseX), my = ly(mouseY);

        if (button == 0 && in(mx, my, GEAR_X, GEAR_Y, GEAR_W, GEAR_H)) {
            click();
            ModNetwork.sendToServer(new OpenAccessoriesPacket());
            return true;
        }
        if (button == 0 && in(mx, my, RX + RW / 2 + 4, 44, RW / 2 - 4, 12)) {
            if (!ClientTitlesCache.get().unlocked().isEmpty()) {
                click();
                ModNetwork.sendToServer(new CycleTitlePacket());
            }
            return true;
        }
        if (button == 0 && ClientStatsCache.get().unspentPoints() > 0) {
            for (Stat stat : Stat.values()) {
                int y = STAT_TOP + stat.ordinal() * STAT_ROW - 2;
                if (in(mx, my, RX + RW - 13, y, 12, 12)) {
                    click();
                    ModNetwork.sendToServer(new AllocateStatPacket(stat));
                    return true;
                }
            }
        }
        // Skill slots: the loadout lives in its own window now.
        if (in(mx, my, RX, SKILL_Y, RW, SKILL_SLOT)) {
            click();
            minecraft.setScreen(new SkillLoadoutScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.OPEN_STATUS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
