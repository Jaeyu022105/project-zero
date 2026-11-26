package com.projectzero;

import com.projectzero.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point for Project Zero.
 *
 * Feature areas live in their own packages so later systems (gates, dungeons, bosses)
 * can plug in without touching the leveling code:
 *   leveling/  player level, XP, stats and their effects (server authoritative)
 *   network/   packets between server and client
 *   client/    key bindings, status window, client-side caches
 */
@Mod(ProjectZero.MOD_ID)
public final class ProjectZero {
    public static final String MOD_ID = "projectzero";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectZero() {
        ModNetwork.register();
        LOGGER.info("[System] Project Zero loaded. Welcome, Player.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
