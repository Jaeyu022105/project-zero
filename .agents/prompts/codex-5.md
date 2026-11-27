# Codex task 5: Skywood and Baobab building sets (only start after codex-4)

You are a builder agent on **Project Zero**, a Minecraft 1.21.1 Forge mod. Start this only after `codex-4.md` is finished and pushed, in the same worktree (`C:\Users\Jaime\Downloads\pz-codex-4`).

## 0. Setup

```
cd C:\Users\Jaime\Downloads\pz-codex-4
git fetch origin
git switch -c codex/5-wood-sets origin/codex/4-waypoints
```
Re-read `AGENTS.md`, `DESIGN-DOCUMENT.md` §15 (Skywood: "Next: sapling, stripped log, wood, slabs, stairs, doors", and the "Unique blocks... nothing is decoration only" bullet), `registry/ModNature.java`, `tools/textures/nature.py`, skills `forge-mod-dev` and `add-game-content`.

## Rules

Same as codex-4: commit only as `Jaeyu022105 <Jaeyu022105@users.noreply.github.com>` with no AI lines; push only to `codex/5-wood-sets`, never force-push; build with
```
$env:JAVA_HOME = "C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"
.\gradlew.bat build --no-daemon
```
and never commit code that doesn't build. Textures are original 16x16 art drawn by extending `tools/textures/nature.py` (no copied textures). All names in `en_us.json`.

## Stage 1: Skywood set
In `ModNature` (same pattern as the existing Skywood blocks): `stripped_skywood_log`, `skywood_wood`, `stripped_skywood_wood`, `skywood_slab`, `skywood_stairs`, `skywood_fence`, `skywood_fence_gate`, `skywood_door`, `skywood_trapdoor`, `skywood_button`, `skywood_pressure_plate`, `skywood_sapling` (grows `configured_feature/skywood` with a `TreeGrower`). For each: blockstate, block + item models, loot table, recipe (vanilla shapes), creative tab entry, lang. Axe stripping (log to stripped log, wood to stripped wood) via Forge's tool-modification event. Block and item tags: `minecraft:logs_that_burn`, `planks`, `wooden_slabs`, `wooden_stairs`, `wooden_fences`, `fence_gates`, `wooden_doors`, `wooden_trapdoors`, `wooden_buttons`, `wooden_pressure_plates`, `saplings`, `mineable/axe`. Skywood leaves drop the sapling (5%).
**Checkpoint 1:** build, commit `feat(nature): Skywood building set and sapling`, push. Past about 70% of your budget? Go to "Finish".

## Stage 2: Baobab set
The same set for Baobab (`baobab_log`, `baobab_planks` already exist): stripped log, wood, stripped wood, slab, stairs, fence, fence gate, door, trapdoor, button, pressure plate. Sapling only if a baobab tree config exists to grow; if not, skip the sapling and note it.
**Checkpoint 2:** build, commit `feat(nature): Baobab building set`, push.

## Finish
1. `HANDOFF.md`: append `### Session Log — 2026-11-28 (Codex 5: wood sets)` (changes, build result, say plainly you couldn't test in game, what to check in `runClient`: placing doors, stripping, sapling growth).
2. `PROGRESS.md` and `DESIGN-DOCUMENT.md` §15: mark the Skywood "Next" items and the Baobab set built.
3. Commit `docs: handoff for wood sets (Codex 5)`, push. Reply with stages done, commits, build result.
