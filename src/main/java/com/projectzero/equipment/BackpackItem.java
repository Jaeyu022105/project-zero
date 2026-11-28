package com.projectzero.equipment;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A carried container whose contents are stored on the item stack itself. */
public final class BackpackItem extends Item {
    private final int slots;

    public BackpackItem(int slots, Properties properties) {
        super(properties.stacksTo(1));
        this.slots = slots;
    }

    public int slots() {
        return slots;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.projectzero.backpack.slots", slots).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.projectzero.backpack.equip_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
