package com.projectzero.skills;

import com.projectzero.ProjectZero;
import com.projectzero.domain.LibraryDomain;
import com.projectzero.domain.ReawakeningVision;
import com.projectzero.jobs.JobRules;
import com.projectzero.leveling.LevelingService;
import com.projectzero.leveling.StatEffects;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncSkillsPacket;
import com.projectzero.ui.NoticeRules;
import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side API for player skills, mana, slots, and cooldowns (DESIGN-DOCUMENT.md §4).
 */
public final class SkillService {
    private SkillService() {}

    public static final String DATA_KEY = ProjectZero.MOD_ID + ":skills";

    public static int maxMana(Player player) {
        return StatEffects.maxMana(player, LevelingService.get(player));
    }

    public static PlayerSkills get(Player player) {
        PlayerSkills data = PlayerSkills.load(player.getPersistentData().getCompound(DATA_KEY));
        data.ensureInitialized(maxMana(player));
        return data;
    }

    public static void set(ServerPlayer player, PlayerSkills skills) {
        skills.ensureInitialized(maxMana(player));
        player.getPersistentData().put(DATA_KEY, skills.save());
        sync(player, skills);
    }

    /** Saves without sending a network packet (used by per-tick cooldown/regen updates; synced once/sec or on change). */
    private static void saveQuiet(ServerPlayer player, PlayerSkills skills) {
        player.getPersistentData().put(DATA_KEY, skills.save());
    }

    public static void sync(ServerPlayer player) {
        sync(player, get(player));
    }

    private static void sync(ServerPlayer player, PlayerSkills skills) {
        int max = maxMana(player);
        skills.ensureInitialized(max);
        Map<ResourceLocation, SyncSkillsPacket.SkillMeta> meta = new HashMap<>();
        for (ResourceLocation id : skills.learned()) {
            Skill def = SkillRegistry.get(id);
            if (def != null) {
                meta.put(id, new SyncSkillsPacket.SkillMeta(def.type() == SkillType.ACTIVE, def.manaCost()));
            }
        }
        ModNetwork.sendToPlayer(player, new SyncSkillsPacket(skills, max, meta));
    }

    /** Teaches a skill to the player (auto-slotting into a free keybind slot if active). */
    public static boolean learn(ServerPlayer player, Skill skill, boolean notify) {
        PlayerSkills data = get(player);
        if (!data.addSkill(skill.id(), skill.type() == SkillType.ACTIVE)) {
            return false;
        }
        set(player, data);
        if (notify) {
            Component chat = Component.literal("Skill learned: ").withStyle(ChatFormatting.WHITE)
                    .append(skill.displayName().copy().withStyle(ChatFormatting.AQUA));
            SystemNotices.system(player, NoticeType.SKILL, NoticeType.SKILL.defaultTitle(),
                    skill.displayName().copy().withStyle(ChatFormatting.AQUA), chat);
        }
        return true;
    }

    /** Auto-grants any basic skills unlocked up to {@code playerLevel}. */
    public static void grantBasicSkillsForLevel(ServerPlayer player, int playerLevel, boolean notify) {
        for (Skill skill : SkillRegistry.basicSkillsForLevel(playerLevel)) {
            learn(player, skill, notify);
        }
    }

    /** Auto-grants any job skills unlocked for {@code jobId} up to {@code jobLevel}. */
    public static void grantJobSkills(ServerPlayer player, ResourceLocation jobId, int jobLevel, boolean notify) {
        for (Skill skill : SkillRegistry.jobSkillsFor(jobId, jobLevel)) {
            learn(player, skill, notify);
        }
    }

    public static boolean forget(ServerPlayer player, ResourceLocation skillId) {
        PlayerSkills data = get(player);
        if (!data.removeSkill(skillId)) return false;
        set(player, data);
        return true;
    }

    public static boolean assignSlot(ServerPlayer player, int slotIndex, ResourceLocation skillId) {
        return assignSlot(player, get(player).activePreset(), slotIndex, skillId);
    }

    /** Puts a learned active skill (or nothing) in a slot of any preset. Sent from the Skills window. */
    public static boolean assignSlot(ServerPlayer player, int preset, int slotIndex, ResourceLocation skillId) {
        if (slotIndex < 0 || slotIndex >= SkillRules.ACTIVE_SLOTS) return false;
        if (preset < 0 || preset >= SkillRules.PRESETS) return false;
        PlayerSkills data = get(player);
        if (skillId != null) {
            if (!data.hasLearned(skillId)) return false;
            Skill def = SkillRegistry.get(skillId);
            if (def == null || def.type() != SkillType.ACTIVE) return false;
        }
        data.setSlot(preset, slotIndex, skillId);
        set(player, data);
        return true;
    }

    /** Game time until which a player who just swapped presets cannot cast. Not saved. */
    private static final java.util.Map<java.util.UUID, Long> SWAP_LOCK = new java.util.HashMap<>();

    /** Makes {@code preset} live ({@code -1} = next). Casting is locked briefly after a swap. */
    public static void switchPreset(ServerPlayer player, int preset) {
        PlayerSkills data = get(player);
        int target = preset < 0 ? (data.activePreset() + 1) % SkillRules.PRESETS : preset;
        if (target >= SkillRules.PRESETS || target == data.activePreset()) return;
        data.setActivePreset(target);
        set(player, data);
        SWAP_LOCK.put(player.getUUID(), player.level().getGameTime() + SkillRules.PRESET_SWAP_LOCK_TICKS);
        player.displayClientMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Preset " + roman(target + 1)).withStyle(ChatFormatting.WHITE)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    public static String roman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n); };
    }

    public static void setMana(ServerPlayer player, double amount) {
        PlayerSkills data = get(player);
        data.setMana(amount, maxMana(player));
        set(player, data);
    }

    /** Attempts to cast the active skill in {@code slotIndex} (0..3). Server-authoritative. */
    public static boolean castSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SkillRules.ACTIVE_SLOTS) return false;
        if (LibraryDomain.isInDomain(player) || ReawakeningVision.isInVision(player)) return false;

        Long lock = SWAP_LOCK.get(player.getUUID());
        if (lock != null) {
            if (player.level().getGameTime() < lock) return false;
            SWAP_LOCK.remove(player.getUUID());
        }

        PlayerSkills data = get(player);
        ResourceLocation skillId = data.slot(slotIndex);
        if (skillId == null || !data.hasLearned(skillId)) return false;

        Skill skill = SkillRegistry.get(skillId);
        if (skill == null || skill.type() != SkillType.ACTIVE) return false;

        int cd = data.cooldownRemaining(skillId);
        if (cd > 0) {
            player.displayClientMessage(Component.literal("Cooldown: " + String.format("%.1fs", cd / 20.0))
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }

        if (data.currentMana() < skill.manaCost()) {
            player.displayClientMessage(Component.literal("Not enough mana (" + skill.manaCost() + " MP)")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        SkillBehavior behavior = SkillBehaviors.get(skill.behavior());
        if (behavior == null) {
            ProjectZero.LOGGER.warn("Missing SkillBehavior {} for skill {}", skill.behavior(), skill.id());
            return false;
        }

        boolean cast;
        com.projectzero.combat.BossAffinity.begin(player, skill.behavior());
        try {
            cast = behavior.execute(player, skill, skill.params());
        } finally {
            com.projectzero.combat.BossAffinity.end();
        }
        if (!cast) {
            return false;
        }
        com.projectzero.anim.PlayerAnim anim = com.projectzero.anim.PlayerAnim.forBehavior(skill.behavior());
        if (anim != null) {
            com.projectzero.network.ModNetwork.sendToTrackingAndSelf(player, new com.projectzero.network.SkillVisualPacket(
                    player.getId(), com.projectzero.client.ClientSkillVisuals.Kind.ANIM.ordinal(), anim.ordinal(), anim.ticks));
        }

        int max = maxMana(player);
        data.setMana(data.rawMana() - skill.manaCost(), max);
        if (skill.cooldownTicks() > 0) {
            data.setCooldown(skillId, skill.cooldownTicks());
        }
        set(player, data);
        return true;
    }

    /** Called each server tick on the player to decrement cooldowns and regenerate mana once per second. */
    public static void tickPlayer(ServerPlayer player) {
        PlayerSkills data = get(player);
        List<ResourceLocation> readySkills = data.tickCooldownsReady();
        boolean cdFinished = !readySkills.isEmpty();
        boolean changed = cdFinished || !data.cooldownsView().isEmpty();

        for (ResourceLocation readyId : readySkills) {
            if (data.slotOf(readyId) >= 0) {
                Skill def = SkillRegistry.get(readyId);
                if (def != null && def.cooldownTicks() >= NoticeRules.COOLDOWN_NOTICE_MIN_TICKS) {
                    SystemNotices.send(player, NoticeType.COOLDOWN, NoticeType.COOLDOWN.defaultTitle(),
                            def.displayName().copy().withStyle(ChatFormatting.AQUA));
                }
            }
        }

        boolean syncNow = cdFinished;
        if (player.tickCount % 20 == 0) {
            int max = maxMana(player);
            int beforeInt = data.currentMana();
            if (data.rawMana() < max) {
                double regen = SkillRules.manaRegenPerSecond(max)
                        + com.projectzero.equipment.EquipmentService.manaRegenBonus(player);
                data.setMana(data.rawMana() + regen, max);
                changed = true;
                if (data.currentMana() != beforeInt) syncNow = true;
            }
            for (ResourceLocation id : data.learned()) {
                Skill s = SkillRegistry.get(id);
                if (s != null && s.type() == SkillType.PASSIVE) {
                    SkillBehavior b = SkillBehaviors.get(s.behavior());
                    if (b != null) b.onPassiveSecond(player, s, s.params());
                }
            }
        }

        if (changed) {
            if (syncNow) {
                set(player, data);
            } else {
                saveQuiet(player, data);
            }
        }
    }

    /**
     * Job reroll hook (DESIGN-DOCUMENT.md §6): keeps up to {@link JobRules#REROLL_KEPT_JOB_SKILLS}
     * random learned skills belonging to {@code previousJobId} and removes the remaining skills of that job.
     */
    public static void retainRandomJobSkillsOnReroll(ServerPlayer player, ResourceLocation previousJobId) {
        PlayerSkills data = get(player);
        List<ResourceLocation> oldJobSkills = new ArrayList<>();
        for (ResourceLocation id : data.learned()) {
            Skill def = SkillRegistry.get(id);
            if (def != null && def.source() == SkillSource.JOB && def.job().filter(previousJobId::equals).isPresent()) {
                oldJobSkills.add(id);
            }
        }
        if (oldJobSkills.size() <= JobRules.REROLL_KEPT_JOB_SKILLS) return;

        RandomSource random = player.getRandom();
        for (int i = oldJobSkills.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ResourceLocation tmp = oldJobSkills.get(i);
            oldJobSkills.set(i, oldJobSkills.get(j));
            oldJobSkills.set(j, tmp);
        }
        for (int i = JobRules.REROLL_KEPT_JOB_SKILLS; i < oldJobSkills.size(); i++) {
            data.removeSkill(oldJobSkills.get(i));
        }
        set(player, data);
    }

    public static void reset(ServerPlayer player) {
        PlayerSkills fresh = new PlayerSkills();
        fresh.ensureInitialized(maxMana(player));
        set(player, fresh);
    }

    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
