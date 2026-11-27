"""Builds our round-3 biomes (Cloud Forest, Baobab Savanna, Tsingy, Karst Tower Bay) from vanilla templates.

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
    placed("tsingy", "projectzero:tsingy", count=1)
    # Karst Tower Bay: towers stand on the sea floor.
    configured("karst_tower", {"type": "projectzero:karst_tower", "config": {}})
    placed("karst_tower", "projectzero:karst_tower", count={"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 2},
           heightmap="OCEAN_FLOOR_WG", water=False)

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
