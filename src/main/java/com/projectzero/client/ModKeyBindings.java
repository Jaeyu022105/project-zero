package com.projectzero.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.projectzero.ProjectZero;
import com.projectzero.network.CastSkillPacket;
import com.projectzero.network.ModNetwork;
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

    public static final KeyMapping SKILL_1 = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".skill_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY);

    public static final KeyMapping SKILL_2 = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".skill_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);

    public static final KeyMapping SKILL_3 = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".skill_3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY);

    public static final KeyMapping SKILL_4 = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".skill_4",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    public static final KeyMapping OPEN_SKILLS = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".skills",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY);

    /** Swaps to the next skill preset, usable mid-fight. */
    public static final KeyMapping CYCLE_PRESET = new KeyMapping(
            "key." + ProjectZero.MOD_ID + ".cycle_preset",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    public static final KeyMapping[] SKILL_KEYS = {SKILL_1, SKILL_2, SKILL_3, SKILL_4};

    @Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_STATUS);
            event.register(OPEN_SKILLS);
            event.register(CYCLE_PRESET);
            for (KeyMapping key : SKILL_KEYS) {
                event.register(key);
            }
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
            while (OPEN_SKILLS.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new SkillLoadoutScreen());
                }
            }
            while (CYCLE_PRESET.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ModNetwork.sendToServer(new com.projectzero.network.SwitchPresetPacket(-1));
                }
            }
            for (int i = 0; i < SKILL_KEYS.length; i++) {
                while (SKILL_KEYS[i].consumeClick()) {
                    if (mc.player != null && mc.screen == null) {
                        ModNetwork.sendToServer(new CastSkillPacket(i));
                    }
                }
            }
        }
    }
}
