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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import com.projectzero.leveling.PlayerStats;
import com.projectzero.network.SyncSkillsPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * The System HUD, bottom left: four skill slots (icon, key, cooldown sweep, out-of-mana tint)
 * above a small panel with level, job, HP, MP and XP bars. Follows the System look
 * ({@code .agents/skills/system-ui-style}). Hidden with F1 and during the Reawakening vision.
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

    // ---- palette (system-ui-style) ----
    private static final int PANEL = 0xD00A1220;
    private static final int GLOW = 0x403FD0FF;
    private static final int BORDER = 0xFF3FD0FF;
    private static final int BORDER_DIM = 0x803FD0FF;
    private static final int LABEL = 0xFF7FE8FF;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8090A0;
    private static final int BAR_BG = 0xFF1A2A3A;
    private static final int HP_FILL = 0xFFE0404A;
    private static final int HP_HI = 0xFFFF7A80;
    private static final int MP_FILL = 0xFF3A7CFF;
    private static final int MP_HI = 0xFF8AB8FF;
    private static final int XP_FILL = 0xFF3FD0FF;
    private static final int LAG = 0xFFFFE0A0;

    private static final int SLOT = 20;
    private static final int PANEL_W = 104;
    private static final int PANEL_H = 44;

    /** Bars ease toward their value; the HP bar leaves a pale "lag" trail when you take damage. */
    private static float hpShown = 1F, hpLag = 1F, mpShown = 1F, xpShown = 0F;
    /** Longest cooldown seen per skill since it started, for the sweep. */
    private static final Map<ResourceLocation, Integer> COOLDOWN_TOTAL = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> ICON_EXISTS = new HashMap<>();

    private static int shownPreset = -1;
    private static long swapAt;

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || VisionOverlayState.coverage() > 0.5F) return;
        if (mc.player.isSpectator()) return;

        int h = g.guiHeight();
        int x = 6;
        int panelY = h - 6 - PANEL_H;
        int slotY = panelY - 5 - SLOT;

        PlayerSkills skills = ClientSkillsCache.get();
        // After a preset swap the slots drop in from above and the preset tag flashes.
        int preset = skills.activePreset();
        if (preset != shownPreset) {
            if (shownPreset >= 0) swapAt = System.currentTimeMillis();
            shownPreset = preset;
        }
        float swap = Mth.clamp((System.currentTimeMillis() - swapAt) / 350F, 0F, 1F);
        float ease = 1F - (1F - swap) * (1F - swap);
        for (int i = 0; i < SkillRules.ACTIVE_SLOTS; i++) {
            float d = Mth.clamp(ease * 1.4F - i * 0.1F, 0F, 1F);
            g.pose().pushPose();
            g.pose().translate(0, (1F - d) * -10F, 0);
            drawSlot(g, mc, x + i * (SLOT + 4), slotY, i, skills);
            g.pose().popPose();
        }
        String tag = com.projectzero.skills.SkillService.roman(preset + 1);
        int tx = x + SkillRules.ACTIVE_SLOTS * (SLOT + 4) + 1;
        g.drawString(mc.font, tag, tx, slotY + (SLOT - 8) / 2, swap < 1F ? 0xFFFFD34D : LABEL, false);

        frame(g, x, panelY, PANEL_W, PANEL_H, BORDER);

        // Level + job
        PlayerStats stats = ClientStatsCache.get();
        g.drawString(mc.font, "LV", x + 4, panelY + 4, LABEL, false);
        g.drawString(mc.font, String.valueOf(stats.level()), x + 17, panelY + 4, VALUE, false);
        PlayerJob job = ClientJobCache.get();
        if (job.hasJob()) {
            JobRarity rarity = ClientJobCache.rarity();
            int color = rarity != null ? rarity.argb() : VALUE;
            Component name = Job.displayName(job.jobId());
            int w = mc.font.width(name);
            g.drawString(mc.font, name, x + PANEL_W - 4 - w, panelY + 4, color, false);
        } else {
            String none = "NO JOB";
            g.drawString(mc.font, none, x + PANEL_W - 4 - mc.font.width(none), panelY + 4, MUTED, false);
        }

        // HP
        float hp = player.getHealth();
        float maxHp = Math.max(1F, player.getMaxHealth());
        float hpFrac = Mth.clamp(hp / maxHp, 0F, 1F);
        hpShown += (hpFrac - hpShown) * 0.35F;
        hpLag = hpFrac > hpLag ? hpFrac : hpLag + (hpFrac - hpLag) * 0.06F;
        barRow(g, mc, x + 4, panelY + 15, "HP", Math.round(hp), Math.round(maxHp), hpShown, hpLag, HP_FILL, HP_HI);

        // MP
        int mp = ClientSkillsCache.currentMana();
        int maxMp = Math.max(0, ClientSkillsCache.maxMana());
        float mpFrac = maxMp <= 0 ? 0F : Mth.clamp(mp / (float) maxMp, 0F, 1F);
        mpShown += (mpFrac - mpShown) * 0.35F;
        barRow(g, mc, x + 4, panelY + 27, "MP", mp, maxMp, mpShown, mpShown, MP_FILL, MP_HI);

        // XP, thin line along the bottom of the panel
        boolean atCeiling = stats.isAtCeiling(ClientJobCache.noCeiling());
        float xpFrac = (stats.isMaxLevel() || atCeiling) ? 1F : Mth.clamp(stats.xp() / (float) Math.max(1L, stats.xpToNext()), 0F, 1F);
        xpShown += (xpFrac - xpShown) * 0.2F;
        int xpY = panelY + PANEL_H - 4;
        g.fill(x + 4, xpY, x + PANEL_W - 4, xpY + 2, BAR_BG);
        g.fill(x + 4, xpY, x + 4 + Math.round((PANEL_W - 8) * xpShown), xpY + 2, XP_FILL);
    }

    private static void barRow(GuiGraphics g, Minecraft mc, int x, int y, String label, int value, int max,
                               float shown, float lag, int fill, int hi) {
        int w = PANEL_W - 8;
        g.drawString(mc.font, label, x, y, LABEL, false);
        String nums = value + " / " + max;
        g.pose().pushPose();
        g.pose().translate(x + w, y + 1, 0);
        g.pose().scale(0.75F, 0.75F, 1F);
        g.drawString(mc.font, nums, -mc.font.width(nums), 0, VALUE, false);
        g.pose().popPose();
        int by = y + 9;
        g.fill(x, by, x + w, by + 3, BAR_BG);
        if (lag > shown) g.fill(x, by, x + Math.round(w * lag), by + 3, LAG);
        int fw = Math.round(w * shown);
        g.fill(x, by, x + fw, by + 3, fill);
        g.fill(x, by, x + fw, by + 1, hi);
    }

    private static void drawSlot(GuiGraphics g, Minecraft mc, int x, int y, int index, PlayerSkills skills) {
        ResourceLocation id = skills.slot(index);
        frame(g, x, y, SLOT, SLOT, id == null ? BORDER_DIM : BORDER);

        if (id != null) {
            if (hasIcon(mc, id)) {
                g.blit(iconOf(id), x + 2, y + 2, 0, 0, 16, 16, 16, 16);
            } else {
                String initials = initials(Skill.displayName(id).getString());
                g.drawString(mc.font, initials, x + (SLOT - mc.font.width(initials)) / 2, y + 6, LABEL, false);
            }

            // Not enough mana: blue wash.
            SyncSkillsPacket.SkillMeta meta = ClientSkillsCache.meta(id);
            if (meta != null && meta.manaCost() > ClientSkillsCache.currentMana()) {
                g.fill(x + 2, y + 2, x + SLOT - 2, y + SLOT - 2, 0x902040A0);
            }

            // Cooldown: dark sweep shrinking from the top, seconds on top.
            int cd = skills.cooldownRemaining(id);
            if (cd > 0) {
                int total = Math.max(cd, COOLDOWN_TOTAL.getOrDefault(id, cd));
                COOLDOWN_TOTAL.put(id, total);
                int inner = SLOT - 4;
                int covered = Math.round(inner * (cd / (float) total));
                g.fill(x + 2, y + 2 + (inner - covered), x + SLOT - 2, y + SLOT - 2, 0xB0000000);
                String secs = cd >= 200 ? String.valueOf(cd / 20) : String.format("%.1f", cd / 20F);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(mc.font, secs, x + (SLOT - mc.font.width(secs)) / 2 + 1, y + 6, VALUE, true);
                g.pose().popPose();
            } else {
                COOLDOWN_TOTAL.remove(id);
            }
        }

        // Key label in the corner.
        String key = ModKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
        if (key.length() > 2) key = key.substring(0, 2);
        g.pose().pushPose();
        g.pose().translate(x + SLOT - 1, y + SLOT - 6, 300);
        g.pose().scale(0.6F, 0.6F, 1F);
        g.drawString(mc.font, key, -mc.font.width(key), 0, LABEL, true);
        g.pose().popPose();
    }

    /** Navy panel, 1px border, soft outer glow. */
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int border) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GLOW);
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    static ResourceLocation iconOf(ResourceLocation skill) {
        return ResourceLocation.fromNamespaceAndPath(skill.getNamespace(), "textures/gui/skills/" + skill.getPath() + ".png");
    }

    static boolean hasIcon(Minecraft mc, ResourceLocation skill) {
        return ICON_EXISTS.computeIfAbsent(skill, id -> mc.getResourceManager().getResource(iconOf(id)).isPresent());
    }

    static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String part : name.split(" ")) {
            if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0)));
            if (out.length() == 2) break;
        }
        return out.isEmpty() ? "?" : out.toString();
    }
}
