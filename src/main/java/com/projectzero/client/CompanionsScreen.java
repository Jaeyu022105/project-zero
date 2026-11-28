package com.projectzero.client;

import com.projectzero.companions.CompanionEntry;
import com.projectzero.companions.CompanionRules;
import com.projectzero.companions.PlayerCompanions;
import com.projectzero.leveling.Stat;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SpendCompanionPointPacket;
import com.projectzero.network.UseBondCrystalPacket;
import com.projectzero.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** System roster and stat allocation screen for summon-capable jobs. */
public final class CompanionsScreen extends Screen {
    private static final int W = 430, H = 248;
    private static final int PANEL = 0xE00A1220, PANEL_DEEP = 0xF0060C16;
    private static final int BORDER = 0xFF3FD0FF, BORDER_DIM = 0xFF1F5A78;
    private static final int GLOW = 0x553FD0FF, DIVIDER = 0x803FD0FF;
    private static final int LABEL = 0xFF7FE8FF, VALUE = 0xFFFFFFFF, MUTED = 0xFF8090A0;
    private static final int HIGHLIGHT = 0xFFFFD34D, BAR_BG = 0xFF1A2A3A;
    private static final int XP_FILL = 0xFF3FD0FF;
    private static final int LIST_X = 10, LIST_Y = 32, LIST_W = 137, LIST_H = 206;
    private static final int INFO_X = 157, INFO_Y = 32, INFO_W = 263, INFO_H = 206;
    private static final int ROWS = 9, ROW_H = 20;

    private float scale = 1F;
    private int left, top, rosterScroll;
    private UUID selectedId;

    public CompanionsScreen() {
        super(Component.translatable("screen.projectzero.companions"));
    }

    @Override
    protected void init() {
        scale = Math.min(1F, Math.min((width - 8F) / W, (height - 8F) / H));
        left = Math.round((width - W * scale) / 2F);
        top = Math.round((height - H * scale) / 2F);
    }

    private double lx(double x) { return (x - left) / scale; }
    private double ly(double y) { return (y - top) / scale; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        double mx = lx(mouseX), my = ly(mouseY);
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1F);
        drawPanel(g, mx, my);
        g.pose().popPose();
    }

    private void drawPanel(GuiGraphics g, double mx, double my) {
        g.fill(-2, -2, W + 2, H + 2, GLOW);
        g.fill(0, 0, W, H, PANEL);
        g.renderOutline(0, 0, W, H, BORDER);
        g.drawCenteredString(font, Component.translatable("screen.projectzero.companions.title"), W / 2, 9, LABEL);
        g.fill(10, 25, W - 10, 26, DIVIDER);
        button(g, 10, 7, 42, 14, Component.translatable("screen.projectzero.companions.back"),
                true, inside(mx, my, 10, 7, 42, 14));
        g.fill(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H, PANEL_DEEP);
        g.renderOutline(LIST_X, LIST_Y, LIST_W, LIST_H, BORDER_DIM);
        g.fill(INFO_X, INFO_Y, INFO_X + INFO_W, INFO_Y + INFO_H, PANEL_DEEP);
        g.renderOutline(INFO_X, INFO_Y, INFO_W, INFO_H, BORDER_DIM);

        List<CompanionEntry> entries = ClientCompanionsCache.get().entries().stream().toList();
        int maxScroll = Math.max(0, entries.size() - ROWS);
        rosterScroll = Math.max(0, Math.min(rosterScroll, maxScroll));
        if (selectedId == null || entries.stream().noneMatch(entry -> entry.id().equals(selectedId))) {
            selectedId = entries.isEmpty() ? null : entries.get(0).id();
        }
        g.drawString(font, Component.translatable("screen.projectzero.companions.roster"), LIST_X + 6, LIST_Y + 6, LABEL, false);
        for (int row = 0; row < ROWS && row + rosterScroll < entries.size(); row++) {
            CompanionEntry entry = entries.get(row + rosterScroll);
            int y = LIST_Y + 22 + row * ROW_H;
            boolean selected = entry.id().equals(selectedId);
            boolean hover = inside(mx, my, LIST_X + 3, y, LIST_W - 6, ROW_H - 1);
            if (selected) g.fill(LIST_X + 3, y, LIST_X + LIST_W - 3, y + ROW_H - 1, 0x503FD0FF);
            if (hover) g.fill(LIST_X + 3, y, LIST_X + LIST_W - 3, y + ROW_H - 1, 0x303FD0FF);
            g.drawString(font, displayName(entry), LIST_X + 7, y + 2, selected ? VALUE : LABEL, false);
            g.drawString(font, Component.translatable("screen.projectzero.companions.level_short", entry.level()),
                    LIST_X + 7, y + 11, MUTED, false);
        }
        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.projectzero.companions.empty"),
                    LIST_X + LIST_W / 2, LIST_Y + 92, MUTED);
        }

        CompanionEntry selected = entries.stream().filter(entry -> entry.id().equals(selectedId)).findFirst().orElse(null);
        if (selected == null) {
            g.drawCenteredString(font, Component.translatable("screen.projectzero.companions.select"),
                    INFO_X + INFO_W / 2, INFO_Y + 92, MUTED);
            return;
        }

        g.drawString(font, displayName(selected), INFO_X + 10, INFO_Y + 7, VALUE, false);
        g.drawString(font, Component.translatable("screen.projectzero.companions.level", selected.level()),
                INFO_X + 10, INFO_Y + 21, LABEL, false);
        long needed = CompanionRules.xpToNext(selected.level());
        float progress = selected.level() >= CompanionRules.MAX_LEVEL ? 1F :
                Math.min(1F, selected.xp() / (float) Math.max(1L, needed));
        g.fill(INFO_X + 10, INFO_Y + 37, INFO_X + INFO_W - 10, INFO_Y + 42, BAR_BG);
        g.fill(INFO_X + 10, INFO_Y + 37, INFO_X + 10 + Math.round((INFO_W - 20) * progress), INFO_Y + 42, XP_FILL);
        g.drawString(font, Component.translatable("screen.projectzero.companions.xp", selected.xp(), needed),
                INFO_X + 10, INFO_Y + 46, MUTED, false);

        for (Stat stat : Stat.values()) {
            int y = INFO_Y + 64 + stat.ordinal() * 13;
            g.drawString(font, stat.displayName(), INFO_X + 10, y, LABEL, false);
            int effective = selected.effectiveStat(stat);
            String value = selected.gearBonus(stat) == 0 ? Integer.toString(effective)
                    : effective + " (" + selected.stat(stat) + "+" + selected.gearBonus(stat) + ")";
            g.drawString(font, value, INFO_X + INFO_W - 47 - font.width(value), y, VALUE, false);
            int bx = INFO_X + INFO_W - 23, by = y - 2;
            button(g, bx, by, 14, 12, Component.literal("+"), selected.unspentPoints() > 0,
                    inside(mx, my, bx, by, 14, 12));
        }
        g.drawString(font, Component.translatable("screen.projectzero.companions.points", selected.unspentPoints()),
                INFO_X + 10, INFO_Y + 135, HIGHLIGHT, false);
        boolean hasSpent = selected.stats().values().stream().mapToInt(v -> Math.max(0, v - CompanionRules.BASE_STAT)).sum() > 0;
        boolean hasCrystal = hasBondCrystal();
        button(g, INFO_X + INFO_W - 113, INFO_Y + 132, 103, 15,
                Component.translatable("screen.projectzero.companions.bond_crystal"), hasCrystal && hasSpent,
                inside(mx, my, INFO_X + INFO_W - 113, INFO_Y + 132, 103, 15));
        g.drawString(font, Component.translatable("screen.projectzero.companions.abilities"),
                INFO_X + 10, INFO_Y + 153, LABEL, false);
        g.drawString(font, abilityText(selected), INFO_X + 68, INFO_Y + 153, VALUE, false);
        g.drawString(font, Component.translatable("screen.projectzero.companions.gear"),
                INFO_X + 10, INFO_Y + 171, LABEL, false);
        int gearX = INFO_X + 65;
        for (int slot = 0; slot < CompanionRules.GEAR_SLOTS.length; slot++) {
            String slotName = CompanionRules.GEAR_SLOTS[slot];
            int x = gearX + slot * 46;
            int boxY = INFO_Y + 182;
            ItemStack stack = selected.gear().getOrDefault(slotName, ItemStack.EMPTY);
            g.fill(x, boxY, x + 43, boxY + 24, PANEL);
            g.renderOutline(x, boxY, 43, 24, BORDER_DIM);
            g.drawCenteredString(font, Component.translatable("screen.projectzero.companions.slot." + slotName),
                    x + 21, boxY + 2, MUTED);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 13, boxY + 8);
                g.renderItemDecorations(font, stack, x + 13, boxY + 8);
            } else {
                g.drawCenteredString(font, Component.translatable("screen.projectzero.companions.gear_empty"),
                        x + 21, boxY + 14, MUTED);
            }
        }
    }

    private Component abilityText(CompanionEntry entry) {
        return entry.name().equals("Spirit Hound")
                ? Component.translatable("companion.projectzero.ability.pack_howl")
                : Component.translatable("screen.projectzero.companions.none");
    }

    private Component displayName(CompanionEntry entry) {
        return entry.name().equals("Spirit Hound") ? Component.translatable("companion.projectzero.spirit_hound")
                : Component.literal(entry.name());
    }

    private boolean hasBondCrystal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.is(ModItems.BOND_CRYSTAL.get())) return true;
        }
        return false;
    }

    private boolean inside(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component text, boolean active, boolean hover) {
        int border = !active ? BORDER_DIM : hover ? VALUE : BORDER;
        g.fill(x, y, x + w, y + h, hover && active ? 0x603FD0FF : PANEL);
        g.renderOutline(x, y, w, h, border);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, active ? (hover ? VALUE : LABEL) : MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        double mx = lx(mouseX), my = ly(mouseY);
        if (inside(mx, my, 10, 7, 42, 14)) {
            click();
            minecraft.setScreen(new StatusScreen());
            return true;
        }
        List<CompanionEntry> entries = ClientCompanionsCache.get().entries().stream().toList();
        int index = (int) ((my - (LIST_Y + 22)) / ROW_H) + rosterScroll;
        if (inside(mx, my, LIST_X + 3, LIST_Y + 22, LIST_W - 6, ROWS * ROW_H)
                && index >= rosterScroll && index < Math.min(entries.size(), rosterScroll + ROWS)) {
            selectedId = entries.get(index).id();
            click();
            return true;
        }
        CompanionEntry selected = entries.stream().filter(entry -> entry.id().equals(selectedId)).findFirst().orElse(null);
        if (selected == null) return super.mouseClicked(mouseX, mouseY, button);
        if (inside(mx, my, INFO_X + INFO_W - 113, INFO_Y + 132, 103, 15) && hasBondCrystal()) {
            click();
            ModNetwork.sendToServer(new UseBondCrystalPacket(selected.id()));
            return true;
        }
        for (Stat stat : Stat.values()) {
            int y = INFO_Y + 64 + stat.ordinal() * 13 - 2;
            if (inside(mx, my, INFO_X + INFO_W - 23, y, 14, 12) && selected.unspentPoints() > 0) {
                click();
                ModNetwork.sendToServer(new SpendCompanionPointPacket(selected.id(), stat.ordinal()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = lx(mouseX), my = ly(mouseY);
        if (inside(mx, my, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            int size = ClientCompanionsCache.get().entries().size();
            rosterScroll = Math.max(0, Math.min(Math.max(0, size - ROWS), rosterScroll + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.OPEN_STATUS.matches(keyCode, scanCode)) {
            minecraft.setScreen(new StatusScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
