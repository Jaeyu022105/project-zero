package com.projectzero.combat;

import com.projectzero.ProjectZero;
import com.projectzero.client.ClientSkillVisuals;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SkillVisualPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Bosses have a kind, skills have an element, and the matchup scales skill damage on bosses only:
 * x1.5 weak, x0.6 resist. The attacker sees a WEAK / RESIST tag on the boss so players can learn
 * matchups without a guide. Gate bosses (MobScaling.GATE_BOSS_KEY) count; set persistent data "pz_boss_kind" to force a kind.
 * Only skill damage counts: {@link #begin}/{@link #end} wrap each skill cast in SkillService.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossAffinity {
    private BossAffinity() {}

    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ProjectZero.id("bosses"));
    public static final float WEAK = 1.5F, RESIST = 0.6F;

    public enum Element { PHYSICAL, SUN, STORM, HEX, SOUL }

    public enum Kind { UNDEAD, BEAST, CONSTRUCT, ABYSSAL, AQUATIC, INFERNAL }

    private static @Nullable ServerPlayer caster;
    private static @Nullable Element element;

    /** Marks the start of a skill cast; damage dealt by {@code player} until {@link #end} is skill damage. */
    public static void begin(ServerPlayer player, ResourceLocation behavior) {
        caster = player;
        element = elementOf(behavior);
    }

    public static void end() {
        caster = null;
        element = null;
    }

    public static Element elementOf(ResourceLocation behavior) {
        return switch (behavior.getPath()) {
            case "solar_cataclysm" -> Element.SUN;
            case "tempest_call" -> Element.STORM;
            case "cursed_edge" -> Element.HEX;
            case "soul_shackle" -> Element.SOUL;
            default -> Element.PHYSICAL;
        };
    }

    public static boolean isBoss(LivingEntity e) {
        return e.getType().is(BOSSES) || e.getType().is(Tags.EntityTypes.BOSSES) || e.getPersistentData().getBoolean(com.projectzero.gates.MobScaling.GATE_BOSS_KEY);
    }

    public static Kind kindOf(LivingEntity e) {
        String tagged = e.getPersistentData().getString("pz_boss_kind");
        for (Kind k : Kind.values()) if (k.name().equalsIgnoreCase(tagged)) return k;
        if (e.getType().is(EntityTypeTags.UNDEAD)) return Kind.UNDEAD;
        if (e instanceof Guardian) return Kind.AQUATIC;   // includes the elder guardian
        if (e instanceof EnderDragon || e instanceof EnderMan || e instanceof Shulker
                || e.getType() == EntityType.WARDEN) return Kind.ABYSSAL;
        if (e instanceof Blaze || e instanceof Ghast || e instanceof MagmaCube || e instanceof AbstractPiglin) return Kind.INFERNAL;
        if (e.getType() == EntityType.IRON_GOLEM) return Kind.CONSTRUCT;
        return Kind.BEAST;
    }

    /** Damage multiplier for an element against a boss kind. */
    public static float multiplier(Kind kind, Element el) {
        return switch (kind) {
            case UNDEAD -> el == Element.SUN || el == Element.SOUL ? WEAK : el == Element.HEX ? RESIST : 1F;
            case BEAST -> el == Element.HEX ? WEAK : 1F;
            case CONSTRUCT -> el == Element.STORM || el == Element.PHYSICAL ? WEAK
                    : el == Element.HEX || el == Element.SOUL ? RESIST : 1F;
            case ABYSSAL -> el == Element.SUN ? WEAK : el == Element.HEX ? RESIST : 1F;
            case AQUATIC -> el == Element.STORM ? WEAK : el == Element.SUN ? RESIST : 1F;
            case INFERNAL -> el == Element.SOUL ? WEAK : el == Element.SUN ? RESIST : 1F;
        };
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        ServerPlayer p = caster;
        Element el = element;
        if (p == null || el == null || event.getSource().getEntity() != p) return;
        LivingEntity target = event.getEntity();
        if (!isBoss(target)) return;
        float m = multiplier(kindOf(target), el);
        if (m == 1F) return;
        event.setAmount(event.getAmount() * m);
        ModNetwork.sendToPlayer(p, new SkillVisualPacket(target.getId(), ClientSkillVisuals.Kind.TAG.ordinal(),
                m > 1F ? 1 : 0, 24));
    }
}
