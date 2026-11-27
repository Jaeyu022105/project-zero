# AGENTS.md — Project Zero

Shared instructions for every AI agent working on this repo (Claude, Gemini, Codex, Copilot...). Agent-specific notes live in `CLAUDE.md` and `GEMINI.md`; both defer to this file.

## 1. Read first

1. `HANDOFF.md`: what exists now, known gaps, session logs.
2. `DESIGN-DOCUMENT.md`: the game design and where it is going.
3. `.agents/rules/`: code style and workflow rules.
4. `.agents/skills/`: step-by-step playbooks. Pick the one that fits your task.

## 2. Project facts

- Minecraft **1.21.1**, Forge **52.1.0**, Java **21**, official Mojang mappings. Mod id `projectzero`, package `com.projectzero`.
- Build: `gradlew build`. Play test: `gradlew runClient`. (Windows: `gradlew.bat`.)
- Owner: Jaime (GitHub `Jaeyu022105`). Repo: `Jaeyu022105/project-zero`. Working clone: `C:\Users\Jaime\Downloads\project-zero`.

## 3. Hard rules

- **No `Co-Authored-By` or AI attribution lines in commits or PRs.**
- Do not change Minecraft or Forge versions, or add a library, without asking the owner.
- Server is authoritative. Validate every client packet on the server.
- Do not copy names, characters, art or text from Solo Leveling or any existing game. Inspired by, never copied.
- Update `HANDOFF.md` (session log + any stale reference section) at the end of every task.

## 4. Roles

The owner works with several AI chats at once. One acts as **adviser/reviewer**: it critiques work and writes the exact instructions ("commands") for the builder chats. Builder chats implement. If you are a builder, follow the reviewer's instructions, but push back with reasons when something conflicts with this file or the design doc.

## 4b. Owner rule: everything is animated

Every movement of the player is animated in third **and** first person: walk, run, jump, land, crouch, idle, turn, attacks and skills. Every mob we add, in dungeons or the overworld, ships with a full movement and attack animation set and a blood type. The specifics are in `.agents/skills/game-feel/SKILL.md` §8.

## 5. Skills index (`.agents/skills/`)

| Skill | Use it when |
|---|---|
| `forge-mod-dev` | Writing or fixing any Java/Gradle code for this mod |
| `add-game-content` | Adding a job, skill, title, stat effect, mob or gate type |
| `review-and-direct` | Acting as the reviewer: critiquing work and writing instructions for other chats |
| `handoff-update` | Ending a task: updating `HANDOFF.md` and the design doc |
| `system-ui-style` | Building any screen, HUD or [System] message |
| `game-feel` | Making a skill, attack, boss move or mob animation feel weighty and readable (Souls timing + hit juice, Minecraft-safe) |

Each skill is a folder with a `SKILL.md` (YAML front matter `name` + `description`, then instructions). Claude Code and Gemini CLI both read this format.
