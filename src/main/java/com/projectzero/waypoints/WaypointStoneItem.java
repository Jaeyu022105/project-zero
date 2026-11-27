package com.projectzero.waypoints;

import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;

public final class WaypointStoneItem extends BlockItem {
    public WaypointStoneItem(net.minecraft.world.level.block.Block block, Properties properties) { super(block, properties); }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel level && WaypointRules.isForbidden(level)) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                SystemNotices.system(player, NoticeType.WARNING, Component.translatable("notice.projectzero.waypoint.forbidden_dimension"));
            }
            return InteractionResult.FAIL;
        }
        boolean customName = context.getItemInHand().has(DataComponents.CUSTOM_NAME);
        String name = customName ? context.getItemInHand().getHoverName().getString() : null;
        InteractionResult result = super.place(context);
        if (result.consumesAction() && customName && context.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && context.getPlayer() instanceof ServerPlayer player) {
            net.minecraft.core.BlockPos pos = context.getClickedPos();
            if (!level.getBlockState(pos).is(this.getBlock())) pos = pos.relative(context.getClickedFace());
            WaypointService.applyPlacedCustomName(level, pos, player, name);
        }
        return result;
    }
}
