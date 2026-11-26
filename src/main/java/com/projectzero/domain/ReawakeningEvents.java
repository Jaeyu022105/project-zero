package com.projectzero.domain;

import com.projectzero.ProjectZero;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drives the Reawakening vision once per server tick. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReawakeningEvents {
    private ReawakeningEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        ReawakeningVision.tickAll(event.getServer());
    }
}
