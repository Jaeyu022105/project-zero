package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.worldgen.WorldTreePiece;
import com.projectzero.worldgen.WorldTreeStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Our structure types and piece types. Placement lives in data/projectzero/worldgen/structure(_set). */
public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ProjectZero.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ProjectZero.MOD_ID);

    public static final RegistryObject<StructureType<WorldTreeStructure>> WORLD_TREE = TYPES.register("world_tree", () -> () -> WorldTreeStructure.CODEC);
    public static final RegistryObject<StructurePieceType> WORLD_TREE_PIECE = PIECES.register("world_tree", () -> (StructurePieceType) WorldTreePiece::new);
}
