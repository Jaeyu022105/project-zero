package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.gates.GateBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Block entity types for Project Zero. */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ProjectZero.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<GateBlockEntity>> GATE_PORTAL =
            BLOCK_ENTITIES.register("gate_portal",
                    () -> BlockEntityType.Builder.of(GateBlockEntity::new, ModBlocks.GATE_PORTAL.get()).build(null));
}
