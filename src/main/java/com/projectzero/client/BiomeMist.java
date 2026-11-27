package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.worldgen.ModBiomes;
import com.projectzero.worldgen.ShatteredPlateauFeature;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
 * Biome mist. Cloud Forest: thick fog all the time. Shattered Sky Plateau: light haze on the table,
 * thick cloud down in the chasms (with drifting cloud puffs). Karst Tower Bay: light sea haze. Sky Pillars: fog thickens near the valley floor so the pillars fade into haze
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
        } else if (biome.is(ModBiomes.SUNKEN_MARSH)) {
            target = 0.7F;
        } else if (biome.is(ModBiomes.MANA_WASTES)) {
            target = 0.55F;   // mana haze
        } else if (biome.is(ModBiomes.SHATTERED_PLATEAU)) {
            // Light haze on the table; the chasms are full of cloud, thickest at the bottom.
            float below = ShatteredPlateauFeature.TABLE - at.getY();
            target = 0.3F + Mth.clamp(below / 30F, 0F, 1F) * 1.05F;
            chasmCloud(mc);
        } else if (biome.is(ModBiomes.BLOODMOON_STEPPE)) {
            target = 0.3F;
        } else if (biome.is(ModBiomes.GEYSER_BASIN)) {
            target = 0.45F;   // steam hangs over the basin
        } else if (biome.is(ModBiomes.KARST_BAY)) {
            target = 0.55F;   // sea haze between the towers
        }
        mist += (target - mist) * 0.02F;
    }

    /** Low cloud drifting in the chasms near you: probes random spots below the table top. */
    private static void chasmCloud(Minecraft mc) {
        if (mc.options.particles().get() == ParticleStatus.MINIMAL) return;
        var level = mc.level;
        RandomSource r = level.random;
        BlockPos c = mc.player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int probes = mc.options.particles().get() == ParticleStatus.DECREASED ? 12 : 28;
        for (int i = 0; i < probes; i++) {
            int x = c.getX() + r.nextInt(65) - 32, z = c.getZ() + r.nextInt(65) - 32;
            // Only inside a chasm: open air two blocks under the table here.
            p.set(x, ShatteredPlateauFeature.TABLE - 2, z);
            if (!level.isLoaded(p) || !level.getBlockState(p).isAir()) continue;
            int y = ShatteredPlateauFeature.TABLE - 5 - r.nextInt(40);
            p.setY(y);
            if (!level.getBlockState(p).isAir()) continue;
            level.addParticle(ParticleTypes.CLOUD, x + r.nextFloat(), y + r.nextFloat(), z + r.nextFloat(),
                    (r.nextFloat() - 0.5F) * 0.02F, 0.004F, (r.nextFloat() - 0.5F) * 0.02F);
        }
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
