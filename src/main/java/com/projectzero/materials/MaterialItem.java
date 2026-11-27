package com.projectzero.materials;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Base type for monster materials with a shared tooltip added in the value stage. */
public class MaterialItem extends Item {
    private final String sourceKey;

    public MaterialItem(Properties properties, String sourceKey) {
        super(properties);
        this.sourceKey = sourceKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(sourceKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.projectzero.material_value", MaterialRules.sellValue(this))
                .withStyle(ChatFormatting.GRAY));
    }
}
