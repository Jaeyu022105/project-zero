package com.projectzero.equipment;

import com.projectzero.leveling.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import java.util.Map;

/**
 * Equippable accessory item for one of the 8 custom {@link AccessorySlotType} slots.
 * Visuals use 16x16 placeholder icons (TODO(claude-art): custom item icons and worn gauntlet model).
 */
public class AccessoryItem extends Item {
    private final AccessorySlotType slotType;

    public AccessoryItem(AccessorySlotType slotType, Properties properties) {
        super(properties);
        this.slotType = slotType;
    }

    public AccessorySlotType slotType() {
        return slotType;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        AccessoryData data = AccessoryData.get(stack);
        if (data == null) return base;
        return Component.literal("[" + data.rank().name() + "-Rank] ")
                .withStyle(data.rank().chatColor())
                .append(base.copy().withStyle(data.rarity().chatColor()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.projectzero.accessory.slot", slotType.displayName())
                .withStyle(ChatFormatting.GRAY));
        AccessoryData data = AccessoryData.get(stack);
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.projectzero.accessory.unrolled")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(data.rarity().displayName().copy().withStyle(data.rarity().chatColor())
                .append(Component.literal(" · " + data.rank().name() + "-Rank").withStyle(data.rank().chatColor())));
        for (Map.Entry<Stat, Integer> e : data.stats().entrySet()) {
            if (e.getValue() != 0) {
                String sign = e.getValue() > 0 ? "+" : "";
                tooltip.add(Component.literal("  " + sign + e.getValue() + " ")
                        .append(e.getKey().displayName())
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        data.special().ifPresent(sp ->
                tooltip.add(Component.literal("  ★ ").append(sp.formatTooltip()).withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (slotType == AccessorySlotType.RELIC) {
            return InteractionResultHolder.pass(held);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(held, true);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack swapped = EquipmentService.quickEquip(serverPlayer, held.copy());
            if (swapped != null) {
                player.setItemInHand(hand, swapped);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8F, 1.1F);
                return InteractionResultHolder.consume(swapped);
            }
        }
        return InteractionResultHolder.pass(held);
    }
}
