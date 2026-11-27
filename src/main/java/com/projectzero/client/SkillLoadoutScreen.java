package com.projectzero.client;

import com.projectzero.network.AssignSkillSlotPacket;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SwitchPresetPacket;
import com.projectzero.network.SyncSkillsPacket;
import com.projectzero.skills.PlayerSkills;
import com.projectzero.skills.Skill;
import com.projectzero.skills.SkillRules;
import com.projectzero.skills.SkillService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Skills window (J by default): every learned active skill on the left, the skill presets on
 * the right. Drag a skill onto a slot, or click it and then click a slot. Drag between slots to
 * swap, drag a slot out to clear it, right-click a slot to clear it. Click a preset's header to
 * make it live; the cycle key (R) swaps presets mid-fight. Same logical-space scaling as the
 * Status window, and the System palette.
 */
public class SkillLoadoutScreen extends Screen {
    private static final int W = 372, H = 224;
    private static final int PANEL = 0xE00A1220, PANEL_DEEP = 0xF0060C16, BORDER = 0xFF3FD0FF, BORDER_DIM = 0xFF1F5A78;
    private static final int GLOW = 0x553FD0FF, DIVIDER = 0x803FD0FF, LABEL = 0xFF7FE8FF, VALUE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8090A0, HIGHLIGHT = 0xFFFFD34D, MANA = 0xFF6F94FF;

    // Learned list (left).
    private static final int LX = 10, LY = 34, LW = 196, LH = 170, TILE_W = 96, TILE_H = 26, COLS = 2;
    // Presets (right).
    private static final int PX = 216, PY = 30, PW = W - PX - 10, PRESET_H = 58, SLOT = 24;

    private float scale = 1F;
    private int left, top;
    private int scroll;
    private @Nullable ResourceLocation held;
    private int fromPreset = -1, fromSlot = -1;
    private double pressX, pressY;
    private boolean dragging;

    public SkillLoadoutScreen() {
        super(Component.translatable("screen.projectzero.skills"));
    }

    @Override
    protected void init() {
        scale = Math.min(1F, Math.min((width - 8F) / W, (height - 8F) / H));
        left = Math.round((width - W * scale) / 2F);
        top = Math.round((height - H * scale) / 2F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private double lx(double x) { return (x - left) / scale; }
    private double ly(double y) { return (y - top) / scale; }

    private static boolean in(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    private static List<ResourceLocation> activeLearned() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation id : ClientSkillsCache.get().learned()) {
            SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
            if (meta != null && meta.active()) out.add(id);
        }
        return out;
    }

    private int maxScroll(int count) {
        int rows = (count + COLS - 1) / COLS;
        return Math.max(0, rows * (TILE_H + 4) - LH);
    }

    private static int slotX(int s) { return PX + 6 + s * (SLOT + 10); }
    private static int presetY(int p) { return PY + p * PRESET_H; }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        double mx = lx(mouseX), my = ly(mouseY);
        List<FormattedCharSequence> tip = null;
        PlayerSkills skills = ClientSkillsCache.get();
        List<ResourceLocation> pool = activeLearned();
        scroll = Mth.clamp(scroll, 0, maxScroll(pool.size()));

        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1F);

        // Window.
        g.fill(-2, -2, W + 2, H + 2, GLOW);
        g.fill(0, 0, W, H, PANEL);
        g.renderOutline(0, 0, W, H, BORDER);
        String title = "SKILLS";
        g.drawString(font, title, (W - font.width(title)) / 2, 8, LABEL, false);
        g.fill(10, 20, W - 10, 21, DIVIDER);

        // Learned skills.
        g.drawString(font, "LEARNED", LX, 24, LABEL, false);
        String count = pool.size() + " ACTIVE";
        g.drawString(font, count, LX + LW - font.width(count), 24, MUTED, false);
        g.fill(LX - 2, LY - 2, LX + LW + 2, LY + LH + 2, PANEL_DEEP);
        g.enableScissor(Math.round(left + LX * scale), Math.round(top + LY * scale),
                Math.round(left + (LX + LW) * scale), Math.round(top + (LY + LH) * scale));
        for (int i = 0; i < pool.size(); i++) {
            ResourceLocation id = pool.get(i);
            int tx = LX + (i % COLS) * (TILE_W + 4), ty = LY + (i / COLS) * (TILE_H + 4) - scroll;
            if (ty + TILE_H < LY || ty > LY + LH) continue;
            boolean hover = in(mx, my, tx, ty, TILE_W, TILE_H) && in(mx, my, LX, LY, LW, LH);
            boolean slotted = skills.slotOf(id) >= 0;
            g.fill(tx, ty, tx + TILE_W, ty + TILE_H, hover ? 0x40FFFFFF : 0x200A1A2A);
            g.renderOutline(tx, ty, TILE_W, TILE_H, id.equals(held) ? HIGHLIGHT : hover ? VALUE : slotted ? BORDER : BORDER_DIM);
            icon(g, id, tx + 4, ty + 4);
            g.drawString(font, font.plainSubstrByWidth(Skill.displayName(id).getString(), TILE_W - 30), tx + 26, ty + 4, VALUE, false);
            SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
            if (meta != null) small(g, meta.manaCost() + " MP", tx + 26, ty + 15, MANA);
            if (hover && held == null) tip = skillTip(id, "Drag onto a slot, or click it, then click a slot.");
        }
        g.disableScissor();
        if (pool.isEmpty()) {
            g.drawWordWrap(font, Component.literal("No active skills yet. Skillbooks and your job grant them."),
                    LX + 6, LY + 8, LW - 12, MUTED);
        }

        // Presets.
        for (int p = 0; p < SkillRules.PRESETS; p++) {
            int y = presetY(p);
            boolean live = p == skills.activePreset();
            boolean headHover = in(mx, my, PX, y, PW, 12);
            g.fill(PX, y + 12, PX + PW, y + PRESET_H - 4, live ? 0x30205A78 : PANEL_DEEP);
            g.renderOutline(PX, y + 12, PW, PRESET_H - 16, live ? BORDER : BORDER_DIM);
            g.drawString(font, "PRESET " + SkillService.roman(p + 1), PX, y + 2, live ? LABEL : (headHover ? VALUE : MUTED), false);
            String tag = live ? "LIVE" : headHover ? "SET LIVE" : "";
            g.drawString(font, tag, PX + PW - font.width(tag), y + 2, live ? HIGHLIGHT : VALUE, false);
            if (headHover && !live) tip = wrap(Component.literal("Make this preset live.").withStyle(ChatFormatting.GRAY));
            for (int s = 0; s < SkillRules.ACTIVE_SLOTS; s++) {
                int sx = slotX(s), sy = y + 16;
                ResourceLocation id = skills.slot(p, s);
                boolean hover = in(mx, my, sx, sy, SLOT, SLOT);
                boolean source = dragging && p == fromPreset && s == fromSlot;
                g.fill(sx, sy, sx + SLOT, sy + SLOT, 0xF0040810);
                g.renderOutline(sx, sy, SLOT, SLOT, hover ? (held != null ? HIGHLIGHT : VALUE) : id != null ? BORDER : BORDER_DIM);
                if (id != null && !source) icon(g, id, sx + 4, sy + 4);
                if (live) {
                    String key = ModKeyBindings.SKILL_KEYS[s].getTranslatedKeyMessage().getString().toUpperCase();
                    small(g, font.plainSubstrByWidth(key, 40), sx + (SLOT - Math.round(font.width(key) * 0.75F)) / 2, sy + SLOT + 2, LABEL);
                }
                if (hover && held == null && id != null) tip = skillTip(id, "Drag to move or swap. Right-click to clear.");
            }
        }
        String cycle = ModKeyBindings.CYCLE_PRESET.getTranslatedKeyMessage().getString().toUpperCase();
        // The hint is wider than the preset column, so it runs under the whole window, centred.
        // If it still does not fit (long key names, other fonts) it breaks into two lines.
        String hintA = cycle + ": NEXT PRESET IN A FIGHT";
        String hintB = "SWAPPING LOCKS SKILLS FOR 0.5S";
        String hint = hintA + "  ·  " + hintB;
        int room = Math.round((W - 2 * LX) / 0.75F);
        if (font.width(hint) <= room) {
            small(g, hint, (W - Math.round(font.width(hint) * 0.75F)) / 2, H - 12, MUTED);
        } else {
            small(g, hintA, (W - Math.round(font.width(hintA) * 0.75F)) / 2, H - 17, MUTED);
            small(g, hintB, (W - Math.round(font.width(hintB) * 0.75F)) / 2, H - 10, MUTED);
        }
        g.pose().popPose();

        // The held skill follows the cursor.
        if (held != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            icon(g, held, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        } else if (tip != null) {
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    private void icon(GuiGraphics g, ResourceLocation id, int x, int y) {
        if (SkillHudOverlay.hasIcon(minecraft, id)) {
            g.blit(SkillHudOverlay.iconOf(id), x, y, 0, 0, 16, 16, 16, 16);
        } else {
            String ini = SkillHudOverlay.initials(Skill.displayName(id).getString());
            g.drawString(font, ini, x + (16 - font.width(ini)) / 2, y + 4, LABEL, false);
        }
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private List<FormattedCharSequence> skillTip(ResourceLocation id, String hint) {
        List<Component> lines = new ArrayList<>();
        lines.add(Skill.displayName(id).copy().withStyle(ChatFormatting.AQUA));
        SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
        if (meta != null) lines.add(Component.literal(meta.manaCost() + " MP").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("skill." + id.getNamespace() + "." + id.getPath() + ".desc").withStyle(ChatFormatting.WHITE));
        int cd = ClientSkillsCache.get().cooldownRemaining(id);
        if (cd > 0) lines.add(Component.literal(String.format("Cooldown: %.1fs", cd / 20.0)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
        return wrap(lines.toArray(Component[]::new));
    }

    private List<FormattedCharSequence> wrap(Component... lines) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component c : lines) out.addAll(font.split(c, 180));
        return out;
    }

    // ------------------------------------------------------------------ input

    /** The preset slot under the cursor as {preset, slot}, or null. */
    private int[] slotAt(double mx, double my) {
        for (int p = 0; p < SkillRules.PRESETS; p++) {
            for (int s = 0; s < SkillRules.ACTIVE_SLOTS; s++) {
                if (in(mx, my, slotX(s), presetY(p) + 16, SLOT, SLOT)) return new int[] {p, s};
            }
        }
        return null;
    }

    private @Nullable ResourceLocation tileAt(double mx, double my) {
        if (!in(mx, my, LX, LY, LW, LH)) return null;
        List<ResourceLocation> pool = activeLearned();
        for (int i = 0; i < pool.size(); i++) {
            int tx = LX + (i % COLS) * (TILE_W + 4), ty = LY + (i / COLS) * (TILE_H + 4) - scroll;
            if (in(mx, my, tx, ty, TILE_W, TILE_H)) return pool.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = lx(mouseX), my = ly(mouseY);
        int[] at = slotAt(mx, my);
        if (held != null) {                       // click-to-place mode
            if (at != null) place(at[0], at[1]);
            drop();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && at != null) {
            if (ClientSkillsCache.get().slot(at[0], at[1]) != null) assign(at[0], at[1], null);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        for (int p = 0; p < SkillRules.PRESETS; p++) {
            if (in(mx, my, PX, presetY(p), PW, 12) && p != ClientSkillsCache.get().activePreset()) {
                ClientSkillsCache.get().setActivePreset(p);
                ModNetwork.sendToServer(new SwitchPresetPacket(p));
                click();
                return true;
            }
        }
        ResourceLocation tile = tileAt(mx, my);
        if (tile != null) {
            pick(tile, -1, -1, mx, my);
            return true;
        }
        if (at != null && ClientSkillsCache.get().slot(at[0], at[1]) != null) {
            pick(ClientSkillsCache.get().slot(at[0], at[1]), at[0], at[1], mx, my);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (held != null && !dragging && Math.hypot(lx(mouseX) - pressX, ly(mouseY) - pressY) > 3) dragging = true;
        return held != null || super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (held != null && dragging) {
            int[] at = slotAt(lx(mouseX), ly(mouseY));
            if (at != null) place(at[0], at[1]);
            else if (fromPreset >= 0) assign(fromPreset, fromSlot, null);   // dragged out: clear
            drop();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (in(lx(mouseX), ly(mouseY), LX, LY, LW, LH)) {
            scroll = Mth.clamp(scroll - (int) (scrollY * 15), 0, maxScroll(activeLearned().size()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.OPEN_SKILLS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && held != null) {
            drop();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void pick(ResourceLocation id, int preset, int slot, double mx, double my) {
        held = id;
        fromPreset = preset;
        fromSlot = slot;
        pressX = mx;
        pressY = my;
        dragging = false;
        click();
    }

    private void drop() {
        held = null;
        fromPreset = fromSlot = -1;
        dragging = false;
    }

    /** Puts the held skill in a slot. Moving between slots of one preset swaps the two. */
    private void place(int preset, int slot) {
        if (held == null || (preset == fromPreset && slot == fromSlot)) return;
        ResourceLocation previous = ClientSkillsCache.get().slot(preset, slot);
        assign(preset, slot, held);
        if (fromPreset == preset && previous != null) assign(fromPreset, fromSlot, previous);
    }

    /** Sends the change and applies it locally so the window updates at once. */
    private void assign(int preset, int slot, @Nullable ResourceLocation id) {
        ClientSkillsCache.get().setSlot(preset, slot, id);
        ModNetwork.sendToServer(new AssignSkillSlotPacket(preset, slot, id));
        click();
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.35F));
        }
    }
}
