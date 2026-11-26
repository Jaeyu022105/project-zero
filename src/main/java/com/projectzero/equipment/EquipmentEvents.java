package com.projectzero.equipment;

import com.projectzero.ProjectZero;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side events for accessories: login/clone/respawn, keepInventory death drops, and special affixes. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EquipmentEvents {
    private EquipmentEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EquipmentCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EquipmentService.refreshEffects(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        EquipmentService.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EquipmentService.refreshEffects(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EquipmentService.refreshEffects(player);
        }
    }

    /** Drop equipped accessories on death ONLY when keepInventory is false (same rule as armor). */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide() || player.isSpectator()) return;
        if (player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        PlayerAccessories acc = EquipmentService.get(player);
        if (acc.isEmpty()) return;

        for (int i = 0; i < PlayerAccessories.TOTAL_SLOTS; i++) {
            ItemStack s = acc.get(i);
            if (!s.isEmpty()) {
                ItemEntity drop = new ItemEntity(player.level(),
                        player.getX(), player.getY() + 0.5, player.getZ(), s.copy());
                drop.setDefaultPickUpDelay();
                event.getDrops().add(drop);
            }
        }
        EquipmentService.clearAll(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        float amount = event.getAmount();

        // Damage vs undead affix
        if (event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
            double undeadBonus = EquipmentService.affixTotal(attacker, AccessoryData.AffixType.DAMAGE_VS_UNDEAD);
            if (undeadBonus > 0.0) {
                amount *= (float) (1.0 + undeadBonus);
            }
        }

        // Crit chance affix
        double critChance = EquipmentService.affixTotal(attacker, AccessoryData.AffixType.CRIT_CHANCE);
        if (critChance > 0.0 && attacker.getRandom().nextDouble() < Math.min(0.80, critChance)) {
            amount *= (float) EquipmentRules.CRIT_DAMAGE_MULTIPLIER;
            attacker.serverLevel().sendParticles(ParticleTypes.CRIT,
                    event.getEntity().getX(), event.getEntity().getY() + 0.9, event.getEntity().getZ(),
                    10, 0.25, 0.25, 0.25, 0.15);
        }

        event.setAmount(amount);

        // Lifesteal affix
        double lifesteal = EquipmentService.affixTotal(attacker, AccessoryData.AffixType.LIFESTEAL);
        if (lifesteal > 0.0 && amount > 0.0F) {
            attacker.heal((float) (amount * Math.min(0.50, lifesteal)));
        }
    }
}
