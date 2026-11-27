package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.ShardCharge;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModEntities;
import com.projectzero.registry.ModItems;
import com.projectzero.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
