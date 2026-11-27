package com.projectzero.worldgen;

import com.mojang.serialization.MapCodec;
import com.projectzero.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The World Tree: a rare colossal tree in the Elderwood, far too big for a feature, so it is a
 * structure drawn chunk by chunk ({@link WorldTreePiece}). Find one with
 * {@code /locate structure projectzero:world_tree}.
 */
public class WorldTreeStructure extends Structure {
    public static final MapCodec<WorldTreeStructure> CODEC = simpleCodec(WorldTreeStructure::new);

    public WorldTreeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        if (y <= ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        BlockPos base = new BlockPos(x, y, z);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new WorldTreePiece(base, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WORLD_TREE.get();
    }
}
