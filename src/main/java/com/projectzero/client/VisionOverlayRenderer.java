package com.projectzero.client;

import com.projectzero.ProjectZero;
import java.lang.reflect.Field;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the Reawakening overlay over the whole HUD: eyelids closing from the top and bottom
 * (with soft edges and the screen dimming behind them), or a plain fade to black.
 *
 * <p>Forge 52 (MC 1.21.1) removed RenderGuiEvent, so the overlay is appended as the last layer of
 * vanilla's {@link Gui} layer stack (which draws above every earlier HUD layer). The layer is added
 * once, on the first client tick, when the Gui is guaranteed to exist.</p>
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VisionOverlayRenderer {
    private VisionOverlayRenderer() {}

    private static final int BLACK = 0xFF000000;
    private static final int CLEAR = 0x00000000;

    private static boolean installed;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (installed) return;
        installed = true;
        Gui gui = Minecraft.getInstance().gui;
        if (gui == null) { installed = false; return; }
        try {
            // Gui has exactly one LayeredDraw field ("layers"); find it by type so no SRG name is needed.
            for (Field f : Gui.class.getDeclaredFields()) {
                if (f.getType() == LayeredDraw.class) {
                    f.setAccessible(true);
                    ((LayeredDraw) f.get(gui)).add(VisionOverlayRenderer::render);
                    return;
                }
            }
            ProjectZero.LOGGER.warn("Vision overlay: no HUD layer stack found on Gui; overlay disabled");
        } catch (ReflectiveOperationException | RuntimeException e) {
            ProjectZero.LOGGER.warn("Vision overlay: could not hook HUD layers; overlay disabled", e);
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        float c = VisionOverlayState.coverage();
        if (c <= 0.001F) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        g.pose().pushPose();
        g.pose().translate(0, 0, 4000);     // above every HUD layer

        if (c >= 0.999F) {
            g.fill(0, 0, w, h, BLACK);
        } else if (!VisionOverlayState.eyelids()) {
            g.fill(0, 0, w, h, ((int) (c * 255) << 24));
        } else {
            // The world dims as the lids get heavy.
            g.fill(0, 0, w, h, ((int) (c * c * 170) << 24));
            int feather = Math.max(10, h / 8);
            int lid = (int) (c * (h / 2F + feather));
            int solid = lid - feather;
            if (solid > 0) {
                g.fill(0, 0, w, solid, BLACK);
                g.fill(0, h - solid, w, h, BLACK);
            }
            int edge = Math.max(0, solid);
            g.fillGradient(0, edge, w, lid, BLACK, CLEAR);
            g.fillGradient(0, h - lid, w, h - edge, CLEAR, BLACK);
        }
        g.pose().popPose();
    }
}
