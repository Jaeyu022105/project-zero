package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.skills.PlayerSkills;
import com.projectzero.skills.Skill;
import com.projectzero.skills.SkillRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/**
 * Placeholder bottom-left HUD showing plain text {@code HP x/y  MP x/y} and active skill slot
 * cooldowns.
 * TODO(claude-art): replace plain-text placeholder HUD with styled System HP/MP bars & skill slot icons.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkillHudOverlay {
    private SkillHudOverlay() {}

    private static boolean installed;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.isPaused()) {
            ClientSkillsCache.tickClient();
        }
        if (installed) return;
        Gui gui = mc.gui;
        if (gui == null) return;
        installed = true;
        try {
            for (Field f : Gui.class.getDeclaredFields()) {
                if (f.getType() == LayeredDraw.class) {
                    f.setAccessible(true);
                    ((LayeredDraw) f.get(gui)).add(SkillHudOverlay::render);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ProjectZero.LOGGER.warn("Skill HUD: could not attach to Gui LayeredDraw", e);
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || VisionOverlayState.coverage() > 0.5F) return;

        int h = g.guiHeight();
        int hp = Math.round(player.getHealth());
        int maxHp = Math.round(player.getMaxHealth());
        int mp = ClientSkillsCache.currentMana();
        int maxMp = ClientSkillsCache.maxMana();

        String vitals = "HP " + hp + "/" + maxHp + "  MP " + mp + "/" + maxMp;
        g.drawString(mc.font, vitals, 8, h - 24, 0xFF7FE8FF, true);

        PlayerSkills skills = ClientSkillsCache.get();
        StringBuilder slotsLine = new StringBuilder();
        for (int i = 0; i < SkillRules.ACTIVE_SLOTS; i++) {
            if (i > 0) slotsLine.append("  ");
            String keyLabel = ModKeyBindings.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase();
            slotsLine.append('[').append(keyLabel).append("] ");
            ResourceLocation id = skills.slot(i);
            if (id == null) {
                slotsLine.append('-');
            } else {
                String name = Skill.displayName(id).getString();
                int cd = skills.cooldownRemaining(id);
                if (cd > 0) {
                    slotsLine.append(name).append(String.format(" (%.1fs)", cd / 20.0));
                } else {
                    slotsLine.append(name);
                }
            }
        }
        g.drawString(mc.font, slotsLine.toString(), 8, h - 12, 0xFFE0E0E0, true);
    }
}
