package com.projectzero.nature;

import com.projectzero.ProjectZero;
import com.projectzero.registry.ModNature;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Maps axe stripping actions onto our original wood species. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID)
public final class NatureEvents {
    private NatureEvents() {}

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!ToolActions.AXE_STRIP.equals(event.getToolAction())) return;
        BlockState state = event.getState();
        var result = strippedSkywood(state.getBlock());
        if (result == null) return;
        event.setFinalState(result.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                state.getValue(RotatedPillarBlock.AXIS)));
    }

    private static net.minecraft.world.level.block.Block strippedSkywood(net.minecraft.world.level.block.Block block) {
        if (block == ModNature.SKYWOOD_LOG.get()) return ModNature.STRIPPED_SKYWOOD_LOG.get();
        if (block == ModNature.SKYWOOD_WOOD.get()) return ModNature.STRIPPED_SKYWOOD_WOOD.get();
        if (block == ModNature.BAOBAB_LOG.get()) return ModNature.STRIPPED_BAOBAB_LOG.get();
        if (block == ModNature.BAOBAB_WOOD.get()) return ModNature.STRIPPED_BAOBAB_WOOD.get();
        return null;
    }
}
