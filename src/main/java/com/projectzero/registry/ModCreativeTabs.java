package com.projectzero.registry;

import com.projectzero.ProjectZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectZero.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.projectzero"))
                    .icon(() -> new ItemStack(ModItems.AWAKENING_SHARD.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.AWAKENING_STONE.get());
                        output.accept(ModItems.WAYPOINT_STONE.get());
                        output.accept(ModMaterials.BEAST_FANG.get());
                        output.accept(ModMaterials.BEAST_CLAW.get());
                        output.accept(ModMaterials.BEAST_PELT.get());
                        output.accept(ModMaterials.RAW_BEAST_MEAT.get());
                        output.accept(ModMaterials.COOKED_BEAST_MEAT.get());
                        output.accept(ModMaterials.CHITIN_PLATE.get());
                        output.accept(ModMaterials.VENOM_GLAND.get());
                        output.accept(ModMaterials.BONE_SHARD.get());
                        output.accept(ModMaterials.ROTTEN_SINEW.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_E.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_D.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_C.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_B.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_A.get());
                        output.accept(ModMaterials.MONSTER_CRYSTAL_S.get());
                        output.accept(ModNature.GLOWCAP_GILLS.get());
                        output.accept(ModNature.VIOLET_CAP.get());
                        output.accept(ModNature.SKYWOOD_LOG.get());
                        output.accept(ModNature.SKYWOOD_PLANKS.get());
                        output.accept(ModNature.STRIPPED_SKYWOOD_LOG.get());
                        output.accept(ModNature.SKYWOOD_WOOD.get());
                        output.accept(ModNature.STRIPPED_SKYWOOD_WOOD.get());
                        output.accept(ModNature.SKYWOOD_SLAB.get());
                        output.accept(ModNature.SKYWOOD_STAIRS.get());
                        output.accept(ModNature.SKYWOOD_FENCE.get());
                        output.accept(ModNature.SKYWOOD_FENCE_GATE.get());
                        output.accept(ModNature.SKYWOOD_DOOR.get());
                        output.accept(ModNature.SKYWOOD_TRAPDOOR.get());
                        output.accept(ModNature.SKYWOOD_BUTTON.get());
                        output.accept(ModNature.SKYWOOD_PRESSURE_PLATE.get());
                        output.accept(ModNature.SKYWOOD_SAPLING.get());
                        output.accept(ModNature.SKYWOOD_LEAVES.get());
                        output.accept(ModNature.LIMESTONE.get());
                        output.accept(ModNature.BAOBAB_LOG.get());
                        output.accept(ModNature.BAOBAB_PLANKS.get());
                        output.accept(ModNature.STRIPPED_BAOBAB_LOG.get());
                        output.accept(ModNature.BAOBAB_WOOD.get());
                        output.accept(ModNature.STRIPPED_BAOBAB_WOOD.get());
                        output.accept(ModNature.BAOBAB_SLAB.get());
                        output.accept(ModNature.BAOBAB_STAIRS.get());
                        output.accept(ModNature.BAOBAB_FENCE.get());
                        output.accept(ModNature.BAOBAB_FENCE_GATE.get());
                        output.accept(ModNature.BAOBAB_DOOR.get());
                        output.accept(ModNature.BAOBAB_TRAPDOOR.get());
                        output.accept(ModNature.BAOBAB_BUTTON.get());
                        output.accept(ModNature.BAOBAB_PRESSURE_PLATE.get());
                        output.accept(ModNature.BAOBAB_SAPLING.get());
                        output.accept(ModNature.SALT_CRUST.get());
                        output.accept(ModNature.MANA_CRYSTAL_BLOCK.get());
                        output.accept(ModNature.FIREFLY_BUSH.get());
                        output.accept(ModItems.AWAKENING_SHARD.get());
                        output.accept(ModItems.SHARD_VESSEL.get());
                        output.accept(ModItems.SKILLBOOK.get());
                        output.accept(ModItems.MANA_CRYSTAL_BLOCK.get());
                        output.accept(ModItems.MANA_CRYSTAL_SHARD.get());
                        output.accept(ModItems.BOND_CRYSTAL.get());
                        output.accept(ModItems.AWAKENING_CRYSTAL.get());
                        output.accept(ModItems.AWAKENING_CORE.get());
                        output.accept(ModItems.AWAKENING_CRYSTAL_DEEP.get());
                        output.accept(ModItems.AWAKENING_CLUSTER.get());
                        output.accept(ModItems.IRON_GAUNTLETS.get());
                        output.accept(ModItems.COPPER_RING.get());
                        output.accept(ModItems.SILVER_RING.get());
                        output.accept(ModItems.GOLD_RING.get());
                        output.accept(ModItems.BONE_AMULET.get());
                        output.accept(ModItems.OBSIDIAN_EARRINGS.get());
                        output.accept(ModItems.LEATHER_BELT.get());
                        output.accept(ModItems.HUNTER_CHARM.get());
                        output.accept(ModItems.MANA_CHARM.get());
                    })
                    .build());
}
