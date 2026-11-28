package com.projectzero.skills;

import com.projectzero.ProjectZero;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime state and combat event hooks for timed active buffs, parries, passive procs, and
 * temporary skill summons.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillEffects {
    private SkillEffects() {}

    public static final String SUMMON_TAG = ProjectZero.MOD_ID + ":summon";
    public static final String SUMMON_EXPIRE_TAG = ProjectZero.MOD_ID + ":summon_expire";

    private static final ResourceLocation BLOODLUST_DAMAGE_ID = ProjectZero.id("bloodlust_damage");
    private static final ResourceLocation BLOODLUST_SPEED_ID = ProjectZero.id("bloodlust_speed");
    private static final ResourceLocation FOCUS_ID = ProjectZero.id("focus");

    private record SecondWindState(int ticksLeft, int intervalTicks, float healPerPulse) {}
    private record RiposteState(int ticksLeft, float counterBaseDamage, float attackDamageScale) {}
    private record BackstabState(int ticksLeft, float bonusMultiplier) {}

    private static final Map<UUID, SecondWindState> SECOND_WIND = new HashMap<>();
    private static final Map<UUID, Integer> BLOODLUST_TICKS = new HashMap<>();
    private static final Map<UUID, RiposteState> RIPOSTE = new HashMap<>();
    private static final Map<UUID, BackstabState> BACKSTAB = new HashMap<>();

    public static void startSecondWind(ServerPlayer player, int durationTicks, double totalFraction) {
        int pulses = Math.max(1, durationTicks / 10);
        float totalHeal = (float) (player.getMaxHealth() * totalFraction);
        SECOND_WIND.put(player.getUUID(), new SecondWindState(durationTicks, 10, totalHeal / pulses));
    }

    public static void startBloodlust(ServerPlayer player, int durationTicks, double damageBonus, double speedBonus) {
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.removeModifier(BLOODLUST_DAMAGE_ID);
            atk.addTransientModifier(new AttributeModifier(
                    BLOODLUST_DAMAGE_ID, damageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(BLOODLUST_SPEED_ID);
            spd.addTransientModifier(new AttributeModifier(
                    BLOODLUST_SPEED_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        BLOODLUST_TICKS.put(player.getUUID(), durationTicks);
    }

    public static void clearBloodlust(ServerPlayer player) {
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.removeModifier(BLOODLUST_DAMAGE_ID);
        AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.removeModifier(BLOODLUST_SPEED_ID);
        BLOODLUST_TICKS.remove(player.getUUID());
    }

    public static boolean hasBloodlust(ServerPlayer player) {
        return BLOODLUST_TICKS.containsKey(player.getUUID());
    }

    public static void startRiposte(ServerPlayer player, int windowTicks, float counterBaseDamage, float attackScale) {
        RIPOSTE.put(player.getUUID(), new RiposteState(windowTicks, counterBaseDamage, attackScale));
    }

    public static void armBackstab(ServerPlayer player, int ticks, float bonusMultiplier) {
        BACKSTAB.put(player.getUUID(), new BackstabState(ticks, bonusMultiplier));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();

        SecondWindState sw = SECOND_WIND.get(id);
        if (sw != null) {
            int rem = sw.ticksLeft() - 1;
            if (rem % sw.intervalTicks() == 0) {
                player.heal(sw.healPerPulse());
                player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
            }
            if (rem <= 0) SECOND_WIND.remove(id);
            else SECOND_WIND.put(id, new SecondWindState(rem, sw.intervalTicks(), sw.healPerPulse()));
        }

        Integer bl = BLOODLUST_TICKS.get(id);
        if (bl != null) {
            if (bl - 1 <= 0) {
                clearBloodlust(player);
            } else {
                BLOODLUST_TICKS.put(id, bl - 1);
                if (bl % 5 == 0) {
                    player.serverLevel().sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        RiposteState rp = RIPOSTE.get(id);
        if (rp != null) {
            if (rp.ticksLeft() - 1 <= 0) RIPOSTE.remove(id);
            else RIPOSTE.put(id, new RiposteState(rp.ticksLeft() - 1, rp.counterBaseDamage(), rp.attackDamageScale()));
        }

        BackstabState bs = BACKSTAB.get(id);
        if (bs != null) {
            if (bs.ticksLeft() - 1 <= 0) BACKSTAB.remove(id);
            else BACKSTAB.put(id, new BackstabState(bs.ticksLeft() - 1, bs.bonusMultiplier()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long now = level.getGameTime();
            for (Entity entity : level.getAllEntities()) {
                if (entity != null && entity.getPersistentData().getBoolean(SUMMON_TAG)) {
                    long expire = entity.getPersistentData().getLong(SUMMON_EXPIRE_TAG);
                    if (now >= expire) {
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.25, 0.25, 0.25, 0.02);
                        com.projectzero.companions.CompanionService.clearLiveId(entity);
                        entity.discard();
                    }
                }
            }
        }
    }

    /** Duelist Riposte Stance: negates the next incoming hit and counters the attacker. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RiposteState rp = RIPOSTE.remove(player.getUUID());
        if (rp == null) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        event.setCanceled(true);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(),
                2, 0.2, 0.2, 0.2, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.8F);

        if (attacker instanceof LivingEntity living) {
            float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            living.hurt(player.damageSources().playerAttack(player),
                    rp.counterBaseDamage() + atk * rp.attackDamageScale());
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
        }
    }

    /** Focus passive (+10% first-hit on full-HP mobs) and Veilstalker Shadow Step backstab bonus. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity target = event.getEntity();
        float multiplier = 1.0F;

        if (SkillService.get(player).hasLearned(FOCUS_ID)) {
            Skill focus = SkillRegistry.get(FOCUS_ID);
            float bonus = focus != null && focus.params().has("bonus_multiplier")
                    ? focus.params().get("bonus_multiplier").getAsFloat() : 0.10F;
            if (target.getHealth() >= target.getMaxHealth() - 0.01F) {
                multiplier += bonus;
                player.serverLevel().sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        8, 0.25, 0.25, 0.25, 0.1);
            }
        }

        BackstabState bs = BACKSTAB.remove(player.getUUID());
        if (bs != null) {
            multiplier += bs.bonusMultiplier();
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    14, 0.3, 0.3, 0.3, 0.15);
        }

        if (multiplier != 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onSummonDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(SUMMON_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSummonXp(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(SUMMON_TAG)) {
            event.setCanceled(true);
        }
    }
}
