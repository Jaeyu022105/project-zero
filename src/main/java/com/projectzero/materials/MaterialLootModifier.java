package com.projectzero.materials;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.skills.SkillEffects;
import com.projectzero.registry.ModMaterials;
import com.projectzero.leveling.XpRewards;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
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
        var entries = MaterialRules.partDrops(victim.getType());
        if (!entries.isEmpty()) {
            int rolls = MaterialRules.BASE_PART_ROLLS;
            float lootingBonus = Math.min(MaterialRules.MAX_LOOTING_BONUS,
                    context.getLootingModifier() * MaterialRules.LOOTING_CHANCE_PER_LEVEL);
            for (int roll = 0; roll < rolls; roll++) {
                for (MaterialRules.PartDrop drop : entries) {
                    float chance = Math.min(MaterialRules.MAX_PART_CHANCE, drop.chance() + lootingBonus);
                    if (random.nextFloat() < chance) {
                        generatedLoot.add(new ItemStack(drop.item().get(),
                                MaterialRules.MINIMUM_DROP_COUNT + random.nextInt(
                                        MaterialRules.MAXIMUM_DROP_COUNT - MaterialRules.MINIMUM_DROP_COUNT + 1)));
                    }
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
