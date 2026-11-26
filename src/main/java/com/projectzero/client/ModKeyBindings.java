package com.projectzero.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.projectzero.ProjectZero;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
    private ModKeyBindings() {}

    public static final String CATEGORY = "key.categories." + ProjectZero.MOD_ID;

    public static final KeyMapping OPEN_STATUS = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".status",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    @Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_STATUS);
        }
    }

    @Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Handler {
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_STATUS.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new StatusScreen());
                }
            }
        }
    }
}
