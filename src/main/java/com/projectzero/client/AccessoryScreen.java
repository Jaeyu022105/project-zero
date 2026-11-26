package com.projectzero.client;

import com.projectzero.equipment.AccessoryMenu;
import com.projectzero.equipment.AccessorySlotType;
import com.projectzero.equipment.PlayerAccessories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Placeholder System-styled container screen for the 8 accessory slots + player inventory
 * (TODO(claude-art): styled System equipment frame and slot silhouette icons).
 */
public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    private static final int PANEL = 0xE60A1220;
    private static final int BORDER = 0xFF3FD0FF;
    private static final int SLOT_BG = 0xFF142236;
    private static final int SLOT_BORDER = 0x883FD0FF;
    private static final int LOCKED_BG = 0xFF1C1824;
    private static final int LOCKED_BORDER = 0x88605070;
    private static final int LABEL = 0xFF7FE8FF;
    private static final int MUTED = 0xFF607488;

    public AccessoryScreen(AccessoryMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // Tooltip hint when hovering an empty accessory slot
        for (int i = 0; i < PlayerAccessories.TOTAL_SLOTS; i++) {
            Slot s = menu.slots.get(i);
            if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mouseX, mouseY)) {
                AccessorySlotType type = PlayerAccessories.SLOT_LAYOUT[i];
                Component tip = i == 8
                        ? Component.translatable("slot.projectzero.relic_locked")
                        : type.displayName();
                g.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, BORDER);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            int sx = leftPos + s.x - 1;
            int sy = topPos + s.y - 1;
            boolean locked = (i == 8);
            g.fill(sx, sy, sx + 18, sy + 18, locked ? LOCKED_BG : SLOT_BG);
            g.renderOutline(sx, sy, 18, 18, locked ? LOCKED_BORDER : SLOT_BORDER);

            if (i < PlayerAccessories.TOTAL_SLOTS && !s.hasItem()) {
                String shortLbl = PlayerAccessories.SLOT_LAYOUT[i].shortLabel();
                g.drawCenteredString(font, shortLbl, sx + 9, sy + 5, locked ? 0xFF705880 : MUTED);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, title, imageWidth / 2, titleLabelY, LABEL);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
