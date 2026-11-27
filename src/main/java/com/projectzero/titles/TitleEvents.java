package com.projectzero.titles;

import com.projectzero.ProjectZero;
import com.projectzero.leveling.LevelingService;
import com.projectzero.leveling.PlayerLevelUpEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side Forge bus events for milestones and titles. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TitleEvents {
    private TitleEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(TitleRegistry.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TitleCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            MilestoneTracker.recordDeath(deadPlayer);
            return;
        }
        if (com.projectzero.leveling.KillCredit.of(event) instanceof ServerPlayer killer) {
            MilestoneTracker.recordKill(killer, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLevelUp(PlayerLevelUpEvent event) {
        MilestoneTracker.recordLevel(event.getPlayer(), event.getNewLevel());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            float bonus = TitleService.damageBonusAgainst(attacker, event.getEntity());
            if (bonus > 0.0F) {
                event.setAmount(event.getAmount() * (1.0F + bonus));
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MilestoneTracker.recordLevel(player, LevelingService.get(player).level());
            TitleService.applyEquippedEffects(player);
            TitleService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        MilestoneTracker.copy(event.getOriginal(), event.getEntity());
        TitleService.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TitleService.applyEquippedEffects(player);
            TitleService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TitleService.sync(player);
        }
    }
}
