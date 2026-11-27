package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.jobs.PlayerJob;
import com.projectzero.leveling.PlayerStats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side Forge bus events. Keeps local caches clean when disconnecting from a world or server. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStatsCache.set(new PlayerStats());
        ClientJobCache.set(PlayerJob.NONE, null);
        ClientSkillsCache.reset();
        ClientTitlesCache.reset();
        ClientBonusCache.reset();
        VisionOverlayState.reset();
        ClientWornGear.clear();
        GauntletLayer.clearCache();
    }

    /** Gauntlets on the first-person arm (empty hand). Vanilla draws the arm right after. */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        GauntletLayer.renderFirstPerson(event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPlayer(), event.getArm());
    }
}
