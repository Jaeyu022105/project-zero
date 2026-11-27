"""
Generates the "realistic vanilla" biome overrides and their features.

Usage:  python tools/biomes/gen.py <folder with vanilla 1.21.1 biome jsons>
(vanilla biomes: data/minecraft/worldgen/biome/*.json from the 1.21.1 client jar, or misode/mcmeta 1.21.1-data)

Rules that keep world generation safe (a broken order crashes world creation with
"Feature order cycle found"):
  * Vanilla features are never added, removed or reordered inside a vanilla biome.
  * Our features go only BEFORE all vanilla ones (PRE, ground cover) or AFTER them (POST),
    always in the global order of the PRE/POST lists below.
  * check_cycles() runs over every biome (vanilla, overridden and ours) before anything is written.
"""
import json, os, sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data')
MOD = os.path.join(ROOT, 'projectzero', 'worldgen')
VEG = 9  # vegetal_decoration step

def st(name, **props):
    s = {"Name": name}
    if props: s["Properties"] = {k: str(v) for k, v in props.items()}
    return s

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', newline='\n') as f:
        json.dump(obj, f, indent=2); f.write('\n')

def placed(name, feature, rarity=None, count=None, heightmap="WORLD_SURFACE_WG", water=True, extra=None):
    pl = []
    if count is not None: pl.append({"type": "minecraft:count", "count": count})
    if rarity is not None: pl.append({"type": "minecraft:rarity_filter", "chance": rarity})
    pl.append({"type": "minecraft:in_square"})
    if water: pl.append({"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0})
    pl.append({"type": "minecraft:heightmap", "heightmap": heightmap})
    if extra: pl += extra
    pl.append({"type": "minecraft:biome"})
    write(os.path.join(MOD, 'placed_feature', name + '.json'), {"feature": feature, "placement": pl})

def configured(name, obj):
    write(os.path.join(MOD, 'configured_feature', name + '.json'), obj)

def ground_patch(name, block, radius=(2, 4)):
    configured(name, {"type": "minecraft:vegetation_patch", "config": {
        "depth": 1, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.4,
        "ground_state": {"type": "minecraft:simple_state_provider", "state": st(block)},
        "replaceable": "#minecraft:dirt", "surface": "floor", "vegetation_chance": 0.0,
        "vegetation_feature": {"feature": {"type": "minecraft:no_op", "config": {}}, "placement": []},
        "vertical_range": 3,
        "xz_radius": {"type": "minecraft:uniform", "min_inclusive": radius[0], "max_inclusive": radius[1]}}})

def plant_patch(name, entries, tries=48, spread=6):
    configured(name, {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": 3,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": {
            "type": "minecraft:weighted_state_provider",
            "entries": [{"weight": w, "data": st(b, **p)} for b, w, p in entries]}}},
            "placement": [{"type": "minecraft:block_predicate_filter",
                           "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]}}})

def shrub(name, log, leaves, radius=2):
    configured(name, {"type": "minecraft:tree", "config": {
        "decorators": [], "force_dirt": False, "ignore_vines": True,
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": st("minecraft:dirt")},
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": st(log, axis="y")},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 1, "height_rand_a": 0, "height_rand_b": 0},
        "foliage_provider": {"type": "minecraft:simple_state_provider",
                             "state": st(leaves, distance=7, persistent="false", waterlogged="false")},
        "foliage_placer": {"type": "minecraft:bush_foliage_placer", "radius": radius, "offset": 1, "height": 2},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 0, "lower_size": 0, "upper_size": 0}}})

SAPLING_OK = lambda sap: [{"type": "minecraft:block_predicate_filter", "predicate": {
    "type": "minecraft:would_survive", "state": st(sap, stage=0)}}]

# ---------------------------------------------------------------- feature library
def build_features():
    # Ground cover (PRE)
    ground_patch("coarse_dirt_patch", "minecraft:coarse_dirt")
    ground_patch("podzol_patch", "minecraft:podzol", (3, 5))
    ground_patch("mud_patch", "minecraft:mud")
    ground_patch("rooted_dirt_patch", "minecraft:rooted_dirt", (1, 3))
    # Boulders
    for n, b in [("boulder_mossy", "minecraft:mossy_cobblestone"), ("boulder_stone", "minecraft:stone"),
                 ("boulder_andesite", "minecraft:andesite"), ("boulder_sandstone", "minecraft:sandstone"),
                 ("boulder_red_sandstone", "minecraft:red_sandstone"), ("boulder_granite", "minecraft:granite")]:
        configured(n, {"type": "projectzero:boulder", "config": {"state": st(b)}})
    # Fallen logs
    for n, b in [("fallen_oak", "minecraft:oak_log"), ("fallen_birch", "minecraft:birch_log"),
                 ("fallen_spruce", "minecraft:spruce_log"), ("fallen_dark_oak", "minecraft:dark_oak_log"),
                 ("fallen_jungle", "minecraft:jungle_log"), ("fallen_acacia", "minecraft:acacia_log"),
                 ("driftwood", "minecraft:stripped_oak_log")]:
        configured(n, {"type": "projectzero:fallen_log", "config": {"state": st(b, axis="y")}})
    # Shrubs
    for n, log, lv, sap in [("shrub_oak", "minecraft:oak_log", "minecraft:oak_leaves", "minecraft:oak_sapling"),
                            ("shrub_birch", "minecraft:birch_log", "minecraft:birch_leaves", "minecraft:birch_sapling"),
                            ("shrub_spruce", "minecraft:spruce_log", "minecraft:spruce_leaves", "minecraft:spruce_sapling"),
                            ("shrub_acacia", "minecraft:acacia_log", "minecraft:acacia_leaves", "minecraft:acacia_sapling"),
                            ("shrub_dark_oak", "minecraft:dark_oak_log", "minecraft:dark_oak_leaves", "minecraft:oak_sapling"),
                            ("shrub_azalea", "minecraft:oak_log", "minecraft:azalea_leaves", "minecraft:oak_sapling")]:
        shrub(n, log, lv)
    # Undergrowth
    plant_patch("undergrowth", [("minecraft:short_grass", 6, {}), ("minecraft:tall_grass", 2, {"half": "lower"}), ("minecraft:fern", 2, {})])
    plant_patch("fern_carpet", [("minecraft:fern", 5, {}), ("minecraft:short_grass", 2, {})], tries=56)
    plant_patch("wildflowers", [("minecraft:dandelion", 3, {}), ("minecraft:poppy", 3, {}), ("minecraft:oxeye_daisy", 2, {}),
                                ("minecraft:cornflower", 2, {}), ("minecraft:azure_bluet", 2, {}), ("minecraft:short_grass", 6, {})], tries=40, spread=4)
    plant_patch("dry_grass", [("minecraft:dead_bush", 2, {}), ("minecraft:short_grass", 5, {})], tries=24)
    plant_patch("moss_floor", [("minecraft:moss_carpet", 4, {}), ("minecraft:fern", 1, {})], tries=40, spread=5)
    plant_patch("snow_scrub", [("minecraft:sweet_berry_bush", 1, {"age": 3}), ("minecraft:fern", 3, {})], tries=20, spread=5)

def place_library():
    """Placed variants. Rarity = 1 in N chunks; count = per chunk."""
    P = {}
    def pv(name, conf, **kw):
        placed(name, "projectzero:" + conf, **kw); P[name] = True
    for n, r in [("coarse_dirt_patch", 3), ("podzol_patch", 3), ("mud_patch", 4), ("rooted_dirt_patch", 5)]:
        pv(n, n, rarity=r)
    for b in ["mossy", "stone", "andesite", "sandstone", "red_sandstone", "granite"]:
        pv(f"boulder_{b}_rare", f"boulder_{b}", rarity=10)
        pv(f"boulder_{b}", f"boulder_{b}", rarity=4)
    for n, sap in [("oak", "minecraft:oak_sapling"), ("birch", "minecraft:birch_sapling"), ("spruce", "minecraft:spruce_sapling"),
                   ("dark_oak", "minecraft:oak_sapling"), ("jungle", "minecraft:jungle_sapling"), ("acacia", "minecraft:acacia_sapling")]:
        pv(f"fallen_{n}", f"fallen_{n}", rarity=3, extra=None)
        pv(f"fallen_{n}_rare", f"fallen_{n}", rarity=9)
    pv("driftwood", "driftwood", rarity=6, water=False)
    for n, sap in [("oak", "minecraft:oak_sapling"), ("birch", "minecraft:birch_sapling"), ("spruce", "minecraft:spruce_sapling"),
                   ("acacia", "minecraft:acacia_sapling"), ("dark_oak", "minecraft:oak_sapling"), ("azalea", "minecraft:oak_sapling")]:
        pv(f"shrub_{n}", f"shrub_{n}", count=3, extra=SAPLING_OK(sap))
        pv(f"shrub_{n}_sparse", f"shrub_{n}", rarity=3, extra=SAPLING_OK(sap))
    pv("undergrowth", "undergrowth", count=2, heightmap="MOTION_BLOCKING")
    pv("undergrowth_dense", "undergrowth", count=5, heightmap="MOTION_BLOCKING")
    pv("fern_carpet", "fern_carpet", count=2, heightmap="MOTION_BLOCKING")
    pv("wildflowers", "wildflowers", rarity=2, heightmap="MOTION_BLOCKING")
    pv("wildflowers_dense", "wildflowers", count=2, heightmap="MOTION_BLOCKING")
    pv("dry_grass", "dry_grass", count=1, heightmap="MOTION_BLOCKING")
    pv("moss_floor", "moss_floor", rarity=2, heightmap="MOTION_BLOCKING")
    pv("snow_scrub", "snow_scrub", rarity=3, heightmap="MOTION_BLOCKING")
    return P

PRE = ["coarse_dirt_patch", "podzol_patch", "mud_patch", "rooted_dirt_patch"]
POST = (["boulder_%s%s" % (b, s) for b in ["mossy", "stone", "andesite", "sandstone", "red_sandstone", "granite"] for s in ["", "_rare"]]
        + ["fallen_%s%s" % (n, s) for n in ["oak", "birch", "spruce", "dark_oak", "jungle", "acacia"] for s in ["", "_rare"]]
        + ["driftwood"]
        + ["shrub_%s%s" % (n, s) for n in ["oak", "birch", "spruce", "acacia", "dark_oak", "azalea"] for s in ["", "_sparse"]]
        + ["undergrowth", "undergrowth_dense", "fern_carpet", "wildflowers", "wildflowers_dense", "dry_grass", "moss_floor", "snow_scrub"])

# What each vanilla biome gains. Realistic, not loud: a forest floor has logs, shrubs and ferns; plains have grass and the odd rock.
ADD = {
    "plains":            ["undergrowth_dense", "wildflowers", "shrub_oak_sparse", "boulder_stone_rare"],
    "sunflower_plains":  ["undergrowth_dense", "wildflowers", "shrub_oak_sparse", "boulder_stone_rare"],
    "meadow":            ["wildflowers_dense", "undergrowth", "boulder_stone_rare", "shrub_birch_sparse"],
    "forest":            ["coarse_dirt_patch", "boulder_mossy_rare", "fallen_oak", "fallen_birch_rare", "shrub_oak", "fern_carpet", "undergrowth", "moss_floor"],
    "flower_forest":     ["fallen_oak_rare", "fallen_birch_rare", "shrub_oak_sparse", "wildflowers", "undergrowth"],
    "birch_forest":      ["boulder_mossy_rare", "fallen_birch", "shrub_birch", "fern_carpet", "undergrowth"],
    "old_growth_birch_forest": ["boulder_mossy_rare", "fallen_birch", "shrub_birch", "fern_carpet", "undergrowth", "moss_floor"],
    "dark_forest":       ["rooted_dirt_patch", "boulder_mossy_rare", "fallen_dark_oak", "shrub_dark_oak", "fern_carpet", "moss_floor"],
    "taiga":             ["podzol_patch", "boulder_mossy_rare", "fallen_spruce", "shrub_spruce", "fern_carpet", "snow_scrub"],
    "snowy_taiga":       ["boulder_stone_rare", "fallen_spruce_rare", "shrub_spruce_sparse", "snow_scrub"],
    "old_growth_pine_taiga":   ["podzol_patch", "boulder_mossy", "fallen_spruce", "shrub_spruce", "fern_carpet", "moss_floor"],
    "old_growth_spruce_taiga": ["podzol_patch", "boulder_mossy", "fallen_spruce", "shrub_spruce", "fern_carpet", "moss_floor"],
    "windswept_hills":   ["boulder_stone", "boulder_andesite_rare", "shrub_spruce_sparse", "undergrowth"],
    "windswept_gravelly_hills": ["boulder_andesite", "boulder_stone_rare"],
    "windswept_forest":  ["boulder_stone", "fallen_spruce_rare", "shrub_spruce", "fern_carpet"],
    "grove":             ["boulder_stone_rare", "fallen_spruce_rare", "shrub_spruce_sparse"],
    "snowy_slopes":      ["boulder_stone_rare"],
    "stony_peaks":       ["boulder_andesite_rare", "boulder_granite_rare"],
    "snowy_plains":      ["boulder_stone_rare", "shrub_spruce_sparse"],
    "ice_spikes":        ["boulder_stone_rare"],
    "savanna":           ["coarse_dirt_patch", "boulder_granite_rare", "fallen_acacia_rare", "shrub_acacia", "dry_grass", "undergrowth"],
    "savanna_plateau":   ["coarse_dirt_patch", "boulder_granite", "shrub_acacia", "dry_grass"],
    "windswept_savanna": ["coarse_dirt_patch", "boulder_granite", "boulder_andesite_rare", "shrub_acacia_sparse", "dry_grass"],
    "desert":            ["boulder_sandstone_rare", "dry_grass"],
    "badlands":          ["boulder_red_sandstone_rare", "dry_grass"],
    "eroded_badlands":   ["boulder_red_sandstone"],
    "wooded_badlands":   ["coarse_dirt_patch", "boulder_red_sandstone_rare", "shrub_oak_sparse", "dry_grass"],
    "jungle":            ["rooted_dirt_patch", "boulder_mossy_rare", "fallen_jungle", "shrub_azalea_sparse", "fern_carpet", "undergrowth_dense", "moss_floor"],
    "sparse_jungle":     ["fallen_jungle_rare", "shrub_oak", "fern_carpet", "undergrowth"],
    "bamboo_jungle":     ["boulder_mossy_rare", "fallen_jungle_rare", "fern_carpet", "moss_floor"],
    "swamp":             ["mud_patch", "fallen_oak", "shrub_oak_sparse", "fern_carpet", "moss_floor"],
    "mangrove_swamp":    ["moss_floor"],
    "cherry_grove":      ["boulder_stone_rare", "wildflowers", "undergrowth"],
    "beach":             ["driftwood"],
    "snowy_beach":       ["driftwood"],
    "stony_shore":       ["boulder_stone", "boulder_andesite_rare"],
}

def check_cycles(biomes):
    """Same rule as Minecraft's FeatureSorter: per step, consecutive features in any biome give an order edge."""
    edges = {}
    for name, b in biomes.items():
        for step, feats in enumerate(b["features"]):
            for a, c in zip(feats, feats[1:]):
                edges.setdefault((step, a), set()).add((step, c))
    state = {}
    def dfs(n, path):
        state[n] = 1
        for m in edges.get(n, ()):
            if state.get(m) == 1:
                raise SystemExit("Feature order cycle: " + " -> ".join(map(str, path + [m])))
            if not state.get(m): dfs(m, path + [m])
        state[n] = 2
    sys.setrecursionlimit(100000)
    for n in list(edges):
        if not state.get(n): dfs(n, [n])

def main(vanilla_dir):
    vanilla = {}
    for f in os.listdir(vanilla_dir):
        if f.endswith('.json'):
            vanilla[f[:-5]] = json.load(open(os.path.join(vanilla_dir, f)))
    assert len(vanilla) >= 60, "expected all vanilla biomes"
    lib = place_library()
    out = {}
    for name, adds in ADD.items():
        for a in adds: assert a in lib, a
        b = json.loads(json.dumps(vanilla[name]))
        feats = b["features"]
        while len(feats) <= VEG: feats.append([])
        pre = ["projectzero:" + x for x in PRE if x in adds]
        post = ["projectzero:" + x for x in POST if x in adds]
        feats[VEG] = pre + feats[VEG] + post
        out[name] = b
    every = dict(vanilla); every.update(out)
    for f in os.listdir(os.path.join(MOD, 'biome')):
        every["projectzero:" + f[:-5]] = json.load(open(os.path.join(MOD, 'biome', f)))
    check_cycles(every)
    build_features()
    for name, b in out.items():
        write(os.path.join(ROOT, 'minecraft', 'worldgen', 'biome', name + '.json'), b)
    print("wrote", len(out), "biome overrides; no feature order cycles")

if __name__ == '__main__':
    main(sys.argv[1])
