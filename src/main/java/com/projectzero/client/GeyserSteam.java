package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.worldgen.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Steam over the Geyser Basin's hot springs: wisps drift up off open water, and vents (bubble
 * columns reaching the surface) throw up tall plumes. Client only; probes random spots near you.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GeyserSteam {
    private GeyserSteam() {}

    private static final int PROBES = 40, RADIUS = 24;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;
        if (!level.getBiome(mc.player.blockPosition()).is(ModBiomes.GEYSER_BASIN)) return;
        RandomSource r = level.random;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos c = mc.player.blockPosition();
        for (int i = 0; i < PROBES; i++) {
            int x = c.getX() + r.nextInt(RADIUS * 2 + 1) - RADIUS, z = c.getZ() + r.nextInt(RADIUS * 2 + 1) - RADIUS;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            p.set(x, y, z);
            var s = level.getBlockState(p);
            if (s.is(Blocks.BUBBLE_COLUMN)) {
                // Vent: a tall plume.
                for (int k = 0; k < 6; k++)
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5 + r.nextGaussian() * 0.2, y + 1, z + 0.5 + r.nextGaussian() * 0.2,
                            r.nextGaussian() * 0.01, 0.12 + r.nextFloat() * 0.08, r.nextGaussian() * 0.01);
            } else if (s.is(Blocks.WATER) && r.nextFloat() < 0.35F) {
                level.addParticle(ParticleTypes.CLOUD, x + r.nextFloat(), y + 1.05, z + r.nextFloat(), 0, 0.03 + r.nextFloat() * 0.03, 0);
            }
        }
    }
}
