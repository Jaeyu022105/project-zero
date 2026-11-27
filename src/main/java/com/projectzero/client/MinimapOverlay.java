package com.projectzero.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.Util;
import com.projectzero.gates.GateBlock;
import com.projectzero.gates.GateBlockEntity;
import com.projectzero.gates.GateRank;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only north-up terrain map, attached above the vanilla HUD layers. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MinimapOverlay {
    private MinimapOverlay() {}

    private static final int NAVY = 0xFF071321;
    private static final int PANEL = 0xD00A1220;
    private static final int CYAN = 0xFF3FD0FF;
    private static final int LIGHT_CYAN = 0xFF7FE8FF;
    private static boolean installed;
    private static DynamicTexture texture;
    private static ResourceLocation textureId;
    private static NativeImage image;
    private static int textureSize;
    private static int nextRow;
    private static int centerX;
    private static int centerZ;
    private static boolean hasCenter;
    private static int centerLayerY;
    private static Level markerLevel;
    private static long lastMarkerScan = Long.MIN_VALUE;
    private static List<GateMarker> gateMarkers = List.of();
    private static Component objectiveLine;
    private static BlockPos objectiveTarget;

    private record GateMarker(BlockPos pos, GateRank rank, boolean broken) {}

    /** Sets the optional quest/objective line and map target for a future quest system. */
    public static void setObjective(@Nullable Component line, @Nullable BlockPos target) {
        objectiveLine = line;
        objectiveTarget = target == null ? null : target.immutable();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!installed && mc.gui != null) {
            installed = true;
            try {
                for (Field f : Gui.class.getDeclaredFields()) {
                    if (f.getType() == LayeredDraw.class) {
                        f.setAccessible(true);
                        ((LayeredDraw) f.get(mc.gui)).add(MinimapOverlay::render);
                        break;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                ProjectZero.LOGGER.warn("Minimap: could not attach to Gui LayeredDraw", e);
            }
        }

        if (mc.player == null || mc.level == null || mc.isPaused() || !ClientConfig.MINIMAP.get()) return;
        if (markerLevel != mc.level) {
            markerLevel = mc.level;
            gateMarkers = List.of();
            lastMarkerScan = Long.MIN_VALUE;
        }
        if (mc.level.getGameTime() - lastMarkerScan >= 20) {
            scanGateMarkers(mc.level, mc.player.blockPosition());
            lastMarkerScan = mc.level.getGameTime();
        }
        ensureTexture(mc, ClientConfig.MINIMAP_SIZE.get());
        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();
        boolean caveLayer = mc.level.dimensionType().hasCeiling()
                || mc.level.dimension().location().equals(ProjectZero.id("dungeon"));
        int playerLayerY = mc.player.blockPosition().getY();
        if (!hasCenter || Math.abs(px - centerX) >= 4 || Math.abs(pz - centerZ) >= 4
                || (caveLayer && playerLayerY != centerLayerY)) {
            centerX = px;
            centerZ = pz;
            centerLayerY = playerLayerY;
            hasCenter = true;
            nextRow = 0;
        }
        int rows = Math.min(12, textureSize);
        for (int i = 0; i < rows; i++) {
            int y = nextRow++;
            if (y >= textureSize) {
                nextRow = 0;
                y = nextRow++;
            }
            renderMapRow(mc.level, y, ClientConfig.MINIMAP_ZOOM.get(), caveLayer, centerLayerY);
        }
        texture.upload();
    }

    private static void scanGateMarkers(Level level, BlockPos playerPos) {
        Map<BlockPos, GateMarker> found = new HashMap<>();
        int minChunkX = (playerPos.getX() - 160) >> 4;
        int maxChunkX = (playerPos.getX() + 160) >> 4;
        int minChunkZ = (playerPos.getZ() - 160) >> 4;
        int maxChunkZ = (playerPos.getZ() + 160) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof GateBlockEntity gate)) continue;
                    BlockPos pos = gate.anchorPos();
                    if (pos.distSqr(playerPos) > 160 * 160) continue;
                    BlockState state = level.getBlockState(entry.getKey());
                    if (!(state.getBlock() instanceof GateBlock) || state.getValue(GateBlock.EXIT)) continue;
                    found.put(pos.immutable(), new GateMarker(pos.immutable(), state.getValue(GateBlock.RANK),
                            state.getValue(GateBlock.BROKEN)));
                }
            }
        }
        gateMarkers = List.copyOf(found.values());
    }

    private static void ensureTexture(Minecraft mc, int size) {
        if (texture != null && textureSize == size) return;
        if (texture != null) texture.close();
        textureSize = size;
        image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        texture = new DynamicTexture(image);
        textureId = ProjectZero.id("minimap");
        mc.getTextureManager().register(textureId, texture);
        nextRow = 0;
        hasCenter = false;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) image.setPixelRGBA(x, y, packAbgr(NAVY));
        }
        texture.upload();
    }

    private static void renderMapRow(Level level, int row, int zoom, boolean caveLayer, int layerY) {
        int radius = textureSize / 2;
        int dz = row - radius;
        for (int x = 0; x < textureSize; x++) {
            int dx = x - radius;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > radius - 1.5) {
                image.setPixelRGBA(x, row, packAbgr(distance <= radius ? CYAN : 0x00000000));
                continue;
            }
            int wx = centerX + dx * zoom;
            int wz = centerZ + dz * zoom;
            BlockPos probe = new BlockPos(wx, level.getMaxBuildHeight() - 1, wz);
            if (!level.hasChunkAt(probe)) {
                image.setPixelRGBA(x, row, packAbgr(NAVY));
                continue;
            }
            int surfaceY;
            BlockPos surface;
            if (caveLayer) {
                surfaceY = layerY;
                int floorY = surfaceY - 1;
                while (floorY >= Math.max(level.getMinBuildHeight(), surfaceY - 12)
                        && level.getBlockState(new BlockPos(wx, floorY, wz)).isAir()) floorY--;
                if (floorY < Math.max(level.getMinBuildHeight(), surfaceY - 12)) {
                    image.setPixelRGBA(x, row, packAbgr(NAVY));
                    continue;
                }
                surfaceY = floorY + 1;
                surface = new BlockPos(wx, floorY, wz);
            } else {
                surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
                if (surfaceY <= level.getMinBuildHeight()) {
                    image.setPixelRGBA(x, row, packAbgr(NAVY));
                    continue;
                }
                surface = new BlockPos(wx, surfaceY - 1, wz);
            }
            BlockState state = level.getBlockState(surface);
            int color = state.getMapColor(level, surface).col | 0xFF000000;
            if (state.getFluidState().isEmpty()) {
                if (state.is(BlockTags.LEAVES)) {
                    color = multiplyRgb(color, BiomeColors.getAverageFoliageColor(level, surface));
                } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
                        || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
                    color = multiplyRgb(color, BiomeColors.getAverageGrassColor(level, surface));
                }
            } else {
                color = multiplyRgb(color, BiomeColors.getAverageWaterColor(level, surface));
            }
            if (state.getFluidState().isSource()) {
                int depth = 1;
                for (int y = surfaceY - 2; y >= Math.max(level.getMinBuildHeight(), surfaceY - 12); y--) {
                    BlockState below = level.getBlockState(new BlockPos(wx, y, wz));
                    if (below.getFluidState().isEmpty()) break;
                    depth++;
                }
                int darken = Math.max(100, 255 - (depth - 1) * 13);
                color = scaleRgb(color, darken / 255F);
            }
            float slope = caveLayer ? heightShadeCave(level, wx, wz, layerY) : heightShade(level, wx, wz, surfaceY);
            image.setPixelRGBA(x, row, packAbgr(scaleRgb(color, slope)));
        }
    }

    private static float heightShadeCave(Level level, int x, int z, int layerY) {
        int north = caveFloor(level, x, z - 1, layerY);
        int west = caveFloor(level, x - 1, z, layerY);
        int south = caveFloor(level, x, z + 1, layerY);
        int east = caveFloor(level, x + 1, z, layerY);
        int light = (north + west) - (south + east);
        return Math.max(0.68F, Math.min(1.22F, 1F + light * 0.035F));
    }

    private static int caveFloor(Level level, int x, int z, int layerY) {
        if (!level.hasChunkAt(new BlockPos(x, layerY, z))) return layerY;
        for (int y = layerY - 1; y >= Math.max(level.getMinBuildHeight(), layerY - 12); y--) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
        }
        return layerY;
    }

    private static int multiplyRgb(int base, int tint) {
        int r = ((base >> 16) & 255) * ((tint >> 16) & 255) / 255;
        int g = ((base >> 8) & 255) * ((tint >> 8) & 255) / 255;
        int b = (base & 255) * (tint & 255) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float heightShade(Level level, int x, int z, int height) {
        int north = loadedHeight(level, x, z - 1, height);
        int west = loadedHeight(level, x - 1, z, height);
        int south = loadedHeight(level, x, z + 1, height);
        int east = loadedHeight(level, x + 1, z, height);
        int light = (north + west) - (south + east);
        return Math.max(0.68F, Math.min(1.22F, 1F + light * 0.035F));
    }

    private static int loadedHeight(Level level, int x, int z, int fallback) {
        if (!level.hasChunkAt(new BlockPos(x, level.getMaxBuildHeight() - 1, z))) return fallback;
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    private static int scaleRgb(int color, float scale) {
        int r = Math.min(255, Math.round(((color >> 16) & 255) * scale));
        int g = Math.min(255, Math.round(((color >> 8) & 255) * scale));
        int b = Math.min(255, Math.round((color & 255) * scale));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int packAbgr(int argb) {
        int a = (argb >>> 24) & 255;
        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !ClientConfig.MINIMAP.get()
                || mc.screen != null || mc.getDebugOverlay().showDebugScreen() || textureId == null) return;

        int size = ClientConfig.MINIMAP_SIZE.get();
        int x = g.guiWidth() - size - 8;
        int effectCount = mc.player.getActiveEffects().size();
        int y = 6 + effectCount * 28;
        g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0x553FD0FF);
        g.fill(x, y, x + size, y + size, PANEL);
        g.blit(textureId, x, y, 0, 0, size, size, textureSize, textureSize);

        int zoom = ClientConfig.MINIMAP_ZOOM.get();
        int radius = size / 2 - 3;
        for (GateMarker marker : gateMarkers) {
            int dx = Math.round((marker.pos().getX() - mc.player.blockPosition().getX()) / (float) zoom);
            int dz = Math.round((marker.pos().getZ() - mc.player.blockPosition().getZ()) / (float) zoom);
            drawMarker(g, x + size / 2, y + size / 2, radius, dx, dz, marker.rank().argb(),
                    marker.broken(), mc.level.getGameTime());
        }
        for (var waypoint : ClientWaypointCache.entries()) {
            if (!waypoint.valid() || !waypoint.dimension().equals(mc.level.dimension().location())) continue;
            BlockPos pos = BlockPos.of(waypoint.pos());
            int dx = Math.round((pos.getX() - mc.player.blockPosition().getX()) / (float) zoom);
            int dz = Math.round((pos.getZ() - mc.player.blockPosition().getZ()) / (float) zoom);
            drawWaypointMarker(g, x + size / 2, y + size / 2, radius, dx, dz);
        }
        for (var other : mc.level.players()) {
            if (other == mc.player || other.isSpectator()) continue;
            int dx = (int) Math.round((other.getX() - mc.player.getX()) / zoom);
            int dz = (int) Math.round((other.getZ() - mc.player.getZ()) / zoom);
            if (dx * dx + dz * dz <= radius * radius) {
                g.fill(x + size / 2 + dx - 1, y + size / 2 + dz - 1,
                        x + size / 2 + dx + 2, y + size / 2 + dz + 2, 0xFFFFFFFF);
            }
        }
        if (objectiveTarget != null) {
            int dx = Math.round((objectiveTarget.getX() - mc.player.blockPosition().getX()) / (float) zoom);
            int dz = Math.round((objectiveTarget.getZ() - mc.player.blockPosition().getZ()) / (float) zoom);
            drawMarker(g, x + size / 2, y + size / 2, radius, dx, dz, 0xFFFFD34D, false, 0L);
        }

        int cx = x + size / 2;
        int cy = y + size / 2;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(180F - mc.player.getYRot()));
        g.fill(-1, -5, 2, 4, 0xFFFFFFFF);
        g.fill(-4, 1, -1, 4, LIGHT_CYAN);
        g.fill(2, 1, 5, 4, LIGHT_CYAN);
        g.pose().popPose();
        g.drawString(mc.font, "N", cx - mc.font.width("N") / 2, y + 2, LIGHT_CYAN, false);
        if (ClientConfig.MINIMAP_COORDS.get()) {
            var biomeKey = mc.level.getBiome(mc.player.blockPosition()).unwrapKey();
            Component biomeName = biomeKey
                    .<Component>map(key -> Component.translatable(Util.makeDescriptionId("biome", key.location())))
                    .orElse(Component.empty());
            String text = biomeName.getString() + "  " + mc.player.blockPosition().getX() + ", "
                    + mc.player.blockPosition().getY() + ", " + mc.player.blockPosition().getZ();
            int textX = Math.max(0, x + size - mc.font.width(text));
            g.drawString(mc.font, text, textX, y + size + 4, LIGHT_CYAN, false);
        }
        if (objectiveLine != null) {
            g.drawString(mc.font, objectiveLine, x, y + size + 14, 0xFFFFFFFF, false);
        }
    }

    private static void drawMarker(GuiGraphics g, int cx, int cy, int radius, int dx, int dz,
                                   int color, boolean broken, long gameTime) {
        int distanceSq = dx * dx + dz * dz;
        if (distanceSq <= radius * radius) {
            int pulse = broken && (gameTime / 5) % 2 == 0 ? 5 : 3;
            int x = cx + dx;
            int y = cy + dz;
            g.fill(x - pulse / 2, y - pulse / 2, x + (pulse + 1) / 2, y + (pulse + 1) / 2, color);
            return;
        }

        double angle = Math.atan2(dz, dx);
        int px = cx + (int) Math.round(Math.cos(angle) * radius);
        int py = cy + (int) Math.round(Math.sin(angle) * radius);
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) angle));
        g.fill(-2, -2, 3, 3, color);
        g.fill(1, -4, 5, -1, color);
        g.fill(1, 1, 5, 4, color);
        g.pose().popPose();
    }

    private static void drawWaypointMarker(GuiGraphics g, int cx, int cy, int radius, int dx, int dz) {
        if (dx * dx + dz * dz > radius * radius) {
            drawMarker(g, cx, cy, radius, dx, dz, 0xFF55E8FF, false, 0L);
            return;
        }
        int px = cx + dx, py = cy + dz;
        int color = 0xFF55E8FF;
        g.fill(px, py - 2, px + 1, py - 1, color);
        g.fill(px - 1, py - 1, px + 2, py + 1, color);
        g.fill(px, py + 1, px + 1, py + 2, color);
    }
}
