# Project Zero Handoff

This is the living handoff for future AI sessions and future maintainers. Read this file first, then `DESIGN-DOCUMENT.md`, then `AGENTS.md` (and `CLAUDE.md` or `GEMINI.md` for your agent), then `README.md`, then inspect only the files needed for the current task.

After every meaningful prompt or implementation pass, update this file with:

- What changed.
- Why it changed.
- Files touched.
- What still needs work.
- Any risks, assumptions, or verification results.

**How this file is organized**

- **Reference sections (top, up to "Operating Guidance For Future AI")** describe the project *as it is now*. Keep them current: when your work makes one of them wrong, fix it in place.
- **Session logs (bottom, oldest first, newest last)** are history. Append one per task using the format `### Session Log — YYYY-MM-DD (Short Title)` with **Context & Direction Check**, **Changes Made**, **Verification Executed**, and **Remaining Recommendations**, separated by `---`. Do not rewrite old logs.
- To find unlogged work, compare `git log -1 -- HANDOFF.md` with `git log` on `main`.

Last caught up: **2026-11-26** (crystal formations, glassy crystal blocks, JobRegistry/awakeningReroll fixes).

---

## Current Project Identity

**Project Zero** is a Minecraft Forge mod that turns the game into a Solo Leveling style world: the player is a Player in the System, levels up by hunting monsters, gets a random job, learns skills, earns titles, and clears gates and dungeons whose bosses fight like a Souls game.

**What Project Zero is**:
- A leveling system: levels, XP from kills, five stats, a System-style status window.
- A job system: Job Change Quest at level 10 (auto-complete for now), weighted random jobs by rarity loaded from JSON.
- (Planned) Basic skills, job skills, milestone titles and skills, skillbooks.
- Awakening Stones & Awakening Shards: two-block crystal block, shard item, and real job reroll backend (custom village worldgen and gate boss loot drops still planned).
- (Planned) Gates and dungeons by color and size, with Souls-like bosses.
- (Long term) A Monarch path.

**What Project Zero is not** (see Exclusions section below).

---

## Exclusions & Explicit Boundaries

- **Not a copy of Solo Leveling.** It is inspired by it. Use our own names for jobs, skills, monsters and bosses. Do not copy characters (Sung Jinwoo, Igris, Beru...), art, logos or text from the manhwa or anime.
- **Not a copy of an existing game.** Souls-like means the *feel* of combat (telegraphs, punishes, stamina). Do not recreate specific bosses, areas or assets from FromSoftware games.
- **Forge only, Minecraft 1.21.1 only.** No Fabric, NeoForge or multi-loader setup unless the owner asks. Do not bump the Minecraft version without asking.
- **No new library dependencies without asking the owner** (e.g. GeckoLib, Curios). Each one is a version lock-in.
- **No co-author or AI attribution lines in commits.** The owner asked for this on 2026-11-26. Commits are made as the owner.
- **No paid services, no telemetry, no network calls** from the mod.

---

## Context Sources Used

- Owner's messages in the Claude project "Project Zero" (2026-11-26): the pitch, the leveling design, Awakening Stones, gates by color and size, the Monarch goal.
- Owner decision (2026-11-26): each job reroll at an Awakening Stone consumes one Awakening Shard, which only drops from gate bosses (`DESIGN-DOCUMENT.md` §6).
- The owner's `HANDOFF.md` from another project (Kogane), used only as the template for this file's structure.

The durable project context lives in:

- `DESIGN-DOCUMENT.md`
- `HANDOFF.md`
- `README.md`
- The source code itself

---

## Current App State

Version **0.1.0** plus the job system (no new version number yet). Implemented:

- Mod scaffold: Forge 52.1.0 for Minecraft 1.21.1, Java 21, Gradle 8.10.2 wrapper, official Mojang mappings.
- Leveling: levels 1 to 100, progressively harder XP curve, 5 ability points per level, full heal and "LEVEL UP!" title on level up.
- XP from kills: every non-player mob gives XP. Vanilla ×0.3, other mods ×1, gate monsters ×3 (`projectzero:gate_monsters` tag, empty for now), bosses ×10.
- Stats: Strength, Agility, Vitality, Sense, Intelligence, each with an effect (Intelligence only shows max mana for now).
- Jobs: at level 10 the System gives a Job Change Quest that completes on the spot (trial dungeon comes later) and rolls a random job weighted by rarity (Common 50, Uncommon 28, Rare 15, Epic 6, Legendary 1). Jobs are JSON data in `data/projectzero/jobs/`; 10 placeholder jobs, 2 per rarity. Jobs have no skills or effects yet.
- Awakening Stones & Crystal Formations: small two-block crystal (`projectzero:awakening_stone`), glassy translucent `projectzero:awakening_crystal` and glowing `projectzero:awakening_core` vein blocks (`AwakeningCrystalBlock`), procedural iceberg-like `CrystalFormation` generator (`small`, `large`, `colossal`), `projectzero:awakening_shard` item, and a Project Zero creative tab. Right-clicking any Awakening Stone or formation block with an Awakening Shard runs `JobService.awakeningReroll`: resets level to `min(currentLevel, firstJobLevel)`, XP to 0, stats to base, unspent points to `(level - 1) * 5`, draws a different random job, keeps up to 3 random old job skills (hook + TODO until skills exist), and consumes the shard in survival only on success. Right-clicking without a shard shows a touch hint with a 40-tick cooldown.
- Status window on **K**: name, level, job (name in rarity color, hover for rarity and job level), title (always "None"), HP, MP, XP bar, stats with + buttons, available points, hover descriptions.
- Data saved with the player and kept on death.
- Operator test commands: `/pz xp <amount>`, `/pz info`, `/pz reset`, `/pz job set <id>`, `/pz job reroll`, `/pz job clear`, `/pz shard give [count]`, `/pz crystal small|large|colossal`. (`/pz reset` resets both stats and job; `/pz job clear` clears only the job.)

**Builds and boots, in-game playtest pending.** `gradlew build` passes and `gradlew runClient` boots cleanly to the title screen (`[System] Project Zero loaded. Welcome, Player.`); in-world gameplay test is still to do (see Known Gaps).

---

## Architecture Notes

Main stack:

- Minecraft 1.21.1, Forge 52.1.0, Java 21.
- ForgeGradle 6 (`[6.0.24,6.2)`), Gradle 8.10.2, official mappings, `reobf = false`.
- No third-party libraries yet.

Package layout (`src/main/java/com/projectzero/`):

- `ProjectZero.java`: mod entry point, `MOD_ID = "projectzero"`, `ProjectZero.id(path)` helper for resource locations, registers `ModBlocks`, `ModItems`, `ModCreativeTabs`, and `ModNetwork`.
- `leveling/`
  - `LevelingService.java`: **the** server API for leveling. Other systems call `grantXp`, `allocate`, `reset`, `resetToLevel`, `get`, `set`, `sync`. Never edit player stats directly.
  - `LevelingRules.java`: every balance number (XP curve, points per level, stat effect sizes, XP multipliers).
  - `PlayerStats.java`: plain data (level, xp, points, stats) with `resetToLevel`, NBT save/load and network read/write.
  - `Stat.java`: the five stats, base value 10.
  - `StatEffects.java`: turns stats into transient attribute modifiers; evade roll; max mana.
  - `XpRewards.java`: XP per kill, `GATE_MONSTERS` tag, `register(type, xp)` for fixed values.
  - `LevelingEvents.java`: Forge event handlers (login, clone, respawn, dimension change, kill, attacked, commands).
  - `LevelingCommands.java`: `/pz` operator commands.
  - `PlayerLevelUpEvent.java`: posted on the Forge bus after a level up (once per XP grant). Other systems hook levels through this instead of `LevelingService` calling them.
- `jobs/`
  - `JobService.java`: **the** server API for jobs: `get`, `set`, `sync`, `tryJobChangeQuest`, `assign`, `reroll`, `awakeningReroll`, `clear`, `copy`.
  - `JobRules.java`: job change level (10), starting job level (1), reroll kept job skills (3).
  - `JobRarity.java`: rarities with draw weights and colors (chat + ARGB).
  - `Job.java`: one job definition (id, rarity, optional `upgradesTo`, optional `trial`). Lang key `job.<ns>.<path>`.
  - `JobRegistry.java`: datapack reload listener for `data/<ns>/jobs/*.json` (file name = id), weighted random draw. Server side only.
  - `PlayerJob.java`: plain data (job id, job level, first-job level) with NBT and network methods.
  - `JobEvents.java`: reload listener registration, level up -> quest, login/clone/respawn/dimension sync, commands.
  - `JobCommands.java`: `/pz job set|reroll|clear` (merged into the `/pz` node).
- `registry/`
  - `ModBlocks.java`: `AWAKENING_STONE`, `AWAKENING_CRYSTAL` (translucent), and `AWAKENING_CORE` (light 15) block registrations.
  - `ModItems.java`: `AWAKENING_STONE`, `AWAKENING_CRYSTAL`, `AWAKENING_CORE` block items and `AWAKENING_SHARD` item registrations.
  - `ModCreativeTabs.java`: `projectzero` creative tab.
- `awakening/`
  - `AwakeningStoneBlock.java`: two-block crystal block with ambient particles/chimes and right-click interaction.
  - `AwakeningCrystalBlock.java`: glassy formation block (culls internal faces like glass; glowing core variant emits sparkles/chimes); right-clicking any formation block triggers touch/awaken.
  - `CrystalFormation.java`: procedural iceberg-like crystal formation generator (`SMALL`, `LARGE`, `COLOSSAL`) with branching spikes and glowing core veins.
  - `AwakeningService.java`: handles touching (40-tick anti-spam cooldown) and offering an Awakening Shard (`touch`, `awaken`), calling `JobService.awakeningReroll` and consuming the shard only on success.
  - `AwakeningCommands.java`: `/pz shard give [count]` operator command (temporary until gate boss loot drops shards).
  - `CrystalCommands.java`: `/pz crystal small|large|colossal` operator command for spawning formations in front of the player.
  - `AwakeningEvents.java`: registers `AwakeningCommands` and `CrystalCommands` on `RegisterCommandsEvent`.
- `network/`
  - `ModNetwork.java`: Forge `SimpleChannel` (`projectzero:main`), protocol version 2. Bump the version when a packet's format changes.
  - `SyncStatsPacket.java`: server → client, full stats.
  - `AllocateStatPacket.java`: client → server, spend one point (server validates).
  - `SyncJobPacket.java`: server → client, the player's job plus its rarity (the client has no job definitions).
- `client/`
  - `ClientStatsCache.java`: last stats received. Has no client-only imports on purpose, so packet handlers can call it on either side.
  - `ClientJobCache.java`: last job and rarity received. Same rule.
  - `ClientEvents.java`: `Dist.CLIENT` Forge bus subscriber; resets `ClientStatsCache` and `ClientJobCache` on `ClientPlayerNetworkEvent.LoggingOut`.
  - `ModKeyBindings.java`: K opens the status window.
  - `StatusScreen.java`: the System-style status window.

Resources:

- `META-INF/mods.toml`, `pack.mcmeta` (pack format 34): filled from `gradle.properties` at build time.
- `assets/projectzero/lang/en_us.json`: all user-facing text that goes through translation keys.
- `assets/projectzero/blockstates/`, `models/`, `textures/`: Awakening Stone two-block crystal model, glassy animated `awakening_crystal` (`.png` + `.png.mcmeta`) and `awakening_core` block/item models/textures, and Awakening Shard item model/texture.
- `data/projectzero/tags/entity_type/gate_monsters.json`: gate monster tag (empty).
- `data/projectzero/jobs/*.json`: job definitions. Format: `{"id": "projectzero:duelist", "rarity": "uncommon", "upgradesTo": "projectzero:veilstalker", "trial": "projectzero:trial/veilstalker"}`; `upgradesTo` and `trial` are optional, `id` is only checked against the file name. Placeholder jobs: Brawler, Slinger (common); Duelist (upgrades to Veilstalker), Bulwark (uncommon); Hexblade (upgrades to Stormweaver), Beastcaller (rare); Stormweaver, Veilstalker (epic); Gravewarden, Sunbreaker (legendary).

---

## Data Model Notes

Player data lives in the player's persistent NBT under `projectzero:stats`:

- `level` (int, 1 to 100)
- `xp` (long, progress inside the current level)
- `points` (int, unspent ability points)
- `stats` (compound: `strength`, `agility`, `vitality`, `sense`, `intelligence`, ints ≥ 10)

It is copied to the new player entity in `PlayerEvent.Clone`, so it survives death and leaving the End. Attribute modifiers are transient and re-applied on login and respawn, so they never pile up in saves.

Job data lives next to it under `projectzero:job` (empty compound = no job):

- `job` (string, job id such as `projectzero:duelist`)
- `jobLevel` (int, starts at 1; no job XP yet)
- `firstJobLevel` (int, always `JobRules.JOB_CHANGE_LEVEL` = 10 for now; kept across rerolls, which will reset the player to it)

Also copied in `PlayerEvent.Clone`. If a saved job id is missing from the datapack, the player keeps it; the status window shows it in gray.

Planned additions (see `DESIGN-DOCUMENT.md`): learned skills with levels, unlocked and equipped titles, milestone counters.

---

## Design Direction

- The System's look: dark navy translucent panels, cyan borders and labels, white values, yellow for things that need attention (unspent points). All caps headers. Reference: `client/StatusScreen.java`.
- [System] messages in aqua, short and a little cold, like the System in Solo Leveling.
- Progress should feel earned. Gates are the main source of XP; vanilla mobs are only a trickle.
- Combat direction is Souls-like: readable, punishing, fair.

---

## Known Gaps & What Needs Work

### Critical / Blocking

- **Built OK and `runClient` boots cleanly; in-world playtest still pending.** `gradlew build` passed on the owner's PC on 2026-11-26 (commit `52fa9ff` and the jobs pass), using the JDK 21 that ships with the Minecraft launcher. `gradlew runClient` boots cleanly to the title screen after fixing `build.gradle`, but in-world gameplay verification (`/pz xp 20000`, K window, death/relog) has not yet been completed.

### High Priority

- Basic skills, mana and cooldowns (`DESIGN-DOCUMENT.md` §4).
- Job content: job skills, job leveling, the real Job Change Quest trial dungeon, job advancement trials (§5). The random job itself is built.
- Titles and milestone tracking (§7).

### Medium Priority

- Move `LevelingRules` numbers into a Forge config file so they can be tuned without rebuilding.
- HUD: small level / XP / mana display.
- Data storage: move from persistent NBT to a capability if player data gets large.

### Low Priority / Nice To Have

- Custom level-up sound and System chime.
- Hunter rank letter on the status window.

---

## Current Git Caveats

- One branch so far: `main`. Suggested flow once work grows: `feat/`, `fix/`, `docs/` branches merged into `main`.
- The owner keeps a working clone at `C:\Users\Jaime\Downloads\project-zero` for uncommitted work. Do not assume the GitHub copy has everything; ask before overwriting local changes there.
- Commits are authored by the owner. **No `Co-Authored-By` or AI attribution lines.**
- `run/` and `build/` are ignored. Do not commit worlds, logs or jars.

---

## Operating Guidance For Future AI

- Start by reading `HANDOFF.md`, then `DESIGN-DOCUMENT.md`, then `AGENTS.md` (and `CLAUDE.md` / `GEMINI.md` for your agent), the rules in `.agents/rules/`, and the skill in `.agents/skills/` that fits your task.
- Before committing: `gradlew build` must pass. For gameplay changes, also run `gradlew runClient` and test in a world. Use conventional commit messages (`feat:`, `fix:`, `docs:`...).
- Server is the source of truth. Validate every client request on the server.
- Put new balance numbers in the system's `*Rules` class, not inline.
- Every new user-facing string goes in `en_us.json` with a `projectzero.` key.
- When the design changes, update `DESIGN-DOCUMENT.md` in the same pass and say why in the session log.
- **Session Log Protocol (Mandatory)**: every AI session MUST read `HANDOFF.md` at startup and MUST append a dated session log below when done, recording (1) any redirection and why, (2) exact changes and files touched, (3) verification results, (4) next recommendations.

---

## Session Logs

### Session Log — 2026-11-26 (Project scaffold + leveling system v0.1.0)

**Context & Direction Check**

- Owner wants a Forge mod: Solo Leveling style progression, gates and dungeons, Souls-like bosses. Starting from scratch, leveling first.
- Chose **Minecraft 1.21.1 + Forge 52.1.0** over the newest Forge (26.1, 62.x): 26.1 is brand new and its modding API is still changing, while 1.21.1 has the most mods, docs and tutorials.
- Owner decided every mob gives XP but gate mobs give the most ("all mobs, gate mobs give most").
- Owner expanded the design: basic skills by level, milestone titles and unique skills, random jobs by rarity at a certain level, job skills, skillbooks from dungeons, progressively harder leveling, Awakening Stones for rerolls (reset to first-job level, keep 3 random job skills), job trials and upgrades, Monarch level, gates by size and color. Written into `DESIGN-DOCUMENT.md`.
- Owner asked for: code on their GitHub (`Jaeyu022105/project-zero`) with no co-author lines, a working clone in Downloads, this handoff in the Kogane handoff's structure, and a skills folder they can import into Gemini.

**Changes Made**

- Gradle/Forge scaffold: `build.gradle`, `settings.gradle`, `gradle.properties`, Gradle wrapper, `.gitignore`, `mods.toml`, `pack.mcmeta`.
- Leveling system: all files under `leveling/`, `network/`, `client/` listed in Architecture Notes.
- Lang file and `gate_monsters` tag.
- Docs: `README.md`, `HANDOFF.md`, `DESIGN-DOCUMENT.md`, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.agents/rules/code-style.md`, `.agents/rules/workflow.md`.
- Skills for AI agents (Claude and Gemini): `.agents/skills/` (see `AGENTS.md`).

**Verification Executed**

- Built OK on the owner's PC on 2026-11-26 (commit `52fa9ff`), JDK 21 from the Minecraft launcher. `runClient` not yet tested.

**Remaining Recommendations**

1. Compile and fix API mismatches (see Known Gaps → Critical).
2. Playtest: `/pz xp 1000`, spend points, check health/speed/damage change, die and respawn, relog.
3. Then start jobs and basic skills (§4–5 of the design doc).

---

### Session Log — 2026-11-26 (Gemini CLI skills setup)

**Context & Direction Check**

- Owner asked to set up the Project Zero skills from `.agents/skills/` for Gemini CLI as well (Antigravity already auto-loads `.agents/skills/`).

**Changes Made**

- Copied `.agents/skills/` into `.gemini/skills/` (all 5 skill folders: `add-game-content`, `forge-mod-dev`, `handoff-update`, `review-and-direct`, `system-ui-style`).
- Added `.gemini/` to `.gitignore` so local Gemini CLI skill copies stay untracked and do not duplicate `.agents/skills/` in Git.
- Updated `HANDOFF.md`.

**Verification Executed**

- Verified all 5 `SKILL.md` files exist under `.gemini/skills/<skill>/SKILL.md`.
- Ran `git status` and confirmed `.gemini/` is ignored and only `.gitignore` (plus `HANDOFF.md`) is modified.
- `gradlew build` / `gradlew runClient` were not run (no mod code or resource files changed).

**Remaining Recommendations**

1. Run the first `gradlew build` and `gradlew runClient` to compile v0.1.0 and fix any Forge 1.21.1 API mismatches.
2. Playtest the leveling system and then move on to basic skills and jobs (§4–5 of `DESIGN-DOCUMENT.md`).

---

### Session Log — 2026-11-26 (Job system: random job by rarity at level 10)

**Context & Direction Check**

- Reviewer's command: fix the stale build status in the handoff, then build §5 jobs: rarity enum, JSON job data, Job Change Quest at level 10 (auto-complete for now), weighted random job stored in player NBT and synced, JOB field on the status window, `/pz job set|reroll`. No Awakening Stones, no skills.
- Small additions beyond the command: `/pz job clear` (needed to re-test the level-10 quest, since `/pz reset` leaves the job alone), a hover tooltip on the JOB field (rarity + job level), and a `PlayerLevelUpEvent` so the leveling package does not call into jobs directly (titles and basic skills can hook the same event).
- On login, a player who is level 10+ with no job gets the quest then. Covers saves from before jobs existed.
- `firstJobLevel` is always the job change level (10), even if one XP grant jumps past 10, because the quest belongs to level 10.

**Changes Made**

- `HANDOFF.md`: replaced "Never compiled" (Known Gaps) and the first session log's Verification line with: built OK on owner's PC 2026-11-26 (commit `52fa9ff`), JDK 21 from the Minecraft launcher, `runClient` not yet tested. Updated Current App State, Architecture Notes, Data Model Notes, Known Gaps.
- New `jobs/` package: `JobRarity`, `Job`, `JobRegistry`, `JobRules`, `PlayerJob`, `JobService`, `JobEvents`, `JobCommands`.
- `leveling/PlayerLevelUpEvent.java` (new); `LevelingService.grantXp` posts it after a level up.
- `network/SyncJobPacket.java` (new), registered in `ModNetwork`; protocol version 1 -> 2.
- `client/ClientJobCache.java` (new); `client/StatusScreen.java` JOB field.
- `data/projectzero/jobs/*.json`: 10 placeholder jobs (listed in Architecture Notes). Duelist upgrades to Veilstalker and Hexblade to Stormweaver, to exercise the `upgradesTo`/`trial` fields; nothing reads them yet.
- `en_us.json`: job names, rarity names, `projectzero.job.none`, `projectzero.job.tooltip`.
- `DESIGN-DOCUMENT.md` §5: status tags updated.

**Verification Executed**

- `gradlew build` with `JAVA_HOME` set to the Minecraft launcher's JDK 21 (`.minecraft/runtime/java-runtime-delta`, 21.0.7): **BUILD SUCCESSFUL**.
- `gradlew runClient` **not run**, so the in-game acceptance test is still to do. Steps:
  1. New creative world with cheats. Check the log for `Loaded 10 jobs`.
  2. `/pz xp 20000` (levels 1 to 10 need about 18k XP). Expect "LEVEL UP!" then a "JOB CHANGE" title with the job name in rarity color, and three [System] lines (quest received, quest complete, your job is now X [Rarity]).
  3. K: JOB shows the name in its rarity color; hover shows rarity and job level 1.
  4. `/kill`, respawn, K: same job. Save and quit, reload: same job.
  5. `/pz job set duelist` (tab completion lists all 10), `/pz job reroll` (always a different job), `/pz job clear`, then `/pz xp 1`: quest fires again.
- Balance: weights are exactly the design doc's table. Jobs carry no stats or skills yet, so they change nothing in combat.

**Remaining Recommendations**

1. Run the in-game steps above and fix anything that breaks.
2. Basic skills, mana and cooldowns (§4), then job skills on top of them.
3. Replace placeholder job names when the owner picks final ones (rename the JSON file and the lang key together).

---

### Session Log — 2026-11-26 (Job system review fixes + runClient build fix)

**Context & Direction Check**

- Applied reviewer fixes before pushing the job system commit: `/pz reset` should also clear the job, client caches should reset on logout, and stale "owner still needs to pick reroll unlock condition" lines should be removed now that the owner chose an Awakening Shard (gate boss drop, `DESIGN-DOCUMENT.md` §6).
- Also fixed a Forge 52.1.0 `runClient` mod-loading crash discovered when launching `gradlew runClient`.

**Changes Made**

- `src/main/java/com/projectzero/leveling/LevelingCommands.java`: `/pz reset` now calls `JobService.clear(player)` after `LevelingService.reset(player)`.
- `src/main/java/com/projectzero/client/ClientEvents.java` (new): `Dist.CLIENT` Forge bus subscriber on `ClientPlayerNetworkEvent.LoggingOut` that resets `ClientStatsCache` to `new PlayerStats()` and `ClientJobCache` to `PlayerJob.NONE`.
- `build.gradle`: added `mods { projectzero { source sourceSets.main } }` under `runs.configureEach` and merged `sourcesSets/<name>` output directory so Forge 52.1.0 loads `projectzero` cleanly during `runClient`/`runServer`.
- `DESIGN-DOCUMENT.md` §6: replaced the open unlock options with the owner's decision (each reroll consumes one Awakening Shard, dropped only by gate bosses).
- `HANDOFF.md`: updated reference sections, removed the stale reroll-cost recommendation lines, and appended this session log.

**Verification Executed**

- `gradlew.bat build` with `JAVA_HOME=C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta` (JDK 21.0.7): **BUILD SUCCESSFUL**.
- `gradlew.bat runClient`: booted cleanly to the Minecraft 1.21.1 title screen with `[System] Project Zero loaded. Welcome, Player.` and zero errors in `run/logs/latest.log`. In-world commands (`/pz xp 20000`, `/pz reset`, K window, death/relog) not yet manually driven.

**Remaining Recommendations**

1. Complete the in-world playtest in `gradlew runClient` (`/pz xp 20000`, K window, `/pz job set|reroll`, `/pz reset`, death and relog).
2. Build basic skills, mana, and cooldowns (`DESIGN-DOCUMENT.md` §4), then job skills on top of them.
3. Replace placeholder job names when final names are chosen.

---

### Session Log — 2026-11-26 (Awakening Stone reroll backend)

**Context & Direction Check**

- Committed Claude's Awakening Stone block, Awakening Shard item, creative tab, models, textures, and effects first as-is (`21c87f2`) without changing visuals.
- Implemented the real Awakening Stone reroll backend (`DESIGN-DOCUMENT.md` §6): `JobService.awakeningReroll` resets level to `PlayerJob.firstJobLevel` (XP 0, base stats, `(level - 1) * 5` unspent points), draws a different random job, includes a hook + TODO to keep up to 3 random old job skills once skills exist, and consumes the Awakening Shard only on success.
- Added temporary op command `/pz shard give [count]` until gate boss loot drops Awakening Shards.

**Changes Made**

- `src/main/java/com/projectzero/leveling/PlayerStats.java`: added `PlayerStats.resetToLevel(int)`.
- `src/main/java/com/projectzero/leveling/LevelingService.java`: added `LevelingService.resetToLevel(ServerPlayer, int)`.
- `src/main/java/com/projectzero/jobs/JobRules.java`: added `REROLL_KEPT_JOB_SKILLS = 3`.
- `src/main/java/com/projectzero/jobs/JobService.java`: added `awakeningReroll(ServerPlayer)` and `retainRandomJobSkillsOnReroll(ServerPlayer, ResourceLocation)` hook with `TODO(skills)`.
- `src/main/java/com/projectzero/awakening/AwakeningService.java`: replaced `TODO(gemini)` `JobService.reroll` call with `JobService.awakeningReroll`, returning early without consuming the shard if reroll fails. Visuals/particles/sounds untouched.
- `src/main/java/com/projectzero/awakening/AwakeningCommands.java` & `AwakeningEvents.java` (new): `/pz shard give [count]` op command.
- `DESIGN-DOCUMENT.md` §6 and `HANDOFF.md`: updated status tags, reference sections, and session log.

**Verification Executed**

- `gradlew.bat build` with `JAVA_HOME=C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta` (JDK 21.0.7): **BUILD SUCCESSFUL**.
- `gradlew.bat runClient`: booted cleanly to the title screen (`[System] Project Zero loaded. Welcome, Player.`) with zero registry, blockstate, model, or texture warnings/errors in `run/logs/latest.log`.
- Manual in-world interaction test not yet driven; recommended steps:
  1. Level past 10 (`/pz xp 50000`), spend some ability points in **K**.
  2. Place an Awakening Stone from the Project Zero tab, run `/pz shard give`, switch to survival (`/gamemode survival`).
  3. Right-click the Awakening Stone with the Awakening Shard: confirm job changes to a different job, level resets to 10, XP resets to 0, stats reset to 10 with 45 unspent points, and 1 shard is consumed.
  4. Test `/pz job clear`, then right-click with a shard: confirm stone rejects and shard is NOT consumed.

**Remaining Recommendations**

1. Run the in-world Awakening Stone + job system playtest in `gradlew runClient`.
2. Build basic skills, mana, and cooldowns (`DESIGN-DOCUMENT.md` §4), then wire `JobService.retainRandomJobSkillsOnReroll` when job skills exist.
3. Add Awakening Shard drops to gate boss loot tables and custom village generation for Awakening Stones once gates/villages are built.

---

### Session Log — 2026-11-26 (Crystal formations, glassy crystal, reroll fixes)

**Context & Direction Check**

- Committed Claude's crystal formation visuals/blocks (`CrystalFormation`, `AwakeningCrystalBlock`, `awakening_crystal` glassy translucent animated texture + `awakening_core` glowing veins, `/pz crystal small|large|colossal`, touch cooldown in `AwakeningService`, Library domain & formations design notes in `DESIGN-DOCUMENT.md` §5–6) without modifying visuals.
- Applied/verified two backend fixes:
  1. `JobRegistry`: keep `GSON` declared before `INSTANCE` so `SimpleJsonResourceReloadListener` does not receive a null `Gson` on world load (crash fix).
  2. `JobService.awakeningReroll`: reset level to `Math.min(LevelingService.get(player).level(), current.firstJobLevel())` so an Awakening Stone reroll can never raise a player's level.

**Changes Made**

- `src/main/java/com/projectzero/awakening/AwakeningCrystalBlock.java`, `CrystalFormation.java`, `CrystalCommands.java` (new): crystal formation blocks, procedural `small`/`large`/`colossal` formations with branching spikes and glowing core veins, and `/pz crystal small|large|colossal`.
- `src/main/java/com/projectzero/awakening/AwakeningEvents.java`, `AwakeningService.java`: registered `CrystalCommands`; added 40-tick touch hint cooldown.
- `src/main/java/com/projectzero/registry/ModBlocks.java`, `ModItems.java`, `ModCreativeTabs.java`: registered `awakening_crystal` and `awakening_core` blocks/items and added them to the Project Zero creative tab.
- `src/main/resources/assets/projectzero/`: glassy animated `awakening_crystal.png` + `.mcmeta`, `awakening_core.png`, blockstates, block/item models, updated `awakening_stone.json` and `en_us.json`.
- `src/main/java/com/projectzero/jobs/JobRegistry.java`: `GSON` declared before `INSTANCE` (fixes world-load NPE).
- `src/main/java/com/projectzero/jobs/JobService.java`: `awakeningReroll` resets to `Math.min(currentLevel, firstJobLevel)`.
- `DESIGN-DOCUMENT.md` §5–6: added Library domain job-choice ceremony plan and crystal formations spec.
- `HANDOFF.md`: updated Current App State, Architecture Notes, and appended this session log.

**Verification Executed**

- `gradlew.bat build` with `JAVA_HOME=C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta` (JDK 21.0.7): **BUILD SUCCESSFUL**.
- `gradlew.bat runClient`: booted cleanly to the title screen (`[System] Project Zero loaded. Welcome, Player.`) with zero model/texture/registry errors.
- In-world test (`/pz crystal colossal`, `/pz xp 20000`, right-click crystal with shard) was **not executed** before committing & pushing per owner's instruction to commit & push right away.

**Remaining Recommendations**

1. Load a world in `gradlew runClient` and run `/pz crystal colossal`, `/pz xp 20000`, `/pz shard give`, and right-click the crystal formation with a shard (confirm `Loaded 10 jobs`, no crash on world load, glassy crystal visuals, and level reset `min(current, firstJobLevel)`).
2. Begin the Library domain job-choice ceremony (`DESIGN-DOCUMENT.md` §5) or basic skills/mana/cooldowns (§4).
