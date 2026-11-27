package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.worldgen.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Biome mist. Cloud Forest: thick fog all the time. Karst Tower Bay: light sea haze. Sky Pillars: fog thickens near the valley floor so the pillars fade into haze
 * row behind row, and thins out as you climb above the pillar tops. Fades in and out over a few
 * seconds when you cross the biome edge.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BiomeMist {
    private BiomeMist() {}

    private static float mist, mistO;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        mistO = mist;
        BlockPos at = mc.player.blockPosition();
        float target = 0F;
        var biome = mc.level.getBiome(at);
        if (biome.is(ModBiomes.SKY_PILLARS)) {
            // Thickest in the valley, gone well above the pillar tops (about y 64 to 260).
            target = 1F - Mth.clamp((at.getY() - 90F) / 170F, 0F, 1F);
        } else if (biome.is(ModBiomes.CLOUD_FOREST)) {
            target = 1.25F;   // the forest lives inside the cloud
        } else if (biome.is(ModBiomes.KARST_BAY)) {
            target = 0.55F;   // sea haze between the towers
        }
        mist += (target - mist) * 0.02F;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float m = Mth.lerp((float) event.getPartialTick(), mistO, mist);
        if (m < 0.01F) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) return;
        float far = event.getFarPlaneDistance();
        // Haze starts close and closes in before render distance, so far pillars read as silhouettes.
        float thick = Math.min(m, 1F), extra = Math.max(0F, m - 1F) * 4F;   // above 1: cloud-forest thick
        float near = Mth.lerp(thick, event.getNearPlaneDistance(), Math.min(far * 0.12F, 24F)) * (1F - extra * 0.6F);
        float end = Mth.lerp(thick, far, Math.min(far, 220F) * 0.8F) * (1F - extra * 0.55F);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(Math.max(end, near + 16F));
        event.setCanceled(true);
    }
}
