package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import com.projectzero.fx.Fx;
import com.projectzero.registry.ModNature;
import com.projectzero.worldgen.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ambient firefly emitter (DESIGN-DOCUMENT section 15):
 * - At night (or any time in dark biomes), nearby firefly_bush blocks emit drifting, blinking fireflies.
 * - At dusk (time 12000..14000), surface water in #projectzero:has_fireflies biomes spawns small swarms.
 * Respects Video Settings -> Particles and ClientConfig.FIREFLIES (ambience.fireflies).
 * TODO(claude-art): firefly particle uses mana_glow sprites until bespoke firefly art is added.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Fireflies {
    private Fireflies() {}

    public static final TagKey<Biome> HAS_FIREFLIES = TagKey.create(Registries.BIOME, ProjectZero.id("has_fireflies"));

    private static final int COLUMN_PROBES = 64;
    private static final int RADIUS = 18;
    private static final int HALF_HEIGHT = 5;
    private static final float BUSH_EMIT_CHANCE = 0.65F;
    private static final float WATER_SWARM_CHANCE = 0.14F;
    private static final int COLOR_LIME = 0xD8FF66;
    private static final int COLOR_GOLD = 0xFFE866;

    private static int bushSpawns;
    private static int waterSpawns;

    public static int bushSpawns() { return bushSpawns; }
    public static int waterSpawns() { return waterSpawns; }
    public static void resetCounters() { bushSpawns = 0; waterSpawns = 0; }

    /** Called from FireflyBushBlock#animateTick on the client. */
    public static void emitFromBush(Level rawLevel, BlockPos pos, RandomSource r) {
        if (!(rawLevel instanceof ClientLevel level)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || !ClientConfig.FIREFLIES.get()) return;
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        if (status == ParticleStatus.DECREASED && r.nextBoolean()) return;
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        boolean night = dayTime >= 12000L && dayTime <= 23500L;
        if (!night && !isDarkBiome(level.getBiome(pos))) return;
        spawnBushFirefly(level, pos, r);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused() || !ClientConfig.FIREFLIES.get()) return;
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;

        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        boolean night = dayTime >= 12000L && dayTime <= 23500L;
        boolean dusk = dayTime >= 12000L && dayTime <= 14000L;

        RandomSource r = level.random;
        int cols = status == ParticleStatus.DECREASED ? COLUMN_PROBES / 3 : COLUMN_PROBES;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int py0 = player.getBlockY();

        for (int i = 0; i < cols; i++) {
            int x = player.getBlockX() + r.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = player.getBlockZ() + r.nextInt(RADIUS * 2 + 1) - RADIUS;
            pos.set(x, py0, z);
            if (!level.isLoaded(pos)) continue;

            Holder<Biome> biome = null;
            for (int dy = -HALF_HEIGHT; dy <= HALF_HEIGHT; dy++) {
                int y = py0 + dy;
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.is(ModNature.FIREFLY_BUSH.get())) {
                    if (!night) {
                        if (biome == null) biome = level.getBiome(pos);
                        if (!isDarkBiome(biome)) continue;
                    }
                    if (r.nextFloat() < BUSH_EMIT_CHANCE) {
                        int count = 1 + r.nextInt(2);
                        for (int c = 0; c < count; c++) {
                            spawnBushFirefly(level, pos, r);
                        }
                    }
                } else if (dusk && state.is(Blocks.WATER) && r.nextFloat() < WATER_SWARM_CHANCE) {
                    above.set(x, y + 1, z);
                    if (!level.getBlockState(above).isAir()) continue;
                    if (biome == null) biome = level.getBiome(pos);
                    if (!biome.is(HAS_FIREFLIES)) continue;
                    int count = 1 + r.nextInt(2);
                    for (int c = 0; c < count; c++) {
                        int rgb = r.nextFloat() < 0.35F ? COLOR_GOLD : COLOR_LIME;
                        float size = 0.8F + r.nextFloat() * 0.35F;
                        double px = x + r.nextDouble();
                        double py = y + 1.12 + r.nextDouble() * 0.65;
                        double pz = z + r.nextDouble();
                        level.addParticle(Fx.firefly(rgb, size), px, py, pz,
                                (r.nextDouble() - 0.5) * 0.015,
                                (r.nextDouble() - 0.45) * 0.006,
                                (r.nextDouble() - 0.5) * 0.015);
                        waterSpawns++;
                    }
                }
            }
        }
    }

    private static void spawnBushFirefly(ClientLevel level, BlockPos pos, RandomSource r) {
        int rgb = r.nextFloat() < 0.25F ? COLOR_GOLD : COLOR_LIME;
        float size = 0.85F + r.nextFloat() * 0.35F;
        double px = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 1.8;
        double py = pos.getY() + 0.35 + r.nextDouble() * 1.25;
        double pz = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 1.8;
        level.addParticle(Fx.firefly(rgb, size), px, py, pz,
                (r.nextDouble() - 0.5) * 0.012,
                (r.nextDouble() - 0.35) * 0.008,
                (r.nextDouble() - 0.5) * 0.012);
        bushSpawns++;
    }

    public static boolean isDarkBiome(Holder<Biome> biome) {
        return biome.is(ModBiomes.BLOODMOON_STEPPE)
                || biome.is(ModBiomes.CAPWOOD)
                || biome.is(ModBiomes.SUNKEN_MARSH)
                || biome.is(ModBiomes.CLOUD_FOREST)
                || biome.is(Biomes.DARK_FOREST)
                || biome.is(Biomes.MANGROVE_SWAMP);
    }
}
