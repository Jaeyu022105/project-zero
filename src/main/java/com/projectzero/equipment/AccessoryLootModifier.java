package com.projectzero.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.ProjectZero;
import com.projectzero.gates.GateRank;
import com.projectzero.jobs.JobRarity;
import com.projectzero.leveling.XpRewards;
import com.projectzero.skills.SkillEffects;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Global loot modifier rolling accessory drops on mob deaths (DESIGN-DOCUMENT.md §12):
 * vanilla hostiles 1.5% (Common/Uncommon, rank E), modded mobs 3%, gate monsters 8%, bosses 100%.
 */
public class AccessoryLootModifier extends LootModifier {
    public static final MapCodec<AccessoryLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst).apply(inst, AccessoryLootModifier::new));

    public AccessoryLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity victimEntity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(victimEntity instanceof LivingEntity victim) || victim instanceof Player) {
            return generatedLoot;
        }
        if (victim.getPersistentData().getBoolean(SkillEffects.SUMMON_TAG)) {
            return generatedLoot;
        }
        boolean playerKill = context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)
                || context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof Player;
        if (!playerKill) {
            return generatedLoot;
        }

        RandomSource random = context.getRandom();
        boolean isBoss = victim.getType().is(Tags.EntityTypes.BOSSES)
                || victim.getPersistentData().getBoolean(ProjectZero.MOD_ID + ":gate_boss");
        boolean isGateMob = victim.getType().is(XpRewards.GATE_MONSTERS)
                || victim.getPersistentData().getBoolean(ProjectZero.MOD_ID + ":gate_monster");
        ResourceLocation typeId = EntityType.getKey(victim.getType());

        GateRank storedRank = GateRank.byName(victim.getPersistentData().getString(ProjectZero.MOD_ID + ":mob_rank"));

        double chance;
        JobRarity rarity;
        GateRank rank;

        if (isBoss) {
            chance = EquipmentRules.BOSS_DROP_CHANCE;
            rarity = EquipmentRules.rollStandardRarity(random, true);
            rank = storedRank != null ? storedRank : GateRank.C;
        } else if (isGateMob) {
            chance = EquipmentRules.GATE_MONSTER_DROP_CHANCE;
            rarity = EquipmentRules.rollStandardRarity(random, false);
            rank = storedRank != null ? storedRank : GateRank.E;
        } else if (!"minecraft".equals(typeId.getNamespace())) {
            chance = EquipmentRules.MODDED_MOB_DROP_CHANCE;
            rarity = EquipmentRules.rollStandardRarity(random, false);
            rank = storedRank != null ? storedRank : GateRank.E;
        } else if (victim instanceof Enemy) {
            chance = EquipmentRules.VANILLA_HOSTILE_DROP_CHANCE;
            rarity = EquipmentRules.rollVanillaRarity(random);
            rank = storedRank != null ? storedRank : GateRank.E;
        } else {
            return generatedLoot;
        }

        if (random.nextDouble() < chance) {
            AccessorySlotType slot = EquipmentRules.randomDroppableSlot(random);
            generatedLoot.add(EquipmentRules.rollItem(random, slot, rarity, rank));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
