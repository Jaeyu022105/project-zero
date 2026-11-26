package com.projectzero;

import com.projectzero.network.ModNetwork;
import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModCreativeTabs;
import com.projectzero.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Project Zero.
 *
 * Feature areas live in their own packages so later systems (gates, dungeons, bosses)
 * can plug in without touching the leveling code:
 *   leveling/  player level, XP, stats and their effects (server authoritative)
 *   network/   packets between server and client
 *   client/    key bindings, status window, client-side caches
 *   registry/  blocks, items, creative tab
 *   awakening/ Awakening Stones (job rerolls)
 */
@Mod(ProjectZero.MOD_ID)
public final class ProjectZero {
    public static final String MOD_ID = "projectzero";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectZero(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        ModNetwork.register();
        LOGGER.info("[System] Project Zero loaded. Welcome, Player.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
