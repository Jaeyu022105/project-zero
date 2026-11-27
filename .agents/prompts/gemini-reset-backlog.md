# Gemini backlog: notices, level ceiling, break storming, corruption, fireflies, companions, biome polish

You are working alone for several hours while Claude is away. Claude (visuals, models, world gen, review) will review and polish what you push afterwards. Work **top to bottom**. Each task is one or more commits, pushed before you start the next. A finished, tested task beats three half-done ones.

Folder: `C:\Users\Jaime\Downloads\project-zero`. Before anything else, read `AGENTS.md`, `GEMINI.md`, `HANDOFF.md`, `DESIGN-DOCUMENT.md` and `.agents/rules/*`, then the skills in `.agents/skills/` (at least `forge-mod-dev`, `add-game-content`, `system-ui-style`, `game-feel`, `handoff-update`). Load the skill that fits each task before you start it.

## Branch

Other work is being pushed to `claude/visual-pass` right now. Do not commit to it or to `main`.

```powershell
git fetch origin
git checkout -b gemini/reset-backlog origin/claude/visual-pass
```

Commit and push only to `gemini/reset-backlog` (`git push -u origin gemini/reset-backlog`). Claude merges it afterwards.

## Rules for every task

1. **Java:** JDK 21 in PowerShell: `$env:JAVA_HOME="C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"`. Build with `.\gradlew.bat build --no-daemon`.
2. **Test in a loaded world, every task.** `.\gradlew.bat runClient`, create or load a creative world, run the task's test steps, then check `run/logs/latest.log` for ERROR lines and exceptions. Reaching the title screen is not a test.
3. **Commits:** author `Jaeyu022105 <Jaeyu022105@users.noreply.github.com>` (`git config user.name Jaeyu022105`, `git config user.email Jaeyu022105@users.noreply.github.com`). **No `Co-Authored-By` lines and no AI or "Generated with" lines.** Conventional messages (`feat(ui): ...`).
4. **No new dependencies.** Forge 52.1.0, MC 1.21.1, official mappings. Item stacks use **data components**, not stack NBT.
5. **Backend first, placeholder visuals.** Vanilla particles and sounds, flat 16x16 placeholder textures. Mark every placeholder `TODO(claude-art)` in code and list it in your HANDOFF entry.
6. **Server is the source of truth.** Check every client request on the server. Balance numbers go in the system's `*Rules` class. Player data goes in persistent NBT under `projectzero:<system>` and is copied on clone (see `LevelingService.copy`). Bump `ModNetwork.PROTOCOL_VERSION` whenever packets change.
7. **Stuck rule:** if a task still fails to build or crashes after 3 honest attempts, revert that task, write what failed in HANDOFF, and move on.
8. **After each task:** append a HANDOFF.md session entry (what, files, how to test, placeholders, open questions), change DESIGN-DOCUMENT.md `[Planned]` to `[Built]` where true, commit, push.

## Do not touch

Until you reach Task 7, leave these alone: `worldgen/GiantBambooFeature.java`, `worldgen/ShatteredPlateauFeature.java`, `worldgen/ModSurfaceRules.java`, `client/BiomeMist.java`, the biome JSONs for `spirit_bamboo` and `shattered_plateau`, and anything under `tools/biomes/`. Claude's polish work on them was paused partway; Task 7 hands it to you.

Also Claude's visual areas (small hooks only): `client/GateRiftRenderer`, `client/JobCardRenderer`, `client/VisionOverlay*`, `client/anim/*`, `domain/*` visuals, `awakening/CrystalFormation`, finished textures and models. If you need something there, write it in HANDOFF instead.

**Worldgen rule (this crashed the game before):** a feature may only read or write the 3x3 chunks around its origin chunk. Never call `level.getBiome(pos)` or `getBlockState` farther out. The same goes for runtime code that edits terrain: only touch blocks in loaded chunks (`level.isLoaded(pos)`). See AGENTS.md "Worldgen rule".

---

## Task 1: System notices, bottom right (DESIGN-DOCUMENT §11)

Every later task uses these, so build them first.

- `ui/SystemNotices` (server): `send(ServerPlayer, NoticeType, Component title, Component body)`. `NoticeType`: `LEVEL_UP`, `SKILL`, `QUEST`, `GATE`, `ITEM`, `COOLDOWN`, `TITLE`, `WARNING`. S2C `SystemNoticePacket`.
- Client `SystemNoticeOverlay` (HUD layer, bottom right): panels slide in from the right (6 ticks), hold about 4 s, fade out (10 ticks). They stack upward, at most 4 on screen. When a 5th arrives, the oldest leaves first. Style per `system-ui-style` (translucent blue panel, cyan border, caps header). A thin accent stripe on the left in a color per type is enough. Mark the look `TODO(claude-art)`; Claude polishes it.
- Placeholder chime per type with a vanilla sound (`AMETHYST_BLOCK_CHIME` and similar), quiet.
- `ClientConfig`: `ui.noticeChatLines` (default **false**). When false, the [System] chat lines that now have a notice are not printed. When true, both show.
- Route the existing messages through it: level up, skill learned, title earned, gate opened nearby, gate surge, gate rank up, Shard Vessel tier reached, skill cooldown ready (`COOLDOWN`, only for slotted skills with cooldowns of 10 s or more). Find the private `system(...)` helpers in `SkillService`, `JobService`, `AwakeningService`, `GateService`, `LibraryDomain` and replace them with one shared helper that sends the notice and, when the config allows it, the chat line. Job change and dungeon break keep their center title as well.
- Command `/pz notice <type> <text>` for testing.
- Test: gain levels with `/pz xp`, learn a skill, `/pz gate age 7` on a gate, spam `/pz notice gate hi` six times (only 4 stack, oldest leaves first), toggle the config.

## Task 2: Level ceiling at 100 (DESIGN-DOCUMENT §2 and §16)

- `PlayerStats`: `ceilingBroken` (boolean, saved, copied on clone, synced to the client). `LevelingRules`: `CEILING_LEVEL = 100`, and raise `MAX_LEVEL` to `200` with a comment saying the owner still has to confirm the real cap.
- At level 100 without the ceiling broken, XP stops: `addXp` grants nothing and the status window XP bar reads `LEVEL CEILING`. Send one `WARNING` notice the first time the player reaches it ("Level ceiling reached. Someone of high rank must judge you.", or your own wording).
- `LevelingService.breakCeiling(ServerPlayer)`: the single entry point the future NPC quest will call. Sets the flag, gives a center title "LIMIT BROKEN" plus a notice, and resumes XP.
- Monarch jobs have no ceiling: add an optional `"no_ceiling": true` field to the job JSON (`JobRegistry`, default false). Not set on any current job yet.
- The XP curve keeps the same formula past 100. Check `XpRewards` and level-gap math still work above 100.
- Commands: `/pz ceiling status|break|reset`.
- Test: `/pz xp` up to level 100 (add a `/pz level <n>` debug command if that is too slow), kill mobs (no XP), `/pz ceiling break` (title, XP resumes), relog keeps it, death keeps it.

## Task 3: Storm the gate (DESIGN-DOCUMENT §9 "Storm the gate")

You built dungeon breaks, so this one is yours end to end.

- When players kill every monster of the current break wave before the next wave is due, the rift **steadies**: `GateBlockEntity` sets `stormable = true` (saved, synced in the update tag) and the gate can be entered again, even though `BROKEN` is true. Use vanilla particles (`END_ROD` pulses) as the placeholder cue; leave the renderer alone and note `TODO(claude-art)` for Claude.
- While any player is inside that gate's dungeon slot, no new waves spawn outside. Remaining outside monsters keep fighting.
- Inside, the dungeon is still populated (break start currently removes the inside monsters, so change that: only remove them when the break started because the dungeon was never built, otherwise leave them in place, or repopulate on first entry during a break; pick the simpler one and write which you chose).
- Killing the boss inside ends the break the same way as outside: stop waves, normal clear rewards, close the gate. If the boss had already walked out on wave 5, the outside kill path stays as it is.
- If every player leaves the dungeon without clearing it, the timer resumes from where it paused.
- Numbers in `GateRules`.
- Test: `/pz gate spawn c small`, `/pz gate break`, clear wave 1 fast, enter, see waves pause outside, kill the boss inside, the gate closes. Second run: enter, leave, the waves resume.

## Task 4: Dungeon break corruption spread (DESIGN-DOCUMENT §9 "Corruption spread")

- During a break, the area around the rift converts step by step into the dungeon's material, a few blocks of radius per wave, up to `CORRUPTION_RADIUS` by rank (E 12, D 14, C 16, B 18, A 20, S 24) in `GateRules`.
- Palette per dungeon theme from `GateDungeonArena` (cave stone, crystals and moss, and for sea gates prismarine, wet stone and kelp in water). Add a small public method there that returns a theme's palette; don't change the generator.
- Only convert natural terrain: add tag `data/projectzero/tags/block/corruptible.json` (stone, deepslate, dirt, grass, sand, gravel, the vanilla stone variants, logs and leaves of natural trees, plus our natural biome blocks like Limestone and Salt Crust). Never touch block entities, containers, anything not in the tag, or blocks in unloaded chunks.
- Record every converted block with its original state in `CorruptionSavedData` (per dimension, keyed by gate position), so it survives reloads.
- When the break is contained (boss killed inside or outside), the corruption recedes over about 30 s (a batch of blocks per tick, outer ring first) and the original blocks come back. Ancient gate ruins stay. If a break is never contained, the corruption stops at its cap.
- Only spreads while a player is within `BREAK_PLAYER_RADIUS`, like the break.
- Commands: `/pz gate corruption grow|clear` on the looked-at gate.
- Test: break a gate in a forest, watch it spread per wave, place a chest and some planks inside the radius (untouched), reload mid-spread, kill the boss, everything comes back.

## Task 5: Fireflies (DESIGN-DOCUMENT §15 "Fireflies")

- Block `firefly_bush`: a plant (cross model like a bush, no collision, breaks instantly, drops itself with shears or silk touch), light level 6, placeholder texture `TODO(claude-art)`.
- Client: at night (and in dark biomes at any time), each bush near the player sends a few glowing firefly particles that drift and blink around it. Add a `FIREFLY` particle (reuse `ManaParticle` with a new kind if that's simpler). Respect the Particles video setting and add client option `ambience.fireflies`.
- Swarms over water at dusk: in the same client ticker, near water blocks in the listed biomes, spawn a few extra fireflies just above the surface between time 12000 and 14000.
- **Placement through a Forge biome modifier only** (`data/projectzero/forge/biome_modifier/fireflies.json`, step `vegetal_decoration`) plus a configured and placed feature. Do **not** edit any biome JSON and do not touch `tools/biomes/`. Target biomes: `#minecraft:is_forest`, swamps, `projectzero:spirit_bamboo`, `projectzero:bloodmoon_steppe`, `projectzero:sunken_marsh`, `projectzero:capwood`, `projectzero:elderwood`, `projectzero:cloud_forest` (ids from `ModBiomes`). The feature must only place inside its own chunk.
- Test: **create a brand-new world** (a feature order problem crashes world creation, so this is the real test), `/locate biome` to a forest and to the Bloodmoon Steppe, wait for night, check the log.

## Task 6: Companions foundation and screen (DESIGN-DOCUMENT §13)

Backend and a plain screen only. No new mob models (Claude does models and animations).

- `companions/PlayerCompanions` (NBT `projectzero:companions`, copied on clone): a roster of entries. Each entry: id (UUID), kind (`SUMMON` or `PET`), entity type id, name, level, XP, five stat values, unspent points, gear stacks (up to 4, by slot name), and the live entity UUID if it's out.
- Level-up: a companion gains XP when it gets a kill or assists the owner; on level-up its new points (3 per level, in `CompanionRules`) go to **random** stats.
- Hook the existing `pack_howl` Spirit Hounds: the first time a Beastcaller uses it, add a "Spirit Hound" roster entry; summoned hounds read their level and stats from the entry and give XP back to it. Keep the skill's current behaviour otherwise.
- Item `bond_crystal` (placeholder texture `TODO(claude-art)`, recipe with mana crystal shards): used from the screen on a companion, it refunds all its points as unspent so the owner can spend them.
- `CompanionsScreen` (status window button `SUMMONS`, shown only for summoner-type jobs; add an optional `"companions": true` field to the job JSON and set it on Beastcaller): list on the left, selected companion's level, XP bar, stats with `+` buttons, abilities line and gear slots on the right. Plain `system-ui-style` look, `TODO(claude-art)`. C2S packets for spending points and using a Bond Crystal, all checked on the server.
- Gear: store and show only for now (stats from gear add to the companion's stats). Showing gear on the model is Claude's.
- Commands: `/pz companion list|add <entity> <name>|xp <name> <amount>`.
- Test: Beastcaller job via `/pz job set`, cast Pack Howl, open SUMMONS, give XP, spend points, use a Bond Crystal, relog.

## Task 7: Bamboo and plateau polish (DESIGN-DOCUMENT §15 "[Next] Polish pass")

**Bamboo half done by Claude on 2026-11-27 (draft built into `GiantBambooFeature`, wip folder removed); only the plateau remains.** Claude started this and paused. First run `git log --oneline -5 -- src/main/java/com/projectzero/worldgen/GiantBambooFeature.java src/main/java/com/projectzero/worldgen/ShatteredPlateauFeature.java` and read both files as they are now, so you build on what was pushed instead of redoing it. Claude also left an **uncompiled draft** of the bamboo rework in `.agents/prompts/wip/bamboo-polish/`: `GiantBambooFeature.java.txt` (a full replacement for the feature, never built or tested) and `ours.py.diff` (puts `bamboo_shoots` before `giant_bamboo` so the groves can clear shoots off the lanes; apply with `git apply`, then regenerate and run `gen.py`'s feature-order check). Start from the draft: copy it over the real file, make it build, test it, fix what's wrong, and delete the `wip/bamboo-polish` folder in the same commit. No plateau draft exists; start that one from the current file. Load `forge-mod-dev` and follow the worldgen rule above strictly (3x3 chunks, biome checks far away go through the biome source).

- **Spirit Bamboo Sea** (`GiantBambooFeature`): thicker culms (2x2 and 3x3) with a node ring every 4 to 6 blocks, varied height and lean per grove, leaf sprays on short side branches, dense groves on flat ground with narrow walkable lanes, stone lantern paths and moss underfoot. Use existing blocks (bamboo block, stripped bamboo, green wool or leaves for sprays, stone lanterns from stone brick walls plus soul lanterns) and mark any new-block wish as `TODO(claude-art)`.
- **Shattered Sky Plateau** (`ShatteredPlateauFeature`, `ModSurfaceRules`): a flat high table first (fill up to a level top in the biome, within the chunk), then narrower, sharper chasms with stepped ledges that keep following the world-wide noise across chunks, more and bigger floating slabs, some linked by stone arches. Mist in the chasms by depth goes in `client/BiomeMist` (keep its current behaviour for other biomes).
- Never reorder vanilla features in a biome JSON. If you change a biome JSON, run `tools/biomes/gen.py`'s check as HANDOFF describes.
- Test: **new world**, `/locate biome projectzero:spirit_bamboo` and `projectzero:shattered_plateau`, fly across several chunk borders (no seams, no "connection lost"), screenshots into HANDOFF.

## If you finish early

Small ones, in any order: skillbooks in dungeon loot chests (rank-weighted, `GateDungeonArena.placeChest`), and first uses for Mana Crystal Shards (Shard Vessel fuel that adds charge XP, and a crafting ingredient for the Bond Crystal). Same rules.

## When you stop

Push, then make sure the last HANDOFF entry lists every task's status (done, partial, reverted), every `TODO(claude-art)`, and the open questions for Jaime (the real level cap above 100 is one).
