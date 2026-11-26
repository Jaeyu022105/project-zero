package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.AwakeningStoneBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ProjectZero.MOD_ID);

    /** Two blocks tall. Unbreakable in survival: stones belong to their village. */
    public static final RegistryObject<Block> AWAKENING_STONE = BLOCKS.register("awakening_stone",
            () -> new AwakeningStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(state -> 12)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
}
