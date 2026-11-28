package com.projectzero.equipment;

import com.projectzero.ProjectZero;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BackpackEvents {
    private BackpackEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) { BackpackService.dropOnDeath(event); }
}
