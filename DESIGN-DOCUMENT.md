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

All five start at 10. Points are spent in the status window (key **K**). **[Built]** Soft-cap rebalance **[Built]**.

Balance rule (owner decision 2026-11-26, "rebalance now"): every point helps, but less than the one before (**soft caps**), so all-in builds stay strong without breaking the game at level 100. `p` = points above the base 10. Most curves use `soft(p, k) = p / (1 + p / k)`. All numbers live in `LevelingRules`.

| Stat | Effect | 45 pts (all-in at lvl 10) | 100 pts | 495 pts (all-in at lvl 100) | Later |
|---|---|---|---|---|---|
| Strength | +`2% × soft(p, 200)` of **total** attack damage (weapon included, `ADD_MULTIPLIED_TOTAL`) | +73% | +133% | +285% | Heavier weapons, stagger power against bosses |
| Vitality | +`0.5 × soft(p, 300)` max health (a quarter heart per point at first) | +20 HP (10 hearts) | +38 HP | +93 HP | Stamina pool, poison and bleed resistance |
| Agility | Speed +`40% × p / (p + 60)` (never reaches +40%); attack speed +`1.5 × p / (p + 60)` | +17% speed | +25% | +36% | Dodge roll i-frames, stamina recovery |
| Sense | Evade chance `30% × p / (p + 100)` (never reaches 30%) | 9% | 15% | 25% | Crit chance, seeing hidden gate traps and boss tells |
| Intelligence | Max mana (10 per point), current MP + regen (`1 + 2%/s`) **[Built]** | | | | Skill power |

Before this rule (until 2026-11-26): Strength +0.5 flat damage per point, Vitality +1 HP, Agility +1.5% speed per point with no cap, Sense 0.4% per point capped at 30% (reached at level ~16).

**[Built]** Plain-text HP/MP + active skill cooldown HUD (`SkillHudOverlay`, placeholder for Claude's styled bars). Stamina should drive Souls-style combat later (dodging, blocking, heavy attacks).

## 4. Skills

Three sources of skills, all stored the same way (skill id in `projectzero:skills`):

1. **Basic skills [Next]** unlock at fixed levels for everyone, before a job.
2. **Job skills [Next]** unlock as the player levels inside their job (section 5).
3. **Milestone skills [Planned]** come from titles and achievements (section 7). They are unique and are never lost on a reroll.
4. **Skillbooks [Built]** (`projectzero:skillbook` with `projectzero:skill_id` data component; dungeon loot drops **[Planned]**). Using one teaches a skill. A book can teach a skill from any job, so a player can mix builds.

Skill types: **active** (bound to 4 skill keys `Z`, `X`, `C`, `V`, cost mana, have cooldowns) and **passive** (always on). **[Built]** (Skill leveling with use **[Idea]**.)

Up to 4 active skill slots on keys, more slots as the player levels. **[Idea]**

## 5. Jobs

- **[Built]** At **level 10** the System gives the player a **Job Change Quest**. Clearing it assigns a job. For now the quest completes instantly; the trial dungeon is **[Planned]**. (Solo Leveling uses level 40; 10 keeps the early game short. Tune once we can playtest.)
- **[Built] The Library domain (owner idea 2026-11-26).** The job choice is a ceremony, not a chat message. At level 10 the world fades and a "domain" unfolds around the player: a circular library (bookshelf walls rising, floating books, dark starry sky, the player frozen in the middle). Job cards orbit the player; right-clicking one takes it. Cards are **face down**: each card's job is pre-drawn with the rarity weights, so the result is still luck, but it is the player's pick. The chosen card flips to show the job and its rarity (color + glow), the others burn away, and the domain collapses back to the world.
  - Split: Claude builds the domain visuals (arena, card entities and renderer, effects); Gemini builds the server flow (start/freeze/timeout/assign, multiplayer safety).
- **[Built]** **The job is random**, weighted by rarity. The player does not choose it. Jobs are JSON data (`data/<ns>/jobs/*.json`); the data files use their own placeholder names (see `HANDOFF.md`). Rarities and draft weights:

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

- **[Built]** Small Awakening Stone: `AwakeningStoneBlock`, two blocks tall, can be placed anywhere.
- **[Built] Formations (owner idea 2026-11-26):** village stones are huge iceberg-like crystal formations built from `awakening_crystal` blocks with glowing `awakening_core` veins (`CrystalFormation`). Three sizes: **small** (~6-8 tall), **large** (~14-18), **colossal** (~26-32, with broken shards around it). Bigger villages get bigger stones. Right-clicking any part of a formation works. Test with `/pz crystal small|large|colossal`.
- Earlier note: Generation in **custom villages** (not vanilla villages) and a System confirmation window are **[Planned]**; right-clicking with an Awakening Shard rerolls directly for now.
- **Rerolling**:
  - **[Built]** Draws a new random job (different from the current one) with the same rarity weights.
  - **[Built]** Resets the player's level back to the level they first got a job (level 10 by default). XP resets to 0, stats reset to base, and stat points are refunded to `(level - 1) × 5`.
  - **[Built]** Keeps **3 random skills** from the previous job (`SkillService.retainRandomJobSkillsOnReroll`). The rest are lost. Basic skills, milestone skills and skillbook skills are always kept.
- **[Built] Reawakening vision (owner idea 2026-11-26):** offering the shard plays a short vision instead of an instant reroll (`ReawakeningVision`, about 14 s). The player's eyes close (eyelid overlay), they stand on a dark mirror sea in a starry void with a sea of face-down cards drifting over the water, one card glides up to them, trembles (heartbeat), then flips to show the new job while the rest burn away. Eyes close again, open back home, and a split-second aura bursts around the player in the job's rarity color (bigger for rarer jobs, visible to everyone nearby). The reroll is saved before the vision starts, so a logout cannot lose the shard. Test with `/pz reawaken`.
- **Cost [Built]** (owner decision 2026-11-26): each reroll consumes one **Awakening Shard** on success. Shards will only drop from gate bosses (**[Planned]**; `/pz shard give` for testing).
- **Shard Vessel: charging a shard [Built]** (owner idea 2026-11-26; Vessel art & golden vision sea **[Planned]** for Claude). A carried item that charges an Awakening Shard for a better reroll, paid for with your own XP:
  - Put a shard into the Vessel. While the Vessel is in your inventory, **half the XP from each kill goes into the shard** instead of to you (the ×0.3 / ×1 / ×3 / ×10 mob multipliers still apply, so gate monsters and bosses charge it fastest). Take the shard out at any time; the charge stays on the shard.
  - The charge fills **five tiers** (Dim, Glowing, Radiant, Blazing, Resplendent). Each tier shifts the reroll's rarity weights toward rarer jobs. Never a guarantee: even a full shard can roll Common. Starting point to tune:

    | Tier | Common | Uncommon | Rare | Epic | Legendary |
    |---|---|---|---|---|---|
    | Uncharged | 50 | 28 | 15 | 6 | 1 |
    | Dim | 44 | 30 | 17 | 7 | 2 |
    | Glowing | 38 | 31 | 20 | 9 | 2 |
    | Radiant | 31 | 31 | 23 | 12 | 3 |
    | Blazing | 25 | 30 | 26 | 15 | 4 |
    | Resplendent | 20 | 28 | 29 | 18 | 5 |

  - Tier thresholds scale with the player's level (a percentage of the XP needed for the next level), so a charge always costs real progress and cannot be farmed early and spent late.
  - Only one shard charges at a time (one Vessel per player). Charged shards stack only with shards of the same tier.
  - Look: the Vessel and shard glow brighter per tier; a charged shard's tier shows in its tooltip. In the Reawakening vision, the sea of cards takes on a warmer, golden shimmer the higher the charge.

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
- **Mob scaling [Planned]** (owner decision 2026-11-26). Vanilla mobs never scale: you outgrow them, like E-rank mobs. Gate monsters scale **by gate rank**; each rank roughly doubles health and damage, and has a target player level:

  | Rank | E | D | C | B | A | S |
  |---|---|---|---|---|---|---|
  | Health and damage | ×1 | ×2 | ×4 | ×8 | ×16 | ×32 |
  | Target player level | 1-15 | 15-30 | 30-50 | 50-70 | 70-90 | 90+ |

  - Each gate monster gets a **mob level** inside its rank's range (shown on its name tag). Bosses sit at the top of the range.
  - **Level gap XP:** a mob more than 5 levels below the player gives 5% less XP per extra level, down to 10%. Farming easy gates stops paying.
  - Tune with playtests: an on-level player with a fitting weapon should need roughly as many hits on an on-rank monster as a vanilla player needs on a vanilla mob of the same type.

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

## 12. Equipment and accessories

- **[Planned]** (owner idea 2026-11-26) Besides armor, mobs can drop **gauntlets, rings and accessories** (amulets, earrings, belts, charms). They have **stats**.
- **Slots:** gauntlets ×1, rings ×2, amulet ×1, earrings ×1, belt ×1, charm ×1. Our own slot system, saved with the player (Curios was approved, but it has no Forge build for 1.21.1).
- **Stats:** each piece gives flat points to the five stats (Strength, Agility, Vitality, Sense, Intelligence) plus at most one special line (crit chance, mana regen, lifesteal, XP bonus, damage vs. a mob type...). Rolled when dropped, so two rings of the same name can differ.
- **Rarity:** the same five rarities and colors as jobs (Common → Legendary). Rarer pieces roll more lines and bigger numbers.
- **Rank:** gate gear has the gate's rank (E → S) in its name and sets the number range. Higher-rank gates drop higher-rank gear.
- **Drops:** vanilla mobs rarely (low rank, Common/Uncommon), gate monsters more often, bosses always drop one piece. A few named **set pieces** from bosses give a bonus when worn together.
- Stats from gear add to the status window as a separate "+X" so players see base vs. gear.
- Gear later ties into item 15 (materials and crafting): materials can reforge or upgrade pieces.

## 13. Summons and pets

- **[Planned]** (owner idea 2026-11-26) Two kinds of companions.
- **Summons (job-based, combat).** Summoner-type jobs (Summoner, Beastcaller, Gravewarden...) call creatures with a skill and mana. Each summon has its own level, stats and a few abilities, follows the player, fights, and returns to the player's **roster** when dismissed or killed (it comes back after a cooldown, never lost for good). Leveled summons evolve into stronger forms. This is also the road to the Monarch path (§8): a Shadow Monarch can raise defeated bosses as summons.
- **Pets (anyone, light).** Small creatures anyone can tame or hatch (eggs from gates and hidden dungeons). They follow you, give a small passive bonus (e.g. pick up XP orbs, glow in dark dungeons, warn of nearby gate monsters), and can wear one accessory. A few rare pets help a little in fights.
- Both are saved with the player, not the world, so they travel through gates and dimensions.
- Look: original creature designs only, with a System-style summon effect (a blue sigil on the ground, the creature rising out of it).

---

## 14. Technical design rules

- **Server is the source of truth.** The client only displays what the server sends. Every action the client asks for (spend a point, use a skill, reroll) is checked on the server.
- **One entry point per system.** Other systems call `LevelingService` (and later `JobService`, `SkillService`, `TitleService`) instead of editing player data directly.
- **Data-driven content where it is cheap.** Jobs, skills, titles and gate types should end up as JSON data (datapack folders) so new content does not need Java changes. Behaviour (what a skill does) stays in Java, referenced by id.
- **Balance numbers live in one place** per system (`LevelingRules` now; a config file later).
- **Player data** is stored in the player's persistent NBT under `projectzero:stats` and copied on death. If it grows, move to a Forge capability.
