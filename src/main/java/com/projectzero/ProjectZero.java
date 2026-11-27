package com.projectzero;

import com.projectzero.gates.GateRules;
import com.projectzero.network.ModNetwork;
import com.projectzero.registry.ModBlockEntities;
import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModCreativeTabs;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModEntities;
import com.projectzero.registry.ModItems;
import com.projectzero.registry.ModLootModifiers;
import com.projectzero.registry.ModMenus;
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
 *   domain/    the Library domain (job choice ceremony)
 *   worldgen/  our biomes (TerraBlender region, surface rules)
 */
@Mod(ProjectZero.MOD_ID)
public final class ProjectZero {
    public static final String MOD_ID = "projectzero";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectZero(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        com.projectzero.registry.ModNature.init();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);
        com.projectzero.fx.ModParticles.PARTICLES.register(modBus);
        com.projectzero.registry.ModFeatures.FEATURES.register(modBus);
        modBus.addListener(com.projectzero.worldgen.ModWorldgen::onCommonSetup);

        GateRules.init();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, com.projectzero.config.ClientConfig.SPEC);
        ModNetwork.register();
        LOGGER.info("[System] Project Zero loaded. Welcome, Player.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
