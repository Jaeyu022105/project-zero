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

Last caught up: **2026-11-26** (first commit: v0.1.0 leveling system and project docs).

---

## Current Project Identity

**Project Zero** is a Minecraft Forge mod that turns the game into a Solo Leveling style world: the player is a Player in the System, levels up by hunting monsters, gets a random job, learns skills, earns titles, and clears gates and dungeons whose bosses fight like a Souls game.

**What Project Zero is**:
- A leveling system: levels, XP from kills, five stats, a System-style status window.
- (Planned) Jobs with rarity, job skills, basic skills, milestone titles and skills, skillbooks.
- (Planned) Awakening Stones in custom villages for job rerolls.
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
- The owner's `HANDOFF.md` from another project (Kogane), used only as the template for this file's structure.

The durable project context lives in:

- `DESIGN-DOCUMENT.md`
- `HANDOFF.md`
- `README.md`
- The source code itself

---

## Current App State

Version **0.1.0**. Implemented:

- Mod scaffold: Forge 52.1.0 for Minecraft 1.21.1, Java 21, Gradle 8.10.2 wrapper, official Mojang mappings.
- Leveling: levels 1 to 100, progressively harder XP curve, 5 ability points per level, full heal and "LEVEL UP!" title on level up.
- XP from kills: every non-player mob gives XP. Vanilla ×0.3, other mods ×1, gate monsters ×3 (`projectzero:gate_monsters` tag, empty for now), bosses ×10.
- Stats: Strength, Agility, Vitality, Sense, Intelligence, each with an effect (Intelligence only shows max mana for now).
- Status window on **K**: name, level, job (always "None"), title (always "None"), HP, MP, XP bar, stats with + buttons, available points, hover descriptions.
- Data saved with the player and kept on death.
- Operator test commands: `/pz xp <amount>`, `/pz info`, `/pz reset`.

**Not yet verified in game.** The first compile and `runClient` have not been done yet (see Known Gaps).

---

## Architecture Notes

Main stack:

- Minecraft 1.21.1, Forge 52.1.0, Java 21.
- ForgeGradle 6 (`[6.0.24,6.2)`), Gradle 8.10.2, official mappings, `reobf = false`.
- No third-party libraries yet.

Package layout (`src/main/java/com/projectzero/`):

- `ProjectZero.java`: mod entry point, `MOD_ID = "projectzero"`, `ProjectZero.id(path)` helper for resource locations, registers networking.
- `leveling/`
  - `LevelingService.java`: **the** server API for leveling. Other systems call `grantXp`, `allocate`, `reset`, `get`, `set`, `sync`. Never edit player stats directly.
  - `LevelingRules.java`: every balance number (XP curve, points per level, stat effect sizes, XP multipliers).
  - `PlayerStats.java`: plain data (level, xp, points, stats) with NBT save/load and network read/write.
  - `Stat.java`: the five stats, base value 10.
  - `StatEffects.java`: turns stats into transient attribute modifiers; evade roll; max mana.
  - `XpRewards.java`: XP per kill, `GATE_MONSTERS` tag, `register(type, xp)` for fixed values.
  - `LevelingEvents.java`: Forge event handlers (login, clone, respawn, dimension change, kill, attacked, commands).
  - `LevelingCommands.java`: `/pz` operator commands.
- `network/`
  - `ModNetwork.java`: Forge `SimpleChannel` (`projectzero:main`), protocol version 1. Bump the version when a packet's format changes.
  - `SyncStatsPacket.java`: server → client, full stats.
  - `AllocateStatPacket.java`: client → server, spend one point (server validates).
- `client/`
  - `ClientStatsCache.java`: last stats received. Has no client-only imports on purpose, so packet handlers can call it on either side.
  - `ModKeyBindings.java`: K opens the status window.
  - `StatusScreen.java`: the System-style status window.

Resources:

- `META-INF/mods.toml`, `pack.mcmeta` (pack format 34): filled from `gradle.properties` at build time.
- `assets/projectzero/lang/en_us.json`: all user-facing text that goes through translation keys.
- `data/projectzero/tags/entity_type/gate_monsters.json`: gate monster tag (empty).

---

## Data Model Notes

Player data lives in the player's persistent NBT under `projectzero:stats`:

- `level` (int, 1 to 100)
- `xp` (long, progress inside the current level)
- `points` (int, unspent ability points)
- `stats` (compound: `strength`, `agility`, `vitality`, `sense`, `intelligence`, ints ≥ 10)

It is copied to the new player entity in `PlayerEvent.Clone`, so it survives death and leaving the End. Attribute modifiers are transient and re-applied on login and respawn, so they never pile up in saves.

Planned additions (see `DESIGN-DOCUMENT.md`): job id and job level, first-job level (for rerolls), learned skills with levels, unlocked and equipped titles, milestone counters.

---

## Design Direction

- The System's look: dark navy translucent panels, cyan borders and labels, white values, yellow for things that need attention (unspent points). All caps headers. Reference: `client/StatusScreen.java`.
- [System] messages in aqua, short and a little cold, like the System in Solo Leveling.
- Progress should feel earned. Gates are the main source of XP; vanilla mobs are only a trickle.
- Combat direction is Souls-like: readable, punishing, fair.

---

## Known Gaps & What Needs Work

### Critical / Blocking

- **Never compiled.** v0.1.0 was written without access to Forge's Maven (the AI sandbox could not reach it). Run `gradlew build` and `gradlew runClient` and fix any API mismatches. Most likely spots: `ModNetwork` (Forge 1.21 `ChannelBuilder` / `messageBuilder` signatures), `StatEffects` (attribute `Holder`s and `AttributeModifier` with `ResourceLocation` ids), `StatusScreen` (`renderBackground` override).

### High Priority

- Basic skills, mana and cooldowns (`DESIGN-DOCUMENT.md` §4).
- Job change quest at level 10 and random job by rarity (§5).
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

- None in game yet. The AI sandbox had no access to `maven.minecraftforge.net` or Mojang's servers, so it could not compile. First build to run on the owner's PC.

**Remaining Recommendations**

1. Compile and fix API mismatches (see Known Gaps → Critical).
2. Playtest: `/pz xp 1000`, spend points, check health/speed/damage change, die and respawn, relog.
3. Then start jobs and basic skills (§4–5 of the design doc). Owner still needs to pick the reroll unlock condition (§6).
