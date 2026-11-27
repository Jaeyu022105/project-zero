"""Builds our round-3 and round-4 biomes from vanilla templates (Cloud Forest, Baobab Savanna, Tsingy,
Karst Tower Bay, Salt Mirror Flats, Geyser Basin, Ice Cathedral, Crystal Tundra).

Usage: python3 tools/biomes/ours.py <folder with vanilla 1.21.1 biome jsons>; then run gen.py, which
checks every biome for feature order cycles.

Feature-order rule for these biomes: keep the template's vanilla features in vanilla order (removing
some is safe), shared library features go in gen.py's PRE (before vanilla) / POST (after vanilla) order,
and features unique to one biome go last.
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import gen
from gen import st, write, placed, configured, PRE, POST, MOD, VEG

LOG_Y = lambda b: {"type": "minecraft:simple_state_provider", "state": st(b, axis="y")}
LEAVES = lambda b: st(b, distance=7, persistent="false", waterlogged="false")
HANGING_ROOTS = {"type": "minecraft:attached_to_leaves", "probability": 0.22, "exclusion_radius_xz": 0,
                 "exclusion_radius_y": 0, "required_empty_blocks": 1, "directions": ["down"],
                 "block_provider": {"type": "minecraft:simple_state_provider", "state": st("minecraft:hanging_roots", waterlogged="false")}}
MOSS_GROUND = {"type": "minecraft:alter_ground", "provider": {"type": "minecraft:simple_state_provider", "state": st("minecraft:moss_block")}}
ROOTS = lambda log, c, l: {"type": "projectzero:roots", "provider": LOG_Y(log), "chance": c, "max_length": l}
CLOUD_LEAVES = {"type": "minecraft:weighted_state_provider", "entries": [
    {"weight": 3, "data": LEAVES("minecraft:oak_leaves")}, {"weight": 1, "data": LEAVES("minecraft:azalea_leaves")}]}

def features():
    # Cloud Forest: tall moss-draped trees with hanging roots trailing from the leaves.
    configured("cloud_tree", {"type": "minecraft:tree", "config": {
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": st("minecraft:dirt")},
        "trunk_provider": LOG_Y("minecraft:oak_log"),
        "trunk_placer": {"type": "minecraft:mega_jungle_trunk_placer", "base_height": 14, "height_rand_a": 4, "height_rand_b": 10},
        "foliage_provider": CLOUD_LEAVES,
        "foliage_placer": {"type": "minecraft:jungle_foliage_placer", "radius": 2, "offset": 0, "height": 2},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 1, "upper_size": 2},
        "decorators": [{"type": "minecraft:trunk_vine"}, {"type": "minecraft:leave_vine", "probability": 0.3},
                       HANGING_ROOTS, MOSS_GROUND, ROOTS("minecraft:oak_log", 0.9, 4)],
        "force_dirt": False, "ignore_vines": False}})
    small = json.load(open(os.path.join(sys.argv[2] if len(sys.argv) > 2 else "/tmp/vcf", "fancy_oak.json")))
    c = small["config"]; c["foliage_provider"] = CLOUD_LEAVES
    c["decorators"] = [{"type": "minecraft:leave_vine", "probability": 0.25}, HANGING_ROOTS, ROOTS("minecraft:oak_log", 0.8, 3)]
    c["ignore_vines"] = False
    configured("cloud_tree_small", small)
    placed_plain("cloud_tree"); placed_plain("cloud_tree_small")
    configured("cloud_forest_trees", {"type": "minecraft:random_selector", "config": {"features": [
        {"chance": 0.3, "feature": "projectzero:cloud_tree"}, {"chance": 0.45, "feature": "projectzero:cloud_tree_small"}],
        "default": "minecraft:spruce_checked"}})
    placed("cloud_forest_trees", "projectzero:cloud_forest_trees", count=9)
    # Extra springs high on the slopes: waterfalls everywhere.
    gen.write(os.path.join(MOD, 'placed_feature', 'cloud_springs.json'), {"feature": "minecraft:spring_water", "placement": [
        {"type": "minecraft:count", "count": 40}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 72}, "max_inclusive": {"absolute": 260}}},
        {"type": "minecraft:biome"}]})
    # Baobab Savanna.
    configured("baobab", {"type": "projectzero:baobab", "config": {}})
    placed("baobab", "projectzero:baobab", rarity=2)
    configured("termite_mound", {"type": "projectzero:termite_mound", "config": {}})
    placed("termite_mound", "projectzero:termite_mound", rarity=3)
    # Tsingy.
    configured("tsingy", {"type": "projectzero:tsingy", "config": {}})
    placed("tsingy", "projectzero:tsingy", rarity=2)
    # Karst Tower Bay: towers stand on the sea floor.
    configured("karst_tower", {"type": "projectzero:karst_tower", "config": {}})
    placed("karst_tower", "projectzero:karst_tower", rarity=3, heightmap="OCEAN_FLOOR_WG", water=False)

    # Round 4.
    for n in ["salt_pool", "geyser", "ice_arch", "crystal_outcrop"]:
        configured(n, {"type": "projectzero:" + n, "config": {}})
    placed("salt_pool", "projectzero:salt_pool", count=3)
    placed("geyser", "projectzero:geyser", rarity=2)
    placed("ice_arch", "projectzero:ice_arch", rarity=2)
    placed("crystal_outcrop", "projectzero:crystal_outcrop", rarity=2)

    # Round 5.
    for n in ["mana_waste", "dead_tree", "giant_bamboo", "sunken_ruin", "shattered_plateau", "shattered_sky"]:
        configured(n, {"type": "projectzero:" + n, "config": {}})
    placed("mana_waste", "projectzero:mana_waste", count=2, water=False)
    placed("dead_tree", "projectzero:dead_tree", count=2)
    placed("giant_bamboo", "projectzero:giant_bamboo", count=1)
    placed("sunken_ruin", "projectzero:sunken_ruin", rarity=2, water=False, heightmap="OCEAN_FLOOR_WG")
    # Whole-chunk passes from the chunk corner; they weigh the biome per column themselves.
    # shattered_plateau levels the table and cuts the chasms (raw generation, before anything grows);
    # shattered_sky floats slabs and arches over the chasms (last in vegetation).
    placed_plain("shattered_plateau"); placed_plain("shattered_sky")
    gen.plant_patch("bloodmoon_brush", [("minecraft:dead_bush", 3, {}), ("minecraft:short_grass", 2, {})], tries=24)
    placed("bloodmoon_brush", "projectzero:bloodmoon_brush", count=1, heightmap="MOTION_BLOCKING")
    gen.plant_patch("bamboo_shoots", [("minecraft:bamboo", 1, {"age": 0, "leaves": "small", "stage": 0})], tries=32, spread=5)
    placed("bamboo_shoots", "projectzero:bamboo_shoots", count=2, heightmap="MOTION_BLOCKING")

def placed_plain(name):
    gen.write(os.path.join(MOD, 'placed_feature', name + '.json'), {"feature": "projectzero:" + name, "placement": []})

BIOMES = {
    # name: template, vanilla features to drop, shared adds, unique adds {step: [..]}, effects/climate overrides
    "cloud_forest": ("taiga", ["minecraft:trees_taiga"],
        ["podzol_patch", "boulder_mossy", "fallen_oak", "fallen_spruce_rare", "shrub_azalea", "undergrowth_dense", "fern_carpet", "moss_floor"],
        {8: ["projectzero:cloud_springs"], VEG: ["projectzero:cloud_forest_trees"]},
        {"temperature": 0.6, "downfall": 0.95, "effects": {"fog_color": 13227728, "sky_color": 11060432, "water_color": 4159204,
         "water_fog_color": 329011, "grass_color": 6200906, "foliage_color": 5016122}}),
    "baobab_savanna": ("savanna", [],
        ["coarse_dirt_patch", "boulder_granite_rare", "shrub_acacia_sparse", "dry_grass", "undergrowth"],
        {VEG: ["projectzero:baobab", "projectzero:termite_mound"]},
        {"effects": {"fog_color": 15254192, "sky_color": 8104959, "water_color": 4159204, "water_fog_color": 329011,
         "grass_color": 12564309, "foliage_color": 11445290}}),
    "tsingy": ("badlands", [],
        ["boulder_stone_rare", "shrub_oak_sparse", "dry_grass"],
        {VEG: ["projectzero:tsingy"]},
        {"effects": {"fog_color": 14211280, "sky_color": 8364543, "water_color": 4159204, "water_fog_color": 329011,
         "grass_color": 9470285, "foliage_color": 10387789}}),
    "karst_bay": ("warm_ocean", [],
        [],
        {VEG: ["projectzero:karst_tower"]},
        {"temperature": 0.8, "downfall": 0.85, "effects": {"fog_color": 13952736, "sky_color": 9224424, "water_color": 3126682,
         "water_fog_color": 1797978, "grass_color": 6074954, "foliage_color": 4169802}}),
    # Round 4.
    "salt_flats": ("desert", ["minecraft:patch_cactus_desert", "minecraft:patch_dead_bush_2"],
        ["boulder_stone_rare"],
        {VEG: ["projectzero:salt_pool"]},
        {"effects": {"fog_color": 15791352, "sky_color": 11063551, "water_color": 10475760, "water_fog_color": 12642552}}),
    "geyser_basin": ("old_growth_pine_taiga", [],
        ["boulder_stone_rare", "fallen_spruce_rare", "fern_carpet"],
        {VEG: ["projectzero:geyser"]},
        {"effects": {"fog_color": 14212304, "sky_color": 8364543, "water_color": 4245728, "water_fog_color": 2140320,
         "grass_color": 9084000, "foliage_color": 7311436}}),
    "ice_cathedral": ("ice_spikes", [],
        ["boulder_stone_rare"],
        {VEG: ["projectzero:ice_arch"]},
        {"effects": {"fog_color": 13697279, "sky_color": 8364543, "water_color": 4020182, "water_fog_color": 329011}}),
    "crystal_tundra": ("snowy_plains", [],
        ["boulder_stone_rare", "snow_scrub"],
        {VEG: ["projectzero:crystal_outcrop"]},
        {"effects": {"fog_color": 13690111, "sky_color": 10273023, "water_color": 4159204, "water_fog_color": 329011}}),
    # Round 5.
    "mana_wastes": ("windswept_savanna", ["minecraft:trees_windswept_savanna"],
        ["boulder_andesite_rare", "dry_grass"],
        {VEG: ["projectzero:mana_waste"]},
        {"effects": {"fog_color": 10255318, "sky_color": 11570416, "water_color": 8084694, "water_fog_color": 5126300,
         "grass_color": 9341598, "foliage_color": 8156816,
         "particle": {"options": {"type": "minecraft:reverse_portal"}, "probability": 0.004}}}),
    "bloodmoon_steppe": ("wooded_badlands", ["minecraft:trees_badlands", "minecraft:patch_cactus_decorated"],
        ["coarse_dirt_patch", "boulder_granite_rare", "dry_grass", "undergrowth"],
        {VEG: ["projectzero:dead_tree", "projectzero:bloodmoon_brush"]},
        {"effects": {"fog_color": 11553850, "sky_color": 13129290, "water_color": 7023146, "water_fog_color": 4200990,
         "grass_color": 8004644, "foliage_color": 6168094,
         "particle": {"options": {"type": "minecraft:crimson_spore"}, "probability": 0.006}}}),
    "spirit_bamboo": ("cherry_grove", [],
        ["fern_carpet", "undergrowth"],
        {VEG: ["projectzero:giant_bamboo", "projectzero:bamboo_shoots"]},
        {"effects": {"fog_color": 13494488, "sky_color": 8103167, "water_color": 6141935, "water_fog_color": 6141935,
         "grass_color": 7979098, "foliage_color": 6268996,
         "particle": {"options": {"type": "minecraft:end_rod"}, "probability": 0.0015}}}),
    "sunken_marsh": ("mangrove_swamp", [],
        ["mud_patch", "fern_carpet", "moss_floor"],
        {VEG: ["projectzero:sunken_ruin"]},
        {"effects": {"fog_color": 9412746, "sky_color": 7907327, "water_color": 3824192, "water_fog_color": 2367520,
         "foliage_color": 9285927, "grass_color_modifier": "swamp",
         "particle": {"options": {"type": "minecraft:spore_blossom_air"}, "probability": 0.003}}}),
    "shattered_plateau": ("stony_peaks", [],
        ["boulder_stone", "boulder_andesite_rare", "shrub_spruce_sparse", "undergrowth"],
        {0: ["projectzero:shattered_plateau"], VEG: ["projectzero:shattered_sky"]},
        {"effects": {"fog_color": 14213354, "sky_color": 9418992, "water_color": 4159204, "water_fog_color": 329011,
         "grass_color": 8166234, "foliage_color": 6527568}}),
}

def build(vanilla_dir):
    features()
    gen.place_library()
    for name, (tmpl, drop, shared, unique, over) in BIOMES.items():
        b = json.load(open(os.path.join(vanilla_dir, tmpl + '.json')))
        feats = b["features"]
        while len(feats) <= VEG: feats.append([])
        feats[:] = [[f for f in step if f not in drop] for step in feats]
        pre = ["projectzero:" + x for x in PRE if x in shared]
        post = ["projectzero:" + x for x in POST if x in shared]
        assert all(x in PRE or x in POST for x in shared), shared
        feats[VEG] = pre + feats[VEG] + post
        for step, fs in unique.items(): feats[step] = feats[step] + fs
        eff = over.pop("effects", None) if isinstance(over, dict) else None
        over = dict(over)
        if eff:
            keep = {k: v for k, v in b["effects"].items() if k in ("mood_sound", "music")}
            keep.update(eff); b["effects"] = keep
        b.update(over)
        write(os.path.join(MOD, 'biome', name + '.json'), b)
    print("wrote", len(BIOMES), "biomes")

if __name__ == '__main__':
    build(sys.argv[1])
