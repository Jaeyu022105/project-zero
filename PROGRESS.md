# Project Zero Progress

Where the project stands against the plan, in one page. `DESIGN-DOCUMENT.md` is the plan (what we're building), `ROADMAP.md` is the build order, `HANDOFF.md` has the details of every session. This file is the short answer to "what's done, what's happening, what's next".

**Every agent keeps this file current** (AGENTS.md §3): when you finish or start a piece of work, move it between the lists below, and when the owner adds to the plan, add a line to the plan log at the bottom in the same commit as the design doc change.

Last updated: **2026-11-29**

---

## Done

**Character and the System**
- Leveling 1 to 100, XP from every mob (gate monsters ×3, bosses ×10), 5 points a level, soft-capped stats, status window (K).
- Random jobs by rarity at level 10, chosen in the Library domain (orbiting face-down cards).
- Skills: mana, cooldowns, 4 basic skills, 10 job signature skills, Skillbooks, Skills window with 3 presets (J, R to cycle).
- Titles and milestones, milestone skills.
- System notices (bottom right), level ceiling at 100, storming a breaking gate, corruption spread around dungeon breaks, fireflies.
- Top-right minimap (first version): north-up terrain, rank-colored nearby gates, player dots, objective-marker hook, biome tint, coordinates, and cave layer.
- Waypoint stones (§16f): craftable rune marker, dimension restrictions, per-dimension saved locations, player attunement, owner renaming, validated safe travel with cooldown, System-style list, and cyan minimap markers.
- Equipment: 8 accessory slots with rolled stats, mob drops, GEAR screen.
- Awakening Stones and crystal formations, Awakening Shards, Shard Vessel charging, Reawakening vision, reroll that keeps 3 random skills.
- Monster parts and monster crystals: player-kill drops from vanilla mobs, rank-scaled part rolls, rank-matched gate crystals, cooking recipes, original textures, sell values and tooltips (§18).
- Companions foundation and screen: saved summon/pet roster, Spirit Hound leveling hook, Beastcaller-only SUMMONS screen, random companion stat growth, server-validated stat spending, Bond Crystal reset, and stored/displayed gear slots (§13).
- Backpacks (§12): Leather Satchel (9), Hunter's Pack (18), and Rank Pack (27), with recipes, original placeholder icons, a dedicated persistent equipment slot, item-owned container contents, B-key menu and server-side no-nesting/capacity rules.
- Skywood and Baobab building sets: stripped logs, wood, slabs, stairs, fences, gates, doors, trapdoors, buttons, pressure plates, feature-growing saplings, crafting, loot, tags, original textures and item-specific placement animations (§15).
- Player animations for every movement and held item; enemy blood and skill marks; boss weakness table.

**Gates and dungeons**
- Rank-colored rifts, sea gates, ancient stone-ring gates.
- Dungeon dimension: big multi-chamber cave dungeons in 8 world themes, mana crystals, chests, return and exit rifts.
- Gate aging (surges, rank-ups) and dungeon breaks (waves plus a boss outside).

**World**
- Large biomes, taller terrain (to y 383), detail on 36 vanilla biomes, roots on vanilla trees, falling leaves.
- 17 biomes of our own: Capwood, Elderwood (with World Trees), Sky Pillars, Skyfall Isles (with Skywood), Cloud Forest, Baobab Savanna, Tsingy Blade Forest, Karst Tower Bay, Salt Mirror Flats, Geyser Basin, Ice Cathedral, Crystal Tundra, Mana Wastes, Bloodmoon Steppe, Spirit Bamboo Sea, Sunken Ruins Marsh, Shattered Sky Plateau.

## In progress

- **Gemini's reset backlog** (`.agents/prompts/gemini-reset-backlog.md`): Tasks 1 to 6 are done on their task branches (System notices bottom right, level ceiling at 100, storming a breaking gate, corruption spread, fireflies, companions foundation and screen). Task 7 (bamboo and plateau polish) moved to Claude.
- **Spirit Bamboo Sea polish [Built] 2026-11-27:** thick 2x2/3x3 culms with node rings, per-grove lean and height, leaf sprays, noise lanes with stone lanterns, moss floor; biome enlarged by mapping Flower Forest to it.
- **Shattered Sky Plateau polish [Built] 2026-11-27:** flat table, stepped misty chasms, big slabs and arches (in-game look not yet checked).
- **Claude, polish pass on every existing biome** (DESIGN-DOCUMENT §15): built rounds 1 to 4 cover Bloodmoon Steppe, Baobab Savanna, Salt Mirror Flats, Geyser Basin, Crystal Tundra, Mana Wastes, Capwood, Elderwood, Cloud Forest, Ice Cathedral and Sunken Ruins Marsh. The separate Codex pass completed the remaining four: Sky Pillars arches and valley boulder heaps; Skyfall Isles splash pools and sky fragments; Tsingy weathered needle tips, rubble and slot pools; Karst Tower Bay low sand-coved islets and sea arches. The Spirit Bamboo Sea and the Shattered Sky Plateau each have their own polish thread.

## Next, from the plan

In roughly this order (see `ROADMAP.md` for the full list):

1. Finish polishing every biome, then crater basins and additional unique blocks, plants, trees and foliage per biome (§15; Skywood and Baobab wood sets are built).
2. First gate monsters with full animation sets, using the built material drop and crystal system (§18, roadmap 8).
3. First Souls-style boss (§10, roadmap 12).
4. Civilizations: lowest-tier town first, with the shabby-shed spawn, house waypoint stones, an Awakening Stone in the center, the Hunter's Association, currency and basic NPC roles (§16a to 16d).
5. Connect Association quests to the minimap objective hook; add civilization discovery and a full-screen map (§11).
6. Trains between civilization tiers, then subways (§16f).
7. Skill workbench and the chosen-and-stacking skill retention on reroll (§4, §6), crafting stations like the Synthesizer.
8. Cinematic texture pass (§17).
9. Later: land and apartments, builder schematics with holograms and worker NPCs (§16e), auctions, arenas, magic stores, civilization-locked NPCs, continents as dimensions with airports and planes, 5 mythical bosses per continent (§16g).

## Plan log

Additions and changes to the plan, newest last. One line each, with the design doc section.

- 2026-11-26: Project start: leveling, stats, jobs by rarity, Awakening Stones, gates by color and size, Monarch path (§1 to §9).
- 2026-11-26: Shard Vessel, Library domain, equipment and accessories, summons and pets, stat soft caps (§3, §5, §6, §12, §13).
- 2026-11-27: Giant and extreme biomes, biome roadmap rounds 3 to 5, underground biomes, fireflies (§15).
- 2026-11-27: Level ceiling at 100, civilizations above and below, main path, travel draft (§2, §16).
- 2026-11-27: System notices bottom right, storm the gate, corruption spread, companions screen (§9, §11, §13).
- 2026-11-27: **Owner's big expansion:**
  - Polish every existing biome; enlarge the Spirit Bamboo Sea; crater basins (biomes inside a ring of mountains) (§15).
  - Unique blocks, plants, trees and foliage per biome, each with crafting uses (§15).
  - Cinematic texture pass after the biomes (§17).
  - Backpacks (§12) and a skill workbench (§4).
  - Reroll: the player picks 3 skills to keep, and kept skills stack per job across rerolls (§6).
  - Mob drops: an item plus body parts (teeth, claws, fur, edible meat), all sellable; monster crystals (§18).
  - Minimap top right with quest markers (§11).
  - Civilization tiers; spawn in a shabby shed in the lowest tier; a waypoint stone in every house; an Awakening Stone in every civilization's center (§16a).
  - NPC roles (blacksmith, artisan, merchant, realtor, builder, guard...) that get better in better civilizations; civilization-locked NPCs (elven enchanters, priests' blessings); crafting stations like a Synthesizer (§16b).
  - Hunter's Association: registration inside the building, licenses, rank discounts, quests, buying loot (§16c).
  - Currency, magic stores with stat items, auctions, arenas (§16d).
  - Realtors (land, apartments), builder schematics with a hologram preview and worker NPCs (§16e).
  - Transport: costly trains between tiers (wait for all ticket holders, seats, skippable ride), continuous Japanese-style subways with escalators, planes between continents unlocked at the highest civilization; craftable waypoint stones that can't go in dungeons (§16f).
  - Continents, each its own dimension with its own biome set, spawn continent is the player's choice; 5 hidden mythical bosses per continent (§16g).
- 2026-11-27: Rope bridges that sag between anchors and sway and shake as you walk; players can build them (§15).
- 2026-11-27: Elves live in tree houses; their main civilization is built around the World Tree (§16a).
