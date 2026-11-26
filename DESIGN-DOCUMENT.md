# Project Zero Design Document

The game design for Project Zero, a Forge mod for Minecraft 1.21.1. `HANDOFF.md` says what is built today; this file says what we are building toward. When the two disagree about the current state, trust `HANDOFF.md` and the code. When they disagree about direction, this file wins until the owner says otherwise.

Status tags used below: **[Built]** works in the code now, **[Next]** the next thing to build, **[Planned]** agreed but not started, **[Idea]** open, needs the owner's call.

---

## 1. Pitch

Minecraft where you are a Player in the System. Gates open across the world, monsters pour out of dungeons, and you grow by hunting them. Leveling, jobs, skills and titles work like Solo Leveling. Bosses fight like a Souls game: readable wind-ups, punishing combos, stamina and timing matter more than gear.

Three pillars:

1. **The System feels alive.** Blue windows, [System] messages, quests and notifications. The player should feel watched and rewarded by something bigger than them.
2. **Growth is earned in gates.** You can level anywhere, but gates and dungeons are where real progress happens.
3. **Bosses are fights, not damage sponges.** Telegraphed attacks, phases, punishes, and dodging that matters.

Where we branch off from Solo Leveling: **Awakening Stones** in custom villages let players reroll their job at a price (section 6).

---

## 2. Leveling

- **[Built]** Levels 1 to 100. XP to the next level is `100 + 80 × level^1.85` (`LevelingRules.xpToNextLevel`), so each level is harder than the last: about 180 XP at level 1, 5.7k at 10, 111k at 50.
- **[Built]** Every mob except players gives XP on kill, scaled by max health, ×2 for hostile mobs. Source multipliers (owner decision 2026-11-26, "all mobs, gate mobs give most"):
  - Vanilla mobs ×0.3
  - Other mods' mobs ×1
  - Gate monsters (entity tag `projectzero:gate_monsters`) ×3
  - Bosses (`forge:bosses` tag) ×10
  - Any mob can be given a fixed XP value with `XpRewards.register`.
- **[Built]** Each level gives **5 ability points**. Leveling up fully heals the player and shows a "LEVEL UP!" title and a [System] message.
- **[Idea]** Hunter rank (E to S, then National Level) shown on the status window, derived from level. Gates check rank, not level.
- **[Idea]** Level cap beyond 100 after the Monarch path (section 8).

## 3. Stats

All five start at 10. Points are spent in the status window (key **K**). **[Built]**

| Stat | Effect now | Later |
|---|---|---|
| Strength | +0.5 attack damage per point | Heavier weapons, stagger power against bosses |
| Agility | Movement and attack speed | Dodge roll i-frames, stamina recovery |
| Vitality | +1 max health per point | Stamina pool, poison and bleed resistance |
| Sense | 0.4% evade chance per point, max 30% | Crit chance, seeing hidden gate traps and boss tells |
| Intelligence | Max mana (10 per point), shown only | Mana for skills, skill power |

**[Next]** Mana and stamina bars on the HUD once skills exist. Stamina should drive Souls-style combat (dodging, blocking, heavy attacks).

## 4. Skills

Three sources of skills, all stored the same way (a skill id plus a skill level):

1. **Basic skills [Next]** unlock at fixed levels for everyone, before a job: e.g. level 5 *Sprint* (stamina dash), level 10 *Dagger Rush*-style lunge, level 15 *Bloodlust*-style fear aura. Placeholder names; final list is the owner's call.
2. **Job skills [Planned]** unlock as the player levels inside their job (section 5).
3. **Milestone skills [Planned]** come from titles and achievements (section 7). They are unique and are never lost on a reroll.
4. **Skillbooks [Planned]** drop in dungeons. Using one teaches a skill. Rarer gates drop rarer books. A book can teach a skill from any job, so a player can mix builds.

Skill types: **active** (bound to skill keys, cost mana, have cooldowns) and **passive** (always on, usually attribute modifiers). Skills level up with use, up to a max level per skill.

Up to 4 active skill slots on keys, more slots as the player levels. **[Idea]**

## 5. Jobs

- **[Planned]** At **level 10** the System gives the player a **Job Change Quest**. Clearing it assigns a job. (Solo Leveling uses level 40; 10 keeps the early game short. Tune once we can playtest.)
- **The job is random**, weighted by rarity. The player does not choose it. Rarities and draft weights:

| Rarity | Weight | Example jobs (placeholders) |
|---|---|---|
| Common | 50% | Fighter, Archer |
| Uncommon | 28% | Assassin, Tank, Healer |
| Rare | 15% | Mage, Ranger, Summoner |
| Epic | 6% | Shadow Blade, Elementalist |
| Legendary | 1% | Necromancer (path toward a Monarch) |

- Each job has its own skill tree. Job skills unlock at job levels.
- **Job advancement [Planned]**: some jobs upgrade into a stronger form after a **trial** (a special solo gate with a boss). Example: Assassin → Shadow Blade. Upgrading keeps all skills and adds new ones.
- Jobs should change *how* you fight, not only numbers. Each job needs at least one signature active skill that feels different in a boss fight.

## 6. Awakening Stones (rerolling jobs)

Our own mechanic, not in Solo Leveling.

- **[Planned]** Huge spiky blue crystal clusters that generate in a few **custom villages** (not vanilla villages). Interacting opens a System window offering a reroll.
- **Rerolling**:
  - Draws a new random job with the same rarity weights.
  - Resets the player's level back to the level they first got a job (level 10 by default). Stat points are refunded to match the new level.
  - Keeps **3 random skills** from the previous job. The rest are lost. Basic skills, milestone skills and skillbook skills are always kept.
- **Unlock conditions [Idea, owner to pick]**: rerolling should not be free. Options to choose from:
  - Requires an item that only drops from high-rank gate bosses (an "Awakening Shard").
  - Requires reaching a minimum level since the last reroll (e.g. 20 levels).
  - A cooldown in in-game days.
  - A one-time trial at the stone itself.

## 7. Titles and milestones

- **[Planned]** The System tracks milestones: kill counts by mob type, first boss kills, gates cleared by color, damage taken without dying, levels reached, and so on.
- Reaching one grants a **title**. A title either gives a passive effect (e.g. *Wolf Slayer*: +10% damage to wolves; *One Who Overcame Adversity*: +20% stats at low health) or unlocks a **unique milestone skill**.
- Only one title can be **equipped** at a time; its effect applies only while equipped. Unique skills from milestones are kept permanently.
- Titles show on the status window (the TITLE field is already there).

## 8. Monarch path

- **[Idea]** The very end game. A Legendary job advanced through several trials can become a **Monarch**. Monarchs get domain-style abilities (e.g. raising defeated bosses as allies for a Necromancer → Shadow Monarch path).
- Kept as a long-term goal. Do not build until jobs, skills, gates and bosses exist.

## 9. Gates and dungeons

- **[Planned]** Gates are portals that spawn in the world. Two things show how dangerous a gate is:
  - **Color** sets the rank. Placeholder ladder: blue E, green D, yellow C, orange B, purple A, **red** S. Red gates are special: once entered, the exit closes until the boss dies (a "red gate" emergency).
  - **Size** sets the scale: small gates are short dungeons; huge gates are big multi-floor dungeons with more loot.
- Dungeons are separate dimensions generated from room templates. Each has mobs from the gate's theme, a mini-boss, and a final boss.
- A gate that stays uncleared for too long can **break**: its monsters spill into the overworld. **[Idea]**
- Gate monsters go in the `projectzero:gate_monsters` entity tag so they give the ×3 XP.
- Loot: skillbooks, gear, Awakening Shards, mana crystals.

## 10. Bosses (Souls-like)

- **[Planned]** Bosses use a state machine of attacks with clear wind-ups, active frames and recovery windows. Players learn the patterns.
- Phases at health thresholds that add new attacks.
- Stagger/poise: enough hits in a window stagger the boss and open a punish.
- Boss arenas lock the exit until the fight ends.
- Use GeckoLib (or similar) for animated models when we get there; this adds a dependency, so confirm with the owner first.

## 11. The System's feel (UI and messages)

- Blue translucent windows with cyan borders, all caps headers (status window is the reference). **[Built]**
- [System] chat lines, on-screen titles for level ups, quest windows that slide in. Quest windows **[Planned]**.
- Notification sound: a soft chime. Avoid vanilla sounds long term.
- Daily quest ("Preparation to Become Strong": push-ups, sit-ups, running) is a fun nod. **[Idea]**, only if the owner wants it.

---

## 12. Technical design rules

- **Server is the source of truth.** The client only displays what the server sends. Every action the client asks for (spend a point, use a skill, reroll) is checked on the server.
- **One entry point per system.** Other systems call `LevelingService` (and later `JobService`, `SkillService`, `TitleService`) instead of editing player data directly.
- **Data-driven content where it is cheap.** Jobs, skills, titles and gate types should end up as JSON data (datapack folders) so new content does not need Java changes. Behaviour (what a skill does) stays in Java, referenced by id.
- **Balance numbers live in one place** per system (`LevelingRules` now; a config file later).
- **Player data** is stored in the player's persistent NBT under `projectzero:stats` and copied on death. If it grows, move to a Forge capability.
