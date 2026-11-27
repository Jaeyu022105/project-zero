package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import com.projectzero.fx.Fx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Leaves drift down from any leaf block with open air under it, in every biome (DESIGN-DOCUMENT
 * section 15). Client only: each tick we probe a few random spots around the player, so dense
 * forests shed more than a lone tree without scanning every block. Leaves take the color the
 * game draws that leaf block with (biome foliage tint, or the block's own color for untinted
 * leaves like cherry). Near an open gate some of them get pulled into the rift ({@link ManaParticle}).
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FallingLeaves {
    private FallingLeaves() {}

    /** Spots probed per tick, and the radius/height of the box around the player. */
    private static final int PROBES = 48, RADIUS = 20, HEIGHT = 12;
    /** Chance a probed leaf with air below drops a leaf this tick. */
    private static final float DROP_CHANCE = 0.09F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused() || !ClientConfig.FALLING_LEAVES.get()) return;
        if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;
        RandomSource r = level.random;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        int probes = mc.options.particles().get() == net.minecraft.client.ParticleStatus.DECREASED ? PROBES / 3 : PROBES;
        // Rain and wind shake more loose.
        float chance = DROP_CHANCE * (level.isRaining() ? 1.8F : 1F);
        for (int i = 0; i < probes; i++) {
            pos.set(player.getBlockX() + r.nextInt(RADIUS * 2 + 1) - RADIUS,
                    player.getBlockY() + r.nextInt(HEIGHT * 2 + 1) - HEIGHT / 2,
                    player.getBlockZ() + r.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES)) continue;
            below.set(pos.getX(), pos.getY() - 1, pos.getZ());
            if (!level.getBlockState(below).isAir() || r.nextFloat() > chance) continue;
            int rgb = colorOf(mc, level, state, pos);
            level.addParticle(Fx.leaf(rgb, 1F),
                    pos.getX() + r.nextDouble(), pos.getY() - 0.05, pos.getZ() + r.nextDouble(),
                    (r.nextDouble() - 0.5) * 0.02, -0.01, (r.nextDouble() - 0.5) * 0.02);
        }
    }

    private static int colorOf(Minecraft mc, ClientLevel level, BlockState state, BlockPos pos) {
        int tint = mc.getBlockColors().getColor(state, level, pos, 0);
        if (tint != -1) return tint & 0xFFFFFF;
        return state.getMapColor(level, pos).col;
    }
}
