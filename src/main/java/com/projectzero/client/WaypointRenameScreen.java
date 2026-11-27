package com.projectzero.client;

import com.projectzero.network.ModNetwork;
import com.projectzero.network.RenameWaypointPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class WaypointRenameScreen extends Screen {
    private static final int PANEL = 0xE00A1220, BORDER = 0xFF3FD0FF, LABEL = 0xFF7FE8FF;
    private final ResourceLocation dimension;
    private final long pos;
    private final String currentName;
    private EditBox name;
    private int left, top, panelW;

    public WaypointRenameScreen(ResourceLocation dimension, long pos, String currentName) {
        super(Component.translatable("screen.projectzero.waypoints.rename_title"));
        this.dimension = dimension; this.pos = pos; this.currentName = currentName;
    }

    @Override protected void init() {
        panelW = Math.min(290, width - 24);
        left = (width - panelW) / 2; top = height / 2 - 48;
        name = new EditBox(font, left + 14, top + 34, panelW - 28, 20, Component.translatable("screen.projectzero.waypoints.name"));
        name.setMaxLength(40); name.setValue(currentName); addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("screen.projectzero.waypoints.save"), b -> save())
                .bounds(left + panelW / 2 - 45, top + 64, 90, 20).build());
        setInitialFocus(name);
    }

    private void save() {
        ModNetwork.sendToServer(new RenameWaypointPacket(dimension, pos, name.getValue()));
        onClose();
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + panelW + 2, top + 96, 0x553FD0FF);
        g.fill(left, top, left + panelW, top + 94, PANEL);
        g.renderOutline(left, top, panelW, 94, BORDER);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.getString().toUpperCase(), left + panelW / 2, top + 10, LABEL);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        if (keyCode == 257 || keyCode == 335) { save(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
