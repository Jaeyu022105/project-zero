# Codex task 7: interaction fixes from the owner's test, then backpacks (two stages)

You are a builder agent on **Project Zero**, a Minecraft 1.21.1 Forge mod. Two stages only.

## 0. Setup

Work in `C:\Users\Jaime\Downloads\pz-codex-4` (never in `C:\Users\Jaime\Downloads\project-zero`):
```
cd C:\Users\Jaime\Downloads\pz-codex-4
git fetch origin
git switch -c codex/7-backpacks origin/codex/6-companions
```
Read: `AGENTS.md` (especially §4b, everything is animated), `PROGRESS.md`, `HANDOFF.md` (last 3 session logs), `DESIGN-DOCUMENT.md` §12 (Backpacks) and §13, `.agents/rules/*.md`, skills `forge-mod-dev`, `game-feel`, `system-ui-style`, `handoff-update`. Look at `client/anim/` (`PlayerPoses`, `PlayerAnimEvents`), `waypoints/`, `companions/`, `client/ModKeyBindings`, `equipment/PlayerAccessories` + `AccessoryMenu`.

## Rules

- **Commit only as the owner, with no AI lines:** `git -c user.name=Jaeyu022105 -c user.email=Jaeyu022105@users.noreply.github.com commit -m "..."`. No `Co-authored-by`, no mention of Codex or any AI.
- Push only to `codex/7-backpacks`. Never push to `main` or `claude/*`, never force-push.
- Build with Java 21, never commit code that doesn't build:
  ```
  $env:JAVA_HOME = "C:\Users\Jaime\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"
  .\gradlew.bat build --no-daemon
  ```
- Server is authoritative. Numbers in `*Rules`. Text translatable in `en_us.json`. Placeholder art marked `TODO(claude-art)`. No new library (no Curios).

## Stage 1: fixes from the owner's play test

1. **Right-clicking a block plays a punch.** When the player uses (right-clicks) a block that has an interaction (waypoint stone, chest, door, crafting table, button, lever...), our animation system shows the bare-hand boxing punch. Add a separate short **"touch / interact" pose** in `PlayerPoses` (hand reaches forward, palm open, eases back; in third and first person) and play it for block and entity *use*, keeping the punch only for *attack* (left click). Held items keep their own use animations.
2. **Waypoint stone:** the first right-click attunes and then also opens the travel list right away (today it only attunes, so it looks like nothing happened). Show the "Waypoint attuned" notice as well.
3. **SUMMONS key:** a key binding (default `U`, category Project Zero) that opens `CompanionsScreen` directly for jobs with companions; for other jobs it shows a short System notice instead. Keep the button in the status window.
**Checkpoint 1:** build, commit `fix: interact pose, waypoint first use, summons key`, push. If you've used more than about 60% of your budget, skip to "Finish".

## Stage 2: backpacks (foundation)

- Items: `leather_satchel` (9 extra slots), `hunters_pack` (18), `rank_pack` (27, recipe uses `beast_pelt` and `monster_crystal_c` from the materials package). Recipes for each, placeholder textures drawn with Pillow in `tools/textures/`.
- A **backpack slot** of its own next to the accessory slots (`PlayerAccessories` or a new `PlayerBackpack` in NBT `projectzero:backpack`, copied on clone, dropped on death only without `keepInventory`). The backpack's contents live on the backpack item (item container data component), so they travel with it if dropped or traded.
- Open it with a key (default `B`) into a menu showing the backpack rows above the player inventory; shift-click moves items both ways. Server validates everything. A backpack can't go inside a backpack.
- Not now: showing it on the player model (Claude does that later; leave a `TODO(claude-art)`).
**Checkpoint 2:** build, commit `feat(equipment): backpacks with their own slot`, push.

## Finish (always)

1. `HANDOFF.md`: append `### Session Log — 2026-11-29 (Codex 7: interact pose, backpacks)` (changes, build result, say plainly you couldn't test in game, and list what the owner should check in `runClient`).
2. `PROGRESS.md` and `DESIGN-DOCUMENT.md` §12: mark what you built.
3. Commit `docs: handoff for Codex 7`, push. Reply with stages done, commits, build result and roughly how much budget is left.
