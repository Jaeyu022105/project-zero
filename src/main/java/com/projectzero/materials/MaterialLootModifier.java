package com.projectzero.materials;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.gates.GateRank;
import com.projectzero.gates.MobScaling;
import com.projectzero.leveling.XpRewards;
import com.projectzero.registry.ModMaterials;
import com.projectzero.skills.SkillEffects;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Rolls server-side monster materials for player kills. */
public class MaterialLootModifier extends LootModifier {
    public static final MapCodec<MaterialLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst).apply(inst, MaterialLootModifier::new));

    public MaterialLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity victim) || victim instanceof Player
                || victim.getPersistentData().getBoolean(SkillEffects.SUMMON_TAG)) {
            return generatedLoot;
        }
        boolean playerKill = context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)
                || context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof Player;
        if (!playerKill) return generatedLoot;

        RandomSource random = context.getRandom();
        GateRank rank = MobScaling.getRank(victim);
        var entries = MaterialRules.partDrops(victim.getType());
        if (!entries.isEmpty()) {
            int rankSteps = rank == null ? 0 : rank.ordinal();
            int rolls = MaterialRules.BASE_PART_ROLLS
                    + rankSteps * MaterialRules.EXTRA_PART_ROLLS_PER_RANK;
            float lootingBonus = Math.min(MaterialRules.MAX_LOOTING_BONUS,
                    context.getLootingModifier() * MaterialRules.LOOTING_CHANCE_PER_LEVEL);
            for (int roll = 0; roll < rolls; roll++) {
                for (MaterialRules.PartDrop drop : entries) {
                    float rankBonus = rankSteps * MaterialRules.PART_CHANCE_PER_RANK;
                    float chance = Math.min(MaterialRules.MAX_PART_CHANCE,
                            drop.chance() + lootingBonus + rankBonus);
                    if (random.nextFloat() < chance) {
                        generatedLoot.add(new ItemStack(drop.item().get(),
                                MaterialRules.MINIMUM_DROP_COUNT + random.nextInt(
                                        MaterialRules.MAXIMUM_DROP_COUNT - MaterialRules.MINIMUM_DROP_COUNT + 1)));
                    }
                }
            }
        }

        boolean isBoss = victim.getType().is(Tags.EntityTypes.BOSSES)
                || victim.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY);
        boolean isGateMonster = victim.getType().is(XpRewards.GATE_MONSTERS)
                || victim.getPersistentData().getBoolean(MobScaling.GATE_MONSTER_KEY);
        GateRank crystalRank = rank == null ? GateRank.E : rank;
        if (isBoss || (isGateMonster && random.nextInt(100) < MaterialRules.crystalChancePercent(crystalRank))) {
            generatedLoot.add(new ItemStack(crystalFor(crystalRank), MaterialRules.BOSS_CRYSTAL_DROP_COUNT));
        }
        return generatedLoot;
    }

    private static Item crystalFor(GateRank rank) {
        return switch (rank) {
            case E -> ModMaterials.MONSTER_CRYSTAL_E.get();
            case D -> ModMaterials.MONSTER_CRYSTAL_D.get();
            case C -> ModMaterials.MONSTER_CRYSTAL_C.get();
            case B -> ModMaterials.MONSTER_CRYSTAL_B.get();
            case A -> ModMaterials.MONSTER_CRYSTAL_A.get();
            case S -> ModMaterials.MONSTER_CRYSTAL_S.get();
        };
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
