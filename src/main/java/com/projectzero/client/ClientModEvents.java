package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.ShardCharge;
import com.projectzero.registry.ModBlockEntities;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModEntities;
import com.projectzero.registry.ModItems;
import com.projectzero.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.projectzero.fx.ModParticles;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only registration on the mod bus (renderers and container screens). */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JOB_CARD.get(), JobCardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GATE_PORTAL.get(), GateRiftRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW.get(), ManaParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARK.get(), ManaParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RUNE.get(), ManaParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHARD.get(), ManaParticle.Provider::new);
        event.registerSpriteSet(ModParticles.MOTE.get(), ManaParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STREAK.get(), ManaParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GauntletLayer.WIDE, () -> GauntletLayer.create(false));
        event.registerLayerDefinition(GauntletLayer.SLIM, () -> GauntletLayer.create(true));
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getPlayerSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new GauntletLayer(renderer, event.getEntityModels(), skin == PlayerSkin.Model.SLIM));
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.ACCESSORIES.get(), AccessoryScreen::new);
            // Vessel: 0 when empty, (tier + 1) / 10 with a shard inside. Picks shard_vessel_t<tier>.
            ItemProperties.register(ModItems.SHARD_VESSEL.get(), ProjectZero.id("fill"), (stack, level, entity, seed) -> {
                ShardCharge charge = ShardCharge.get(stack);
                return charge.hasShard() ? (charge.tier() + 1) / 10F : 0F;
            });
            // Charged shards: tier / 10. Picks awakening_shard_t<tier>.
            ItemProperties.register(ModItems.AWAKENING_SHARD.get(), ProjectZero.id("tier"), (stack, level, entity, seed) -> {
                Integer tier = stack.get(ModDataComponents.SHARD_CHARGE_TIER.get());
                return tier == null ? 0F : tier / 10F;
            });
        });
    }
}
