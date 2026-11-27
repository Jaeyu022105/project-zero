package com.projectzero.skills;

import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Consumable dungeon book that teaches the skill stored in {@link ModDataComponents#SKILL_ID}.
 * Can teach a skill from any job so players can mix builds (DESIGN-DOCUMENT.md §4).
 * Texture: System tome with a glowing sigil (art pass 2026-11-27).
 */
public class SkillbookItem extends Item {
    public SkillbookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(ResourceLocation skillId) {
        ItemStack stack = new ItemStack(ModItems.SKILLBOOK.get());
        stack.set(ModDataComponents.SKILL_ID.get(), skillId);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation skillId = stack.get(ModDataComponents.SKILL_ID.get());
        if (skillId == null) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("This skillbook is blank.").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            Skill skill = SkillRegistry.get(skillId);
            if (skill == null) {
                sp.displayClientMessage(Component.literal("Unknown skill: " + skillId).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!SkillService.learn(sp, skill, true)) {
                sp.displayClientMessage(Component.literal("You already know ").withStyle(ChatFormatting.GRAY)
                        .append(skill.displayName()), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation skillId = stack.get(ModDataComponents.SKILL_ID.get());
        if (skillId == null) {
            tooltip.add(Component.translatable("tooltip.projectzero.skillbook.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.projectzero.skillbook.teaches",
                Skill.displayName(skillId).copy().withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Skill.description(skillId).copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}
