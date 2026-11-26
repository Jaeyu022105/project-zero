package com.projectzero.client;

import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import com.projectzero.leveling.PlayerStats;
import com.projectzero.leveling.Stat;
import com.projectzero.network.AllocateStatPacket;
import com.projectzero.network.CycleTitlePacket;
import com.projectzero.network.ModNetwork;
import com.projectzero.skills.PlayerSkills;
import com.projectzero.skills.Skill;
import com.projectzero.titles.PlayerTitles;
import com.projectzero.titles.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The "System" status window. Opens with K by default. */
public class StatusScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 268;

    private static final int PANEL = 0xE00A1220;
    private static final int BORDER = 0xFF3FD0FF;
    private static final int GLOW = 0x553FD0FF;
    private static final int DIVIDER = 0x803FD0FF;
    private static final int LABEL = 0xFF7FE8FF;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8090A0;
    private static final int HIGHLIGHT = 0xFFFFD34D;
    private static final int XP_BG = 0xFF1A2A3A;
    private static final int XP_FILL = 0xFF3FD0FF;

    private static final int STAT_TOP = 128;
    private static final int STAT_ROW = 16;

    private final Map<Stat, Button> plusButtons = new EnumMap<>(Stat.class);
    private int left;
    private int top;

    public StatusScreen() {
        super(Component.translatable("screen.projectzero.status"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        plusButtons.clear();

        for (Stat stat : Stat.values()) {
            int y = top + STAT_TOP + stat.ordinal() * STAT_ROW - 3;
            Button button = Button.builder(Component.literal("+"),
                            b -> ModNetwork.sendToServer(new AllocateStatPacket(stat)))
                    .bounds(left + WIDTH - 34, y, 14, 14)
                    .build();
            plusButtons.put(stat, addRenderableWidget(button));
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        boolean hasPoints = ClientStatsCache.get().unspentPoints() > 0;
        for (Button button : plusButtons.values()) {
            button.active = hasPoints;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // super.render draws the background (including our panel, see renderBackground) and the buttons.
        super.render(g, mouseX, mouseY, partialTick);
        renderContents(g, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        renderPanel(g);
    }

    private void renderPanel(GuiGraphics g) {
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, GLOW);
        g.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        g.renderOutline(left, top, WIDTH, HEIGHT, BORDER);
    }

    private void renderContents(GuiGraphics g, int mouseX, int mouseY) {
        PlayerStats s = ClientStatsCache.get();
        Player player = Minecraft.getInstance().player;
        String name = player != null ? player.getGameProfile().getName() : "?";

        // Header
        g.drawCenteredString(font, Component.literal("STATUS"), left + WIDTH / 2, top + 10, LABEL);
        divider(g, top + 24);

        // Identity
        int col1 = left + 14;
        int col2 = left + WIDTH / 2 + 6;
        field(g, "NAME", name, col1, top + 32);
        field(g, "LEVEL", String.valueOf(s.level()), col2, top + 32);
        boolean jobHovered = jobField(g, col1, top + 46, mouseX, mouseY);
        boolean titleHovered = titleField(g, col2, top + 46, mouseX, mouseY);

        // HP / MP
        if (player != null) {
            field(g, "HP", Math.round(player.getHealth()) + " / " + Math.round(player.getMaxHealth()), col1, top + 62);
        }
        field(g, "MP", ClientSkillsCache.currentMana() + " / " + ClientSkillsCache.maxMana(), col2, top + 62);

        // XP bar
        int barX = left + 14;
        int barY = top + 82;
        int barW = WIDTH - 28;
        g.fill(barX, barY, barX + barW, barY + 6, XP_BG);
        float progress = s.isMaxLevel() ? 1.0F : (float) s.xp() / s.xpToNext();
        g.fill(barX, barY, barX + Math.round(barW * Math.min(1.0F, progress)), barY + 6, XP_FILL);
        String xpText = s.isMaxLevel() ? "MAX LEVEL" : "XP " + s.xp() + " / " + s.xpToNext();
        g.drawCenteredString(font, xpText, left + WIDTH / 2, barY + 10, MUTED);

        divider(g, top + 116);

        // Stats
        Component hovered = null;
        for (Stat stat : Stat.values()) {
            int y = top + STAT_TOP + stat.ordinal() * STAT_ROW;
            g.drawString(font, stat.displayName().getString().toUpperCase(), col1, y, LABEL, false);
            g.drawString(font, String.valueOf(s.get(stat)), left + WIDTH - 70, y, VALUE, false);
            if (mouseX >= col1 && mouseX < left + WIDTH - 40 && mouseY >= y - 3 && mouseY < y + STAT_ROW - 3) {
                hovered = stat.description();
            }
        }

        // Skills (placeholder plain list; TODO(claude-art): styled skills panel/icons)
        divider(g, top + 212);
        PlayerSkills skills = ClientSkillsCache.get();
        g.drawString(font, "SKILLS: ", col1, top + 220, LABEL, false);
        if (skills.learned().isEmpty()) {
            g.drawString(font, "None", col1 + font.width("SKILLS: "), top + 220, MUTED, false);
        } else {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (ResourceLocation id : skills.learned()) {
                if (shown > 0) sb.append(", ");
                int slot = skills.slotOf(id);
                if (slot >= 0 && slot < ModKeyBindings.SKILL_KEYS.length) {
                    sb.append('[').append(ModKeyBindings.SKILL_KEYS[slot].getTranslatedKeyMessage().getString().toUpperCase()).append("] ");
                }
                sb.append(Skill.displayName(id).getString());
                shown++;
                if (shown >= 4 && skills.learned().size() > 4) {
                    sb.append(" +").append(skills.learned().size() - 4);
                    break;
                }
            }
            String line = font.plainSubstrByWidth(sb.toString(), WIDTH - 28);
            g.drawString(font, line, col1, top + 232, VALUE, false);
        }

        // Points
        int pointsY = top + HEIGHT - 16;
        int pointColor = s.unspentPoints() > 0 ? HIGHLIGHT : MUTED;
        g.drawString(font, "Available Ability Points: ", col1, pointsY, LABEL, false);
        g.drawString(font, String.valueOf(s.unspentPoints()),
                col1 + font.width("Available Ability Points: "), pointsY, pointColor, false);

        if (jobHovered) {
            JobRarity rarity = ClientJobCache.rarity();
            hovered = Component.translatable("projectzero.job.tooltip",
                    rarity != null ? rarity.displayName() : Component.literal("?"),
                    ClientJobCache.get().jobLevel());
        }
        if (titleHovered) {
            PlayerTitles titles = ClientTitlesCache.get();
            List<Component> lines = new ArrayList<>();
            ResourceLocation eq = titles.equipped();
            if (eq != null) {
                lines.add(Title.displayName(eq).copy().withStyle(ChatFormatting.GOLD));
                lines.add(Title.description(eq).copy().withStyle(ChatFormatting.WHITE));
            }
            lines.add(Component.translatable("projectzero.title.cycle_hint", titles.unlocked().size())
                    .withStyle(ChatFormatting.GRAY));
            g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        } else if (hovered != null) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    /** TITLE: equipped title name (clickable to cycle). Returns true if hovered. */
    private boolean titleField(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        PlayerTitles titles = ClientTitlesCache.get();
        g.drawString(font, "TITLE: ", x, y, LABEL, false);
        int valueX = x + font.width("TITLE: ");
        int maxW = (left + WIDTH - 12) - valueX;
        ResourceLocation eq = titles.equipped();
        String text = eq != null ? Title.displayName(eq).getString() : "None";
        text = font.plainSubstrByWidth(text, Math.max(20, maxW));
        int color = eq != null ? HIGHLIGHT : (titles.unlocked().isEmpty() ? MUTED : VALUE);
        g.drawString(font, text, valueX, y, color, false);
        int hitEnd = Math.max(valueX + font.width(text), x + 76);
        return mouseX >= x && mouseX < hitEnd && mouseY >= y - 2 && mouseY < y + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int col2 = left + WIDTH / 2 + 6;
            int y = top + 46;
            if (mouseX >= col2 && mouseX < left + WIDTH - 12 && mouseY >= y - 2 && mouseY < y + 10) {
                if (!ClientTitlesCache.get().unlocked().isEmpty()) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    ModNetwork.sendToServer(new CycleTitlePacket());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** JOB: name in its rarity color. Returns true if the mouse is over the job name. */
    private boolean jobField(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        PlayerJob job = ClientJobCache.get();
        g.drawString(font, "JOB: ", x, y, LABEL, false);
        int valueX = x + font.width("JOB: ");
        if (!job.hasJob()) {
            g.drawString(font, Component.translatable("projectzero.job.none"), valueX, y, VALUE, false);
            return false;
        }
        JobRarity rarity = ClientJobCache.rarity();
        Component name = Job.displayName(job.jobId());
        g.drawString(font, name, valueX, y, rarity != null ? rarity.argb() : MUTED, false);
        return mouseX >= valueX && mouseX < valueX + font.width(name) && mouseY >= y - 1 && mouseY < y + 9;
    }

    private void field(GuiGraphics g, String label, String value, int x, int y) {
        g.drawString(font, label + ": ", x, y, LABEL, false);
        g.drawString(font, value, x + font.width(label + ": "), y, VALUE, false);
    }

    private void divider(GuiGraphics g, int y) {
        g.fill(left + 10, y, left + WIDTH - 10, y + 1, DIVIDER);
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
