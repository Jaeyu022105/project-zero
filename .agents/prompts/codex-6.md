# Codex task 6: companions foundation and screen (Gemini's unfinished Task 6), two stages

You are a builder agent on **Project Zero**, a Minecraft 1.21.1 Forge mod. This picks up Task 6 of `.agents/prompts/gemini-reset-backlog.md`, which Gemini never finished. Only two stages, so you shouldn't run out of budget mid-task.

## 0. Setup

Work in `C:\Users\Jaime\Downloads\pz-codex-4` (never in `C:\Users\Jaime\Downloads\project-zero`, which has Gemini's uncommitted files; don't read or copy them, start fresh):
```
cd C:\Users\Jaime\Downloads\pz-codex-4
git fetch origin
git switch -c codex/6-companions origin/codex/5-wood-sets
```
Read: `AGENTS.md`, `PROGRESS.md`, `HANDOFF.md` (reference sections + last 3 session logs), `DESIGN-DOCUMENT.md` §13, Task 6 in `.agents/prompts/gemini-reset-backlog.md` (the spec, but follow the stage split below), `.agents/rules/*.md`, skills `forge-mod-dev`, `add-game-content`, `system-ui-style`, `handoff-update`. Look at how `skills/` stores player data and syncs it (`projectzero:skills`), how `pack_howl` summons its hounds, and how `StatusScreen` adds its `GEAR` button.

## Rules

- **Commit only as the owner, with no AI lines:** `git -c user.name=Jaeyu022105 -c user.email=Jaeyu022105@users.noreply.github.com commit -m "..."`. No `Co-authored-by`, no mention of Codex or any AI.
- Push only to `codex/6-companions`. Never push to `main` or `claude/*`, never force-push.
- Build with Java 21, never commit code that doesn't build:
  ```
  $env:JAVA_HOME = "C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"
  .\gradlew.bat build --no-daemon
  ```
- Server is authoritative: every C2S packet is checked on the server (owns the companion, has the points, has the item). Every number in `CompanionRules`. All visible text translatable in `en_us.json`. No new mob models (Claude does those); placeholder art is marked `TODO(claude-art)`.

## Stage 1: roster backend

- Package `companions/`: `PlayerCompanions` (NBT `projectzero:companions`, copied on clone), `CompanionEntry` (UUID id, kind `SUMMON`/`PET`, entity type id, name, level, XP, five stats, unspent points, up to 4 gear stacks by slot name, live entity UUID when out), `CompanionRules`, `CompanionService`, `CompanionEvents`, `CompanionCommands`.
- XP: a companion gains XP when it kills something or when the owner kills something it hit recently. On level-up, 3 points per level go to **random** stats.
- Hook `pack_howl`: the first cast by a player adds a "Spirit Hound" entry; summoned hounds take their health and damage from the entry's level and stats, and their kills give XP back to it. Otherwise keep the skill as it is.
- Sync the roster to the client (`ClientCompanionsCache`, no client imports) on login, respawn, dimension change and every change.
- Commands: `/pz companion list | add <entity> <name> | xp <name> <amount>`.
**Checkpoint 1:** build, commit `feat(companions): roster, leveling and Spirit Hound hook`, push. If you've used more than about 60% of your budget, skip to "Finish".

## Stage 2: screen and Bond Crystal

- Item `bond_crystal` (placeholder texture drawn with Pillow in `tools/textures/`, recipe: mana crystal shards around an amethyst shard). Used from the screen on a companion, it refunds all spent points as unspent and is consumed.
- `CompanionsScreen` in `client/`: opened by a `SUMMONS` button in `StatusScreen`, shown only for jobs with `"companions": true` in their JSON (add the optional field to the job loader, default false, and set it on Beastcaller). Left: the roster list. Right: the selected companion's name, level, XP bar, five stats with `+` buttons, unspent points, an abilities line and 4 gear slots (show only; gear stats add to the companion's stats). System style, same look as the status window.
- C2S packets for spending a point and using a Bond Crystal, registered in `ModNetwork` (bump the protocol version), validated on the server.
**Checkpoint 2:** build, commit `feat(companions): companions screen and Bond Crystal`, push.

## Finish (always)

1. `HANDOFF.md`: append `### Session Log — 2026-11-28 (Codex 6: companions)` (changes, build result, say plainly you couldn't test in game, and give the owner a test: `/pz job set` Beastcaller, cast Pack Howl, open SUMMONS from K, `/pz companion xp`, spend points, use a Bond Crystal, relog).
2. `PROGRESS.md`: move Gemini's Task 6 to Done (or In progress if you stopped after Stage 1); update "Last updated". `DESIGN-DOCUMENT.md` §13: mark the built parts `**[Built]**`.
3. Commit `docs: handoff for companions (Codex 6)`, push. Reply with stages done, commits, build result, and roughly how much budget you have left.
