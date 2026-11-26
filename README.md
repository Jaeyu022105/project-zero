# Project Zero

A Forge mod for Minecraft **1.21.1** (Forge **52.1.0**, Java 21): a Solo Leveling style progression system,
with gates, dungeons and Souls-like bosses planned next.

## What works now (v0.1.0): leveling system

- **Levels 1 to 100** with an XP curve (`LevelingRules.xpToNextLevel`).
- **XP from kills**: every mob except players, based on max health, x2 for hostile mobs.
  Vanilla mobs x0.3, gate monsters (`projectzero:gate_monsters` tag) x3, bosses x10.
  Custom mobs can set a fixed value with `XpRewards.register(type, xp)`.
- **Progressively harder levels**: `100 + 80 * level^1.85` XP per level.
- **Level up**: title on screen, system message, full heal, **+5 ability points**.
- **Five stats** (all start at 10):
  - Strength: +0.5 attack damage per point
  - Agility: faster movement and attack speed
  - Vitality: +1 max health per point
  - Sense: 0.4% chance per point to evade an attack (max 30%)
  - Intelligence: max mana (shown now, used by skills later)
- **Status window**: press **K** to open. Spend points with the + buttons. Hover a stat to see what it does.
- Data is saved with the player and kept on death.
- Test commands (op only): `/pz xp <amount>`, `/pz info`, `/pz reset`.

## Running it

Needs a JDK 21 installed (Gradle can also download one automatically).

```
gradlew runClient      # Windows: gradlew.bat runClient
gradlew build          # jar ends up in build/libs/
```

The first run downloads Minecraft and Forge, which takes a few minutes.

## Docs

- `HANDOFF.md`: current state, known gaps, session logs. Read first.
- `DESIGN-DOCUMENT.md`: jobs, skills, titles, Awakening Stones, gates, bosses.
- `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`: rules for AI agents. Skills in `.agents/skills/`.

## Layout

```
src/main/java/com/projectzero/
  ProjectZero.java           mod entry point
  leveling/                  server-side rules, data, events, commands
    LevelingService.java     the one API other systems call (grantXp, allocate, ...)
    LevelingRules.java       all balance numbers in one place
  network/                   packets (stat sync, spend point)
  client/                    status window, key binding, client cache
```
