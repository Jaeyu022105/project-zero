package com.projectzero.worldgen;

import com.projectzero.ProjectZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Our overworld biomes. Each biome is data (data/projectzero/worldgen/biome/*.json); this class
 * holds their keys. {@link ProjectZeroRegion} places them in the world through TerraBlender and
 * {@link ModSurfaceRules} paints their ground.
 */
public final class ModBiomes {
    private ModBiomes() {}

    /** Giant-mushroom woods: huge caps among dark oaks, mycelium and podzol floor, drifting spores. */
    public static final ResourceKey<Biome> CAPWOOD = key("capwood");
    /** Elven woods: giant pale elder trees with leaf-cloud crowns and glowing berry vines, birch between them. */
    public static final ResourceKey<Biome> ELDERWOOD = key("elderwood");
    /** Sheer stone pillars over misty green valleys, pines and vines on top. */
    public static final ResourceKey<Biome> SKY_PILLARS = key("sky_pillars");
    /** Floating islands with waterfalls over lush flowered land. */
    public static final ResourceKey<Biome> SKYFALL_ISLES = key("skyfall_isles");

    /** Moss-draped giant trees in constant fog, hanging roots, waterfalls off every slope. */
    public static final ResourceKey<Biome> CLOUD_FOREST = key("cloud_forest");
    /** Dry golden grass under fat bottle-trunk baobabs, termite mounds and red earth. */
    public static final ResourceKey<Biome> BAOBAB_SAVANNA = key("baobab_savanna");
    /** Fields of razor-thin limestone needles with narrow slots between them. */
    public static final ResourceKey<Biome> TSINGY = key("tsingy");
    /** Jungle-topped limestone towers standing in emerald water. */
    public static final ResourceKey<Biome> KARST_BAY = key("karst_bay");

    /** White salt crust under a still sheet of water that mirrors the sky. */
    public static final ResourceKey<Biome> SALT_FLATS = key("salt_flats");
    /** Rainbow hot springs, steam and launching vents among old pines. */
    public static final ResourceKey<Biome> GEYSER_BASIN = key("geyser_basin");
    /** Blue-ice arches and frozen waterfalls among the ice spikes. */
    public static final ResourceKey<Biome> ICE_CATHEDRAL = key("ice_cathedral");
    /** Snow fields broken by glowing mana-crystal outcrops. */
    public static final ResourceKey<Biome> CRYSTAL_TUNDRA = key("crystal_tundra");

    /** Land around a long-dead gate: crystallised ground, rocks hanging in the air, broken gate rings. */
    public static final ResourceKey<Biome> MANA_WASTES = key("mana_wastes");
    /** Dark red grass and dead trees under a blood-red sky. A high-rank danger zone. */
    public static final ResourceKey<Biome> BLOODMOON_STEPPE = key("bloodmoon_steppe");
    /** Giant bamboo with spirit lanterns among the last cherry trees. */
    public static final ResourceKey<Biome> SPIRIT_BAMBOO = key("spirit_bamboo");
    /** Half-drowned towers, columns and statues of an older civilisation among the mangroves. */
    public static final ResourceKey<Biome> SUNKEN_MARSH = key("sunken_marsh");
    /** A plateau split by deep chasms, with broken-off slabs hanging over them. */
    public static final ResourceKey<Biome> SHATTERED_PLATEAU = key("shattered_plateau");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ProjectZero.id(name));
    }
}
