package com.projectzero.gates;

import com.projectzero.ProjectZero;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side Forge events for mob scaling and Gate dungeons. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobEvents {
    private MobEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MobCommands.register(event.getDispatcher());
        GateCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
            GateService.onDungeonBossKilled(serverLevel, event.getEntity(), killer);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level instanceof ServerLevel serverLevel) {
            GateService.tickNaturalSpawning(serverLevel);
        }
    }

    /**
     * Catch-up hook when entity sections load in {@code projectzero:dungeon}:
     * discards stale monsters if a Dungeon Break occurred while unloaded, or rescales them
     * if the gate surged or ranked up while their cave chunk was unloaded.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(GateService.DUNGEON)) return;
        var entity = event.getEntity();
        if (!entity.getPersistentData().getBoolean(MobScaling.GATE_MONSTER_KEY)) return;
        if (!entity.getPersistentData().contains(GateDungeonArena.DUNGEON_SLOT_KEY)) return;
        int slot = entity.getPersistentData().getInt(GateDungeonArena.DUNGEON_SLOT_KEY);
        DungeonSavedData.DungeonRecord rec = DungeonSavedData.get(serverLevel).record(slot);
        if (rec == null) return;
        if (rec.broken) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.isAlive()) {
            String surgeTag = ProjectZero.MOD_ID + ":applied_surges";
            GateRank curRank = MobScaling.getRank(mob);
            int prevSurges = mob.getPersistentData().getInt(surgeTag);
            if (mob.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY)) {
                if (curRank != rec.rank) {
                    GateDungeonArena.applyBossRankScaling(mob, rec.rank);
                }
            } else if (curRank != rec.rank) {
                MobScaling.apply(mob, rec.rank, GateDungeonArena.surgedMobLevel(rec.rank, rec.surges, serverLevel.getRandom()));
                mob.getPersistentData().putInt(surgeTag, rec.surges);
            } else if (rec.surges > prevSurges) {
                int delta = (rec.surges - prevSurges) * GateRules.AGING_SURGE_MOB_LEVELS;
                int nextLvl = Math.min(rec.rank.maxLevel(), MobScaling.effectiveLevel(mob) + delta);
                MobScaling.apply(mob, rec.rank, nextLvl);
                mob.getPersistentData().putInt(surgeTag, rec.surges);
            }
        }
    }
}
