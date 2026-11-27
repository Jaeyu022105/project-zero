package com.projectzero.client;

import com.projectzero.network.ModNetwork;
import com.projectzero.network.WaypointTravelPacket;
import com.projectzero.waypoints.WaypointService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.List;

/** System-style list of attuned waypoints. */
public final class WaypointScreen extends Screen {
    private static final int PANEL = 0xE00A1220, DEEP = 0xF0101D2B, BORDER = 0xFF3FD0FF;
    private static final int LABEL = 0xFF7FE8FF, MUTED = 0xFF8090A0, GOLD = 0xFFFFD34D;
    private int left, top, panelW, panelH, scroll;

    public WaypointScreen() { super(Component.translatable("screen.projectzero.waypoints.title")); }

    @Override protected void init() {
        panelW = Math.min(330, width - 24);
        panelH = Math.min(230, height - 24);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        scroll = Math.min(scroll, Math.max(0, ClientWaypointCache.entries().size() - visibleRows()));
    }

    private int visibleRows() { return Math.max(1, (panelH - 54) / 27); }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + panelW + 2, top + panelH + 2, 0x553FD0FF);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
        g.renderOutline(left, top, panelW, panelH, BORDER);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.getString().toUpperCase(), left + panelW / 2, top + 10, LABEL);
        g.fill(left + 10, top + 25, left + panelW - 10, top + 26, 0x803FD0FF);
        List<WaypointService.Entry> entries = ClientWaypointCache.entries();
        if (entries.isEmpty()) g.drawCenteredString(font, Component.translatable("screen.projectzero.waypoints.empty"), left + panelW / 2, top + 49, MUTED);
        for (int i = 0; i < Math.min(visibleRows(), entries.size() - scroll); i++) {
            WaypointService.Entry entry = entries.get(i + scroll);
            int y = top + 33 + i * 27;
            boolean hover = mouseX >= left + 8 && mouseX <= left + panelW - 8 && mouseY >= y && mouseY < y + 24;
            g.fill(left + 8, y, left + panelW - 8, y + 24, hover ? 0x603FD0FF : DEEP);
            g.renderOutline(left + 8, y, panelW - 16, 24, entry.valid() ? (hover ? 0xFFFFFFFF : 0x803FD0FF) : 0xFF39434D);
            int color = entry.valid() ? 0xFFFFFFFF : MUTED;
            g.drawString(font, font.plainSubstrByWidth(entry.name(), panelW - 112), left + 14, y + 3, color, false);
            String dim = Component.translatable(net.minecraft.Util.makeDescriptionId("dimension", entry.dimension())).getString();
            String detail = Component.translatable("screen.projectzero.waypoints.dimension", dim).getString();
            if (entry.distance() >= 0) detail += " · " + Component.translatable("screen.projectzero.waypoints.distance",
                    Math.round(Math.sqrt(entry.distance()))).getString();
            g.drawString(font, font.plainSubstrByWidth(detail, panelW - 100), left + 14, y + 13, MUTED, false);
            int bx = left + panelW - 67;
            g.fill(bx, y + 3, bx + 50, y + 21, entry.valid() ? 0xFF0D2733 : DEEP);
            g.renderOutline(bx, y + 3, 50, 18, entry.valid() ? BORDER : 0xFF39434D);
            g.drawCenteredString(font, Component.translatable("screen.projectzero.waypoints.travel"), bx + 25, y + 8,
                    entry.valid() ? GOLD : MUTED);
        }
        g.drawCenteredString(font, Component.translatable("screen.projectzero.waypoints.close"), left + panelW / 2, top + panelH - 16, MUTED);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = (int) ((mouseY - top - 33) / 27);
            int bx = left + panelW - 67;
            if (row >= 0 && row < visibleRows() && mouseX >= bx && mouseX < bx + 50) {
                int index = row + scroll;
                List<WaypointService.Entry> entries = ClientWaypointCache.entries();
                if (index < entries.size() && entries.get(index).valid()) {
                    WaypointService.Entry entry = entries.get(index);
                    ModNetwork.sendToServer(new WaypointTravelPacket(entry.dimension(), entry.pos()));
                    onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(Math.max(0, ClientWaypointCache.entries().size() - visibleRows()), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
