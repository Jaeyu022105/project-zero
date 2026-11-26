package com.projectzero.gates;

import com.projectzero.ProjectZero;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
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
}
