# Gemini 12-hour backlog: Project Zero (backend sprint)

You are working alone for about 12 hours. Claude (visuals, models, world gen, review) is away and will review and polish everything you push afterwards. Work **top to bottom**; each task is one or more commits pushed to `main` before starting the next. Get as far as you can; a finished, tested task beats three half-done ones.

Folder: `C:\Users\Jaime\Downloads\project-zero`. Read `AGENTS.md`, `GEMINI.md`, `HANDOFF.md`, `DESIGN-DOCUMENT.md`, `ROADMAP.md` and `.agents/rules/*` first.

## Rules for every task

1. **Java:** JDK 21 in PowerShell: `$env:JAVA_HOME="C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"`. Build with `.\gradlew.bat build --no-daemon`.
2. **Test in a loaded world, every task.** `.\gradlew.bat runClient`, create/load a creative world, run the task's test steps, then check `run/logs/latest.log` for ERROR lines and exceptions. Reaching the title screen is not a test.
3. **Commits:** authored by Jaeyu022105. **No `Co-Authored-By` lines and no AI / "Generated with" lines.** Conventional messages (`feat(skills): ...`).
4. **No new dependencies.** Forge 52.1.0, MC 1.21.1, official mappings. Item stacks use **data components** (1.21), not stack NBT.
5. **Backend first, placeholder visuals.** Use vanilla particles/sounds and simple flat placeholder textures (16x16, one color + outline) and mark every one with `TODO(claude-art)` in a comment or in `HANDOFF.md`. Claude replaces them.
6. **Don't touch Claude's areas** beyond small hooks: `domain/*` visuals and timing, `client/JobCardRenderer`, `client/VisionOverlay*`, `awakening/CrystalFormation`, textures/models already made. If something there blocks you, write it in HANDOFF instead.
7. **Server is the source of truth**; every client request is checked on the server. Balance numbers go in one `*Rules` class per system. Player data in persistent NBT under `projectzero:<system>`, copied on clone (see `LevelingService.copy`). Bump `ModNetwork.PROTOCOL_VERSION` whenever packets change.
8. **Stuck rule:** if a task still does not build or crashes after 3 honest attempts, `git stash`/revert that task, write what failed in HANDOFF, and move to the next task.
9. **After each task:** append a short HANDOFF.md session entry (what, files, how to test, placeholders, open questions), update ROADMAP.md marks and DESIGN-DOCUMENT.md `[Planned]` → `[Built]` where true, commit, push.

---

## Task 0: Commit today's work and rebalance stats (do first)

The folder holds uncommitted, build-checked work by Claude: the Library domain (v1+v2), solid-core crystals with clusters, the Reawakening vision (void sea, eyelid overlay, aura), docs. **Keep `client/VisionOverlayRenderer.java` exactly as it is in the folder** (it adds itself as a HUD layer).
1. Build, then commit in chunks (`feat(domain): library job choice`, `feat(awakening): solid-core crystals and clusters`, `feat(awakening): reawakening vision`, `docs: ...`) and push.
2. Stat soft caps (DESIGN-DOCUMENT §3, owner approved). `p` = points above base, `soft(p,k) = p/(1+p/k)`:
   - Strength: `ATTACK_DAMAGE`, `ADD_MULTIPLIED_TOTAL`, `0.02 * soft(p,200)`.
   - Vitality: `MAX_HEALTH`, `ADD_VALUE`, `0.5 * soft(p,300)`.
   - Agility: `MOVEMENT_SPEED`, `ADD_MULTIPLIED_BASE`, `0.40 * p/(p+60)`; `ATTACK_SPEED`, `ADD_VALUE`, `1.5 * p/(p+60)`.
   - Sense: evade `0.30 * p/(p+100)`.
   - Constants in `LevelingRules` with comments giving the 45/100/495-point values. Update `stat.projectzero.*.desc` in `en_us.json`.
3. Test: `/pz xp` up to ~level 20, spend points in K, `/pz domain` (pick a card), `/pz reawaken` (vision plays, aura on return), `/pz crystal colossal`.

## Task 1: Shard Vessel (DESIGN-DOCUMENT §6)

- Item `shard_vessel` (stack size 1). Right-click with a shard in the other hand to insert; sneak + right-click to take the shard out. Stored with a data component (`ShardCharge`: has shard, charge XP long).
- New item state for charged shards: the Awakening Shard gets a `shard_charge_tier` component (0-5). Shards stack only with the same tier.
- `LivingDeathEvent` (or wherever `XpRewards` grants XP): if the killer has a Vessel holding a shard anywhere in the inventory (first one found), divert `VESSEL_DIVERT = 0.5` of the kill XP into the Vessel. Only one Vessel charges per player.
- Tier thresholds scale with level: tier `t` needs `t * VESSEL_TIER_FRACTION * xpToNextLevel(level)` cumulative XP, `VESSEL_TIER_FRACTION = 0.6`. When taking the shard out, it keeps its tier.
- `JobRegistry.random(...)` gets an overload with a weight table; `JobRules` holds the six tier tables from §6. `AwakeningService.awaken` uses the shard's tier. Tooltip on Vessel and shard: tier name and progress.
- Pass the tier to the vision as an int on `JobService.AwakeningResult` (add a field) so Claude can tint the sea later; do not change the vision itself.
- Commands: `/pz vessel charge <xp>`, `/pz shard give <count> [tier]`.
- Placeholder textures for the Vessel (TODO(claude-art)).
- Test: give a Vessel + shard, kill mobs, watch the tooltip fill and XP gain halve; reroll with a tier-5 shard ~20 times via `/pz job set` + stone and note the rarity spread in HANDOFF.

## Task 2: Skills foundation (DESIGN-DOCUMENT §4)

- **Mana:** max mana from Intelligence (`StatEffects.maxMana`), current mana in player data, regen `MANA_REGEN_PER_SECOND` (start 1 + 2% of max). Synced to the client.
- **Skill definitions** as JSON in `data/projectzero/skills/*.json` (reload listener like `JobRegistry`): `id`, `type` (`active`/`passive`), `source` (`basic`/`job`/`milestone`/`skillbook`), `job` (optional), `mana_cost`, `cooldown_ticks`, `unlock_level`, `behavior` (id of a Java `SkillBehavior` in a registry map), `params` (free JSON object).
- **PlayerSkills** (NBT `projectzero:skills`): learned skill ids, 4 active slots, cooldown end times. Copied on clone. Respect the job reroll hook: implement `JobService.retainRandomJobSkillsOnReroll` (keep 3 random skills of the old job, remove the rest of that job).
- **Keys:** 4 skill keys (default Z, X, C, V; category "Project Zero"), C2S `CastSkillPacket(slot)`; the server checks learned, slot, mana, cooldown, then runs the behavior.
- **Skillbook item** with a `skill_id` component: right-click learns it (consumed). `/pz skill learn|forget|list|slot <slot> <skill>|mana <amount>`.
- **Status window:** a SKILLS list and a plain MP line (placeholder; Claude restyles).
- **Placeholder HUD:** plain text `HP x/y  MP x/y` + slot cooldown numbers bottom-left (TODO(claude-art), Claude makes the real HUD).
- Test: learn a skill via command, slot it, cast with Z, mana drops, cooldown blocks a recast, relog keeps everything.

## Task 3: Basic skills + one signature skill per job

Behaviors in Java, numbers in JSON, vanilla particles/sounds only.
- **Basic (everyone, auto-learned at level):** lvl 3 *Dash* (short burst forward, i-frames 6 ticks), lvl 8 *Second Wind* (heal 30% over 3 s, long cooldown), lvl 15 *Focus* (passive: +5% crit chance... if crits don't exist yet, +10% damage on the first hit against a full-health mob), lvl 25 *Bloodlust* (active: +20% damage, +10% speed for 10 s).
- **One signature skill for each of the 10 jobs in `data/projectzero/jobs`**, granted when the job is assigned. Make them feel different (a ranged one, a defensive one, a summon-like one, an area one...). Pick names that fit each job; keep them original.
- Level-based grant on `PlayerLevelUpEvent`; job skill on `JobService.assign`.
- Test each one in game; list them in HANDOFF with what they do and any visual Claude should make.

## Task 4: Titles and milestones (DESIGN-DOCUMENT §7)

- `MilestoneTracker` (NBT `projectzero:milestones`): kills per entity type, total kills, boss kills, deaths, highest level, gates cleared (stub, 0 for now).
- Titles as JSON `data/projectzero/titles/*.json`: `id`, condition (`kills` of a type/tag ≥ n, `level` ≥ n, `boss_kills` ≥ n...), effect (attribute modifier list, or `behavior` id), optional `grants_skill`.
- On unlock: [System] message + title text. One equipped title at a time (`/pz title equip|list`, plus a click in the status window TITLE field to cycle).
- Ship 6 starter titles, e.g. *Wolf Slayer* (100 wolves: +10% damage to wolves), *Hundred Cuts* (100 kills: +2 Strength), *Undead Bane* (200 undead: +10% damage to undead), *Survivor* (reach level 20 without dying: +5% max health), *Giant Killer* (first boss kill: grants a skill), *Awakened One* (first reroll: +5% XP).
- Test: `/pz milestone add <stat> <n>` to trigger each.

## Task 5: Equipment and accessories (DESIGN-DOCUMENT §12, own slots)

Curios has no Forge build for 1.21.1, so **build our own slots**.
- `PlayerAccessories` (NBT `projectzero:accessories`): 8 slots: gauntlets, ring ×2, amulet, earrings, belt, charm (+1 spare "relic" slot disabled for now). Copied on clone; dropped on death only if `keepInventory` is off (same rules as armor).
- **Accessory screen:** a button on the status window opening a simple container menu (player inventory + the 8 slots). Placeholder look (TODO(claude-art)).
- `AccessoryItem` with a `slot_type`; stats in a data component: rarity, gate rank (E-S), flat stat points per stat, optional one special line (crit chance, mana regen, lifesteal, XP bonus, damage vs. tag). Rolled on drop. Tooltip in rarity color.
- Gear stats feed `StatEffects` as a separate bonus; the status window shows `STR 25 (+3)`.
- Drops via a **global loot modifier** (`data/projectzero/loot_modifiers`): vanilla hostile mobs 1.5% (Common/Uncommon, rank E), other mods' mobs 3%, gate monsters 8%, bosses 100%. Numbers in `EquipmentRules`.
- Items: iron gauntlets, copper/silver/gold rings, a bone amulet, a leather belt, a few charms. Placeholder textures (TODO(claude-art)). Worn gauntlet rendering on the arm is Claude's.
- `/pz gear roll <slot> <rarity> <rank>` gives a rolled piece.
- Test: roll pieces, equip, see stat changes, relog, die with keepInventory off/on.

## Task 6: Mob ranks and scaling infrastructure (DESIGN-DOCUMENT §9)

- `GateRank` enum E..S with health/damage multipliers (×1..×32) and level ranges.
- `MobScaling.apply(LivingEntity, GateRank, int mobLevel)`: attribute modifiers for health/damage, store rank + level on the entity (persistent data), name tag `[C · Lv 34] Zombie` (only when scaled).
- `XpRewards`: rank multiplier on XP and the level-gap rule (−5% per level beyond a 5-level gap, floor 10%).
- `/pz mob spawn <entity> <rank> [level]` for testing.
- Test: spawn the same zombie at E and S and compare hits to kill.

## Task 7 (stretch): Gate backend skeleton (DESIGN-DOCUMENT §9)

Only if everything above is pushed and tested.
- `GateBlock` + block entity storing rank, size, open time, linked dungeon slot. Placeholder look (a flat colored portal, TODO(claude-art)).
- A `projectzero:dungeon` dimension (void, like the library). Entering teleports into a placeholder stone room built by code (Claude will write the real dungeon generator), with rank-scaled gate monsters (use vanilla mobs + `MobScaling` + the `gate_monsters` tag via persistent data) and a "boss" (a scaled-up mob). Killing the boss opens the exit and closes the gate.
- Random gate spawning near players (rate in `GateRules`, off by default with a gamerule `projectzero:gateSpawning`), plus `/pz gate spawn <rank> <size>`.

## Not for you (Claude will do these)
Textures/models/animations, the real HUD and System windows' look, terrain/biomes (TerraBlender), dungeon/structure generation, villages and NPC looks, custom mob models, the Vessel's golden-sea effect in the vision.

## When you stop
Final HANDOFF entry: a list of every task with its status (done / partial / not started), commit hashes, all `TODO(claude-art)` placeholders, anything that felt unbalanced, and open questions for the owner.
