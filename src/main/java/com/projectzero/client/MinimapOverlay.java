package com.projectzero.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

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
        ensureTexture(mc, ClientConfig.MINIMAP_SIZE.get());
        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();
        if (!hasCenter || Math.abs(px - centerX) >= 4 || Math.abs(pz - centerZ) >= 4) {
            centerX = px;
            centerZ = pz;
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
            renderMapRow(mc.level, y, ClientConfig.MINIMAP_ZOOM.get());
        }
        texture.upload();
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

    private static void renderMapRow(Level level, int row, int zoom) {
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
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            if (surfaceY <= level.getMinBuildHeight()) {
                image.setPixelRGBA(x, row, packAbgr(NAVY));
                continue;
            }
            BlockPos surface = new BlockPos(wx, surfaceY - 1, wz);
            BlockState state = level.getBlockState(surface);
            int color = state.getMapColor(level, surface).col | 0xFF000000;
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
            float slope = heightShade(level, wx, wz, surfaceY);
            image.setPixelRGBA(x, row, packAbgr(scaleRgb(color, slope)));
        }
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

        int cx = x + size / 2;
        int cy = y + size / 2;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(mc.player.getYRot()));
        g.fill(-1, -5, 2, 4, 0xFFFFFFFF);
        g.fill(-4, 1, -1, 4, LIGHT_CYAN);
        g.fill(2, 1, 5, 4, LIGHT_CYAN);
        g.pose().popPose();
        g.drawString(mc.font, "N", cx - mc.font.width("N") / 2, y + 2, LIGHT_CYAN, false);
    }
}
