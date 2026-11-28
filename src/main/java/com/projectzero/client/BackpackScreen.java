package com.projectzero.client;

import com.projectzero.equipment.BackpackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** System-styled backpack rows above the player's inventory. */
public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int PANEL = 0xE60A1220, BORDER = 0xFF3FD0FF, GLOW = 0x553FD0FF;
    private static final int SLOT_BG = 0xFF142236, SLOT_BORDER = 0x883FD0FF, DISABLED = 0x88203040;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 176;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = 87;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, GLOW);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, BORDER);
        graphics.fill(leftPos + 8, topPos + 19, leftPos + imageWidth - 8, topPos + 20, 0x803FD0FF);
        graphics.fill(leftPos + 8, topPos + 91, leftPos + imageWidth - 8, topPos + 92, 0x803FD0FF);
        for (int i = 0; i < 27; i++) {
            int x = leftPos + 7 + (i % 9) * 18;
            int y = topPos + 23 + (i / 9) * 18;
            graphics.fill(x, y, x + 18, y + 18, SLOT_BG);
            graphics.renderOutline(x, y, 18, 18, SLOT_BORDER);
            if (i >= menu.capacity()) graphics.fill(x, y, x + 18, y + 18, DISABLED);
        }
        graphics.drawString(font, Component.translatable("screen.projectzero.backpack.capacity", menu.capacity()),
                leftPos + imageWidth - 8 - font.width(Component.translatable("screen.projectzero.backpack.capacity", menu.capacity())),
                topPos + 6, 0xFF7FE8FF, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF7FE8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF8090A0, false);
    }
}
