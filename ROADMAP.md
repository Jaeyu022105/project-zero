# Project Zero Roadmap

The build order, agreed with the owner on 2026-11-26. `DESIGN-DOCUMENT.md` says *what* each feature is; this file says *when*. Update the status marks as work lands. Split: **Gemini** = systems, data, saving, server logic. **Claude** = visuals, models, animation, world generation, structures, mobs, review and tests.

Status: ✅ done · 🔨 in progress · ⏳ next · ⬜ later

## Phase 0: Foundations
- ✅ Leveling, stats, status window (K)
- ✅ Random jobs by rarity at level 10
- ✅ Awakening Stone (2-block shrine), Awakening Shard, reroll
- 🔨 Crystal formations (small / large / colossal), glassy crystal

## Phase 1: The System core
1. ⏳ **Library domain job choice.** Claude: arena, orbiting face-down cards, flip reveal. Gemini: freeze, timeout, assign, multiplayer safety.
2. ⬜ **Skills foundation.** Gemini: mana, cooldowns, slots and keys, JSON skills. Claude: HUD (HP/MP/XP), skill icons.
3. ⬜ **Basic skills + one signature skill per job.** Gemini: logic. Claude: effects.
4. ⬜ **Titles and milestones.**

## Phase 2: The world
5. ⬜ **New terrain:** medieval to comic-fantasy biomes (e.g. banner-dotted highlands, giant-mushroom woods, floating-rock fields, crystal tundra). Claude. *Needs the owner's OK on a biome library (TerraBlender), see Open decisions.*
6. ⬜ **Gates:** portals by color (rank) and size (scale). Claude: look and spawning. Gemini: rank rules, timer.
7. ⬜ **First dungeon** (generated rooms, mobs, chests with skillbooks).
8. ⬜ **First gate monsters** (2-3 original mobs, models + animation). Claude.
9. ⬜ **Hidden ruins and hidden dungeons:** ruins in the overworld that hide a sealed dungeon, and "a dungeon inside the dungeon": a secret second dungeon behind a normal one, far more dangerous. Claude: structures. Gemini: triggers and seals.
10. ⬜ **Civilizations:** two village styles, *medieval* and *new/fantasy*, every town built around its Awakening Stone formation (bigger town, bigger stone). Claude: buildings and layouts.
11. ⬜ **NPCs with jobs and homes:** townsfolk with professions (smith, scholar, guild clerk, merchant...), their own house, daily routine, and interaction (trade, quests, gossip). Claude: models, homes, schedules' look. Gemini: AI routines, trades, quests, saving.

## Phase 3: The Souls part
12. ⬜ **First boss** (telegraphs, phases, stagger, arena lock, drops Awakening Shards).
13. ⬜ **Combat feel:** stamina, dodge roll, Agility/Sense effects.

## Phase 4: Beyond the gates
14. ⬜ **Red gates:** entering one locks the exit and takes you to a whole *other world* (its own dimension with its own terrain, sky, mobs and rules) until its boss dies.
15. ⬜ **New materials and weapon crafting:** ores and monster parts from gates and red-gate worlds (e.g. mana-steel, beast fangs, essence stones) feeding new weapons and armor with their own recipes and stations. Gemini: items, recipes, stats. Claude: textures, models, stations.
16. ⬜ **Weapon-themed mobs:** monsters whose drops are the parts for specific weapons.

## Later
- Job trials and job upgrades, gate breaks (monsters spill out), more jobs, bosses and biomes, the Monarch path.

## Open decisions (owner)
- **Biome library:** custom overworld biomes on Forge 1.21.1 practically need **TerraBlender** (a widely used free library). Without it we can only add structures and features to vanilla biomes. Recommendation: use TerraBlender.
