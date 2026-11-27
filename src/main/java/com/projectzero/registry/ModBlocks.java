package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.AwakeningClusterBlock;
import com.projectzero.awakening.AwakeningCrystalBlock;
import com.projectzero.awakening.AwakeningStoneBlock;
import com.projectzero.domain.VoidMoteBlock;
import com.projectzero.domain.VoidSeaBlock;
import com.projectzero.gates.GateBlock;
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

    /**
     * Building block of the big Awakening Stone formations. Translucent like glass, so the glowing
     * cores show through. Unbreakable: they belong to their village.
     */
    public static final RegistryObject<Block> AWAKENING_CRYSTAL = BLOCKS.register("awakening_crystal",
            () -> new AwakeningCrystalBlock(crystalProperties(9).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false), false));

    /** Glowing vein inside a formation: brighter, sparkles and hums. */
    public static final RegistryObject<Block> AWAKENING_CORE = BLOCKS.register("awakening_core",
            () -> new AwakeningCrystalBlock(crystalProperties(15), true));

    /** Solid crystal deep inside a formation: you see the shell, then this, never through. */
    public static final RegistryObject<Block> AWAKENING_CRYSTAL_DEEP = BLOCKS.register("awakening_crystal_deep",
            () -> new AwakeningCrystalBlock(crystalProperties(6), false));

    /** Spiky clusters on the surface of formations. */
    public static final RegistryObject<Block> AWAKENING_CLUSTER = BLOCKS.register("awakening_cluster",
            () -> new AwakeningClusterBlock(crystalProperties(7).noOcclusion().noCollission()));

    /** Mirror floor of the Reawakening sea. Glassy, always drawn at full brightness. */
    public static final RegistryObject<Block> VOID_SEA = BLOCKS.register("void_sea",
            () -> new VoidSeaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .emissiveRendering((state, level, pos) -> true)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Glowing specks hanging in the Reawakening void (and their reflections). */
    public static final RegistryObject<Block> VOID_MOTE = BLOCKS.register("void_mote",
            () -> new VoidMoteBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noCollission()
                    .noOcclusion()
                    .emissiveRendering((state, level, pos) -> true)
                    .pushReaction(PushReaction.BLOCK)));

    /** Rank-colored Gate portal block (TODO(claude-art): swirling rift visuals). */
    public static final RegistryObject<Block> GATE_PORTAL = BLOCKS.register("gate_portal",
            () -> new GateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 13)
                    .emissiveRendering((state, level, pos) -> true)
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK)));

    private static BlockBehaviour.Properties crystalProperties(int light) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .lightLevel(state -> light)
                .sound(SoundType.AMETHYST)
                .pushReaction(PushReaction.BLOCK);
    }
}
