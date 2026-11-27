"""Vanilla tree configs with our buttress roots added (projectzero:roots decorator).

Usage: python3 tools/biomes/trees.py <dir with vanilla 1.21.1 configured_feature JSONs>
Download them from https://raw.githubusercontent.com/misode/mcmeta/1.21.1-data/data/minecraft/worldgen/configured_feature/<name>.json
"""
import json, os, sys

OUT = "src/main/resources/data/minecraft/worldgen/configured_feature/"
# name: (chance per side, max root length)
TREES = {
    "oak": (0.55, 3), "oak_bees_0002": (0.55, 3), "oak_bees_002": (0.55, 3), "oak_bees_005": (0.55, 3),
    "fancy_oak": (0.8, 4), "fancy_oak_bees": (0.8, 4), "fancy_oak_bees_0002": (0.8, 4),
    "fancy_oak_bees_002": (0.8, 4), "fancy_oak_bees_005": (0.8, 4),
    "birch": (0.4, 2), "birch_bees_0002": (0.4, 2), "birch_bees_002": (0.4, 2), "birch_bees_005": (0.4, 2),
    "super_birch_bees": (0.5, 3), "super_birch_bees_0002": (0.5, 3),
    "spruce": (0.45, 2), "pine": (0.4, 2), "mega_spruce": (0.85, 4), "mega_pine": (0.85, 4),
    "dark_oak": (0.8, 3), "jungle_tree": (0.7, 3), "jungle_tree_no_vine": (0.7, 3),
    "mega_jungle_tree": (0.9, 4), "acacia": (0.5, 3), "cherry": (0.6, 3), "cherry_bees_005": (0.6, 3),
    "swamp_oak": (0.8, 3),
}

def main(src):
    os.makedirs(OUT, exist_ok=True)
    for name, (chance, length) in TREES.items():
        with open(os.path.join(src, name + ".json")) as f:
            cf = json.load(f)
        cfg = cf["config"]
        cfg["decorators"] = [d for d in cfg.get("decorators", []) if d.get("type") != "projectzero:roots"]
        cfg["decorators"].append({"type": "projectzero:roots", "provider": cfg["trunk_provider"],
                                  "chance": chance, "max_length": length})
        with open(OUT + name + ".json", "w") as f:
            json.dump(cf, f, indent=2)
    print("wrote", len(TREES), "tree configs")

if __name__ == "__main__":
    main(sys.argv[1])
