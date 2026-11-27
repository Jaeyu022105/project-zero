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
- **[Built] The level ceiling** (`LevelingRules.CEILING_LEVEL = 100`, `PlayerStats.ceilingBroken`, `LevelingService.breakCeiling`; owner decision 2026-11-27): XP stops at **level 100** (`LEVEL CEILING` on the status window XP bar + one-time warning notice; temporary `MAX_LEVEL = 200` pending owner's final cap). To break the ceiling, the player takes a quest from a high-ranked NPC who lives only in an advanced civilization, completes it, and reports back to them (`LevelingService.breakCeiling` is built; the NPC quest itself is **[Planned]** in section 16). Monarch-class jobs have **no ceiling** (`"no_ceiling": true` in job JSON, section 8). Debug: `/pz ceiling status|break|reset` and `/pz level <n>`.

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

1. **Basic skills [Built]** unlock at fixed levels for everyone: lvl 3 *Dash* (burst + 6t i-frames), lvl 8 *Second Wind* (30% max HP over 3s), lvl 15 *Focus* (passive +10% damage on first hit against a full-HP foe), lvl 25 *Bloodlust* (+20% damage & +10% speed for 10s).
2. **Job skills [Built]** (starter signature skill per job, granted on assignment; deeper trees **[Planned]**).
3. **Milestone skills [Built]** come from titles and achievements (section 7; e.g. *Giant Killer* grants *Colossus Breaker*). They are unique and are never lost on a reroll.
4. **Skillbooks [Built]** (`projectzero:skillbook` with `projectzero:skill_id` data component; dungeon loot drops **[Planned]**). Using one teaches a skill. A book can teach a skill from any job, so a player can mix builds.

Skill types: **active** (bound to 4 skill keys `Z`, `X`, `C`, `V`, cost mana, have cooldowns) and **passive** (always on). **[Built]** (Skill leveling with use **[Idea]**.)

Up to 4 active skill slots on keys, more slots as the player levels. **[Idea]**

**[Built] Skills window and presets (owner idea, 2026-11-27).**
- Open the Skills window with `J`. Clicking the skill slots in the Status window also opens it.
- Learned active skills are on the left and **3 presets** of 4 slots are on the right.
- Drag a skill onto a slot, or click the skill and then the slot.
- Dragging between slots swaps them. Dragging a slot out, or right-clicking it, clears it.
- Click a preset's header to make it live. `R` cycles to the next preset mid-fight.
- After a swap, casting is locked for 0.5 s (`SkillRules.PRESET_SWAP_LOCK_TICKS`), so swapping is a choice.
- Cooldowns belong to skills, not slots, so swapping can't dodge a cooldown.
- The HUD shows the live preset (I, II, III), and the slots drop in after a swap. Old saves move into preset I.

**[Planned] Skill workbench** (owner idea 2026-11-27): a craftable station for skills, separate from the normal crafting table. Ideas to settle when we build it: copy a known skill into a blank Skillbook (costs mana crystals and monster parts), combine or upgrade Skillbooks, and pick which skills ride along on the next reroll (section 6). Its look follows the System style: a stone desk with a floating blue window over it.

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
  - **[Planned] The player chooses the 3 skills, and they stack per job** (owner decision 2026-11-27, replaces the random pick). Before the reroll, a System window lists the current job's skills and the player picks 3 to keep. Kept skills are remembered per job. If the player rolls that job again later, they get 3 more on the next reroll, so the pile grows each time: keep 3 from Slinger, roll Slinger again later, reroll away from it and you now carry 6 Slinger skills, then 9, and so on until the job has none left to give. Kept skills from every job stay usable together, which is how long-lived players build hybrids.
- **[Built] Reawakening vision (owner idea 2026-11-26):** offering the shard plays a short vision instead of an instant reroll (`ReawakeningVision`, about 14 s). The player's eyes close (eyelid overlay), they stand on a dark mirror sea in a starry void with a sea of face-down cards drifting over the water, one card glides up to them, trembles (heartbeat), then flips to show the new job while the rest burn away. Eyes close again, open back home, and a split-second aura bursts around the player in the job's rarity color (bigger for rarer jobs, visible to everyone nearby). The reroll is saved before the vision starts, so a logout cannot lose the shard. Test with `/pz reawaken`.
- **Cost [Built]** (owner decision 2026-11-26): each reroll consumes one **Awakening Shard** on success. Shards drop from gate dungeon bosses (`GateService.onDungeonBossKilled`), plus `/pz shard give` for testing.
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

- **[Built]** The System tracks milestones (`MilestoneTracker` in `projectzero:milestones`): kill counts by mob type/tag, total kills, boss kills, deaths, highest level reached, rerolls, and gates cleared.
- **[Built]** Reaching a milestone condition in `data/projectzero/titles/*.json` grants a **title** (`[System]` message + on-screen title). A title gives flat stat bonuses, transient attribute modifiers, or combat/XP behaviors (e.g. *Wolf Slayer*: +10% damage to wolves; *Hundred Cuts*: +2 Strength; *Undead Bane*: +10% damage to undead; *Survivor*: +5% max health; *Awakened One*: +5% XP), and can unlock a **unique milestone skill** (*Giant Killer* grants *Colossus Breaker*).
- **[Built]** Only one title can be **equipped** at a time (`/pz title equip|list` or clicking the `TITLE` field in the status window to cycle); its effect applies only while equipped. Unique skills from milestones are kept permanently.
- **[Built]** Titles show on the status window (`TITLE` field, clickable with hover tooltip).

## 8. Monarch path

- **[Idea]** The very end game. A Legendary job advanced through several trials can become a **Monarch**. Monarchs get domain-style abilities (e.g. raising defeated bosses as allies for a Necromancer → Shadow Monarch path).
- **[Built]** Monarch-class jobs have no level ceiling (`"no_ceiling": true` in job JSON): they never need the ceiling-break quest (section 2).
- Kept as a long-term goal. Do not build until jobs, skills, gates and bosses exist.

## 9. Gates and dungeons

- **[Built]** Gates (`GateBlock` + `GateBlockEntity`, `/pz gate spawn <rank> <size>`, natural spawning gated by `projectzero:gateSpawning` gamerule, default `false`) are portals that spawn in the world. Two things show how dangerous a gate is:
  - **Color** sets the rank (`E..S`: blue E, green D, yellow C, orange B, purple A, **red** S).
  - **Size** (`SMALL`, `MEDIUM`, `LARGE`) sets the portal frame scale, hall size, and monster count.
- **[Built]** Entering a gate teleports the player into a linked instance slot in the `projectzero:dungeon` void dimension (`GateDungeonArena`, placeholder deepslate hall), populated with rank-scaled gate monsters (`MobScaling` + `projectzero:gate_monster` NBT) and a scaled Dungeon Boss (`projectzero:gate_boss` NBT). Killing the boss drops an **Awakening Shard** (+ guaranteed accessory via `AccessoryLootModifier`), increments `gatesCleared`, opens the Dungeon Exit portal, and seals the overworld gate. Multi-room template generation and Souls-like boss attack state machines are **[Planned]**.
- **Look [Built, Claude 2026-11-27]:** a gate is a free-floating rift: a thin bright ring in the rank color around a dark swirling core (`client/GateRiftRenderer`), no frame. Exit rifts are pale cyan.
- **Dungeons are remnants of other worlds [Built, Claude 2026-11-27]** (`GateDungeonArena`). Every dungeon is big regardless of rank: a cave system of domed chambers joined by winding tunnels, entrance cavern → chain of chambers → huge boss cavern, with side caves and treasure caves. Small is about 4 chambers (~130 blocks end to end), Medium 6, Large 8.
  - Each dungeon rolls a world, weighted by rank: Crystal Grotto, Sunken Ruins, Fungal Hollow, Frozen Tomb, Ember Depths, Bone Ossuary, Sculk Abyss, Mossy Crypt. The world sets the rock, floor, lights, flora and the broken ruins (pillars, arches, wall pieces) left by whoever lived there.
  - **Mana crystals** grow in clusters on the walls (`mana_crystal` block). Mining one drops 1-3 **Mana Crystal Shards** (Fortune works, Silk Touch gives the block) and a little XP. Uses for the shards are **[Planned]** (Gemini): vessel fuel, crafting, trading.
  - **Chests are scattered**: some chambers have one near a wall, side caves are likelier, treasure caves hold 2-3 and glitter with crystals, and the boss dais has a reward chest. Loot: vanilla dungeon table plus a chance of rolled gear, an Awakening Shard, and mana crystal shards.
  - Normal gates have a return rift in the entrance cavern (retreat allowed); **S-rank (red) gates have none** until the boss dies. The exit rift opens on the boss dais.
  - Monsters are vanilla placeholders until our own mobs exist (§10, roadmap).
- **Gate aging [Built]** (owner request 2026-11-27): the longer a gate stays open and uncleared, the stronger it gets, slowly and in steps. Numbers in `GateRules`:
  - Age is server game time since the gate opened (`GateBlockEntity.openTime`), so it only grows while the server runs. No offline growth.
  - Every **7 in-game days** uncleared (about 2 h 20 min of play) the gate **surges** once: its monsters get **+3 levels** (still capped at the rank's max level), chests roll **+5%** better, and the rift spins faster and throws more motes. Up to **3 surges** per rank.
  - After the 3rd surge plus another 7 days (**28 in-game days**, about 9 h 20 min of play) the gate **ranks up** one step (E→D→C→B→A→S): its `RANK` blockstate changes (the ring color and any ancient gate runes change via `AncientGate.setRank`), surges reset. A dungeon that was already built keeps its layout; its living monsters are rescaled to the new rank, and 25% more are added.
  - [System] message to players within 128 blocks on a surge ("The C-Rank gate grows unstable.") and a title on rank-up. Gamerule `projectzero:gateAging` (default on). Debug: `/pz gate age <days>` on the gate you are looking at.
  - An E gate needs about 140 in-game days (~47 h of play) to reach S. That is the "long, long time".
- **Dungeon breaks [Built]** (gamerule `projectzero:dungeonBreaks`, default **on**; debug: `/pz gate break` on the gate you are looking at):
  - **When:** a gate breaks at its **2nd rank-up** (56 in-game days uncleared, ~19 h of play), or, for an S gate that cannot rank up, at its next rank-up time. It only breaks while a player is within 96 blocks; otherwise it waits (no breaks in unloaded chunks).
  - **What happens:** the gate's blocks get `BROKEN=true` (the rift shakes, flickers and bleeds red; `GateRiftRenderer` draws it). Players within 128 blocks get a red title "DUNGEON BREAK" and a [System] line. Then **5 waves, one every 60 s**: `size.mobCount() / 2 + 2` monsters from the dungeon's pool (sea gates: drowned and guardians) at the gate's rank and surge level appear within 6 blocks of the rift and hunt the nearest player. The **dungeon boss walks out with the last wave**.
  - **Ending it:** kill the boss outside. That gives the normal clear rewards (shard, gear roll, `gatesCleared`) and closes the gate (ruining ancient gate stones). Monsters still inside a built dungeon are removed when the break starts ("they came out"). A broken gate cannot be entered.
  - Balance knobs in `GateRules`: `BREAK_AFTER_RANKUPS = 2`, `BREAK_WAVES = 5`, `BREAK_WAVE_TICKS = 1200`, `BREAK_PLAYER_RADIUS = 96`.
  - **[Built] Storm the gate** (`GateBlockEntity.stormable`, `GateService`; owner idea 2026-11-27): if players kill every monster from the current wave before the next wave appears, the rift steadies (`stormable = true` + `END_ROD` pulses) and can be entered. Inside, the dungeon is still populated. Clearing it from inside (killing the boss there) opens the exit rift, ends the break, and closes the gate, just like killing the boss outside. While anyone is inside, no new waves spawn outside; if everyone leaves without clearing it, the wave timer resumes from where it paused.
  - **[Built] Corruption spread** (`gates/CorruptionService`, `CorruptionSavedData`, `#projectzero:corruptible`; owner idea, 2026-11-27): during a break, the dungeon's inside spreads into the overworld around the rift (`GateDungeonArena.themePaletteFor`). Terrain turns into whatever the dungeon is made of, such as cave stone, crystals and moss, or for sea gates, flooded prismarine and wet stone. The longer the break runs, the further it creeps.
    - **Limits:** it spreads in slow steps (expanding per wave) up to a hard radius (`12..24` blocks by rank E..S in `GateRules.corruptionRadius`). Only natural terrain blocks in `#projectzero:corruptible` are converted. Player builds, containers, block entities, persistent player leaves, and unloaded chunks are never touched. It only runs while a player is nearby, like the break itself.
    - **Cleanup:** every converted block is recorded with its original state in `CorruptionSavedData`. When the boss dies and the break is contained, the corruption recedes outer-ring-first over ~30s (`CORRUPTION_RECEDE_TICKS = 600`) and the original blocks come back. Ancient gate ruins stay. If a break is never contained, the corruption stays at its cap and never grows further.
    - **Tuning:** gamerule `projectzero:breakCorruption` (default on), `GateRules` knobs (`CORRUPTION_SPREAD_INTERVAL_TICKS`, `CORRUPTION_BLOCKS_PER_STEP`, `CORRUPTION_RECEDE_TICKS`), and `/pz gate corruption grow|clear`.
- **Sea gates [Built, Claude 2026-11-27]:** gates that open over ocean water are sea gates (`GateBlock.SEA`, `DungeonRecord.sea`; `/pz gate spawn <rank> <size> sea`). The rift swirls teal. The dungeon is the same cave plan, flooded: every cave floor sits below the entrance floor and the water reaches it, so tunnels are swims and chambers have an air pocket under the dome. Seagrass, kelp and sea pickles below; crystals and chests are waterlogged. Monsters: drowned and guardians; the boss is a drowned, or an elder guardian for A and S. Entering grants 3 minutes of water breathing. The exit rift opens on an island in the boss cavern.
- **Ancient gates [Built, Claude 2026-11-27]:** some gates are old. The rift stands at ground level in a ring of carved stones that sits flush against the glowing ring, its lower arc sunk into the earth. Around it lies worn paving, quarter-block plates near the ring and eighth-block plates at the edge, much of it missing, with a broken pillar stub here and there (owner, 2026-11-27: subtle, not a pedestal). The stones are not full blocks: each one is the slice of a circular band that falls in its cell, so the ring reads as a real circle. Runes on the stones and on floor plates beside the rift glow in the gate's rank color; the crown has a keystone with a sigil. About a quarter of the upper stones have already fallen and lie around as rubble. The structure is dressed for its biome: jungle (mossy stone, leaf clumps on the ring, hanging vines, ferns), desert and badlands (sandstone half buried in drifted sand, dead bushes), snowy biomes (deepslate, snow layers, packed ice), swamps (mud bricks, mangrove roots, moss), forests (mossy stone, moss, ferns) and plains or hills (stone bricks, grass, cobble). **Where they come from:** rarely with the terrain (about one per 700 chunks on dry, fairly flat land; the dungeon is created the first time someone steps in), 1 in 10 natural gate spawns, and `/pz gate spawn <rank> <size> ancient`. **When cleared** the rift closes but the stones stay: the runes go dark, about a third of the ring stones fall as rubble, the paving cracks, and a dark rune seal marks where the rift hung.
- Gate monsters go in the `projectzero:gate_monsters` entity tag so they give the ×3 XP.
- Loot: skillbooks, gear, Awakening Shards, mana crystals.
- **Mob scaling [Built]** (owner decision 2026-11-26; `GateRank`, `MobScaling`, `XpRewards`). Vanilla mobs never scale: you outgrow them, like E-rank mobs. Gate monsters scale **by gate rank**; each rank doubles health, damage, and XP, and has a target player level:

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
- **Attack timing and feel follow `.agents/skills/game-feel/SKILL.md`.** Every attack has five phases: opening pose, signal, strike, end pose, return. For enemy attacks, signal + strike lasts at least 7 ticks. The return is the punish window. Make fights harder through timing, not damage.
- **No warning overlays** (owner decision, 2026-11-27). Like vanilla mobs, attacks are read from the wind-up pose and sound only. There are no glows, glints, danger symbols or ground markers.
- **Hit juice:**
  - Every hit gets a flash, directional sparks, a white flash and flinch on the victim, and a sound on the impact tick.
  - Every hit gets hitstop in tiers: light 1-2 ticks, heavy 3-4, finisher 5-6. The hitstop is client visual only.
  - Finishers and ultimates also get the "anime hit" extras: a longer freeze, an inverted frame, burst lines and an FOV punch-in.
- Camera shake is small (≤0.5°, ≤6 ticks), used only for heavy and boss hits, and can be turned off in config.
- **[Built] Boss weaknesses** (`combat/BossAffinity`):
  - Bosses have a kind and skills have an element. Skill damage on a boss is x1.5 when it is weak to the element and x0.6 when it resists. The attacker sees a [WEAK] or [RESIST] tag.
  - Elements: Physical, Sun, Storm, Hex, Soul.
  - Kinds and matchups:

    | Kind | Weak to | Resists |
    |---|---|---|
    | Undead | Sun, Soul | Hex |
    | Beast | Hex | |
    | Construct | Storm, Physical | Hex, Soul |
    | Abyssal | Sun | Hex |
    | Aquatic | Storm | Sun |
    | Infernal | Soul | Sun |

  - Our own bosses set `pz_boss_kind`. A boss is anything in `#projectzero:bosses`, `#c:bosses`, or a gate boss.
- **[Built] Enemy body reactions** (`client/EnemyReactions`):
  - Blood depends on the creature: red, rot, bone dust, goo, embers, stone, snow, void, ichor or leaf.
  - Blood sprays away from the hit, and death gives a bigger burst. A `gore` client option hides the red blood and rot.
  - Skill marks linger on the body: burn, storm sparks, hex aura, soul wisps. Bosses get a third of the particles for a third of the time.
  - Our own mobs should pick a blood type when we add them.
- **[Built] Player animations** (`client/anim`):
  - These are our own code; there is no animation library.
  - Skills play keyframed body poses: wind-up, strike, end pose.
  - Sword and axe swings become a diagonal slice with a faint swipe arc.
  - Sprinting adds a slight forward lean.
  - First person moves the held item to match.
  - Every movement is animated in third and first person: stride bob and twist, sprint pump and lean, idle breathing, jump, fall, landing squash, crouch, and turn lag.
  - The sword combo is forehand, backhand, overhead chop, then thrust. It resets after 16 ticks without a click.
  - The slash trail traces the real blade tip.
  - Every item swings its own way: bare hands box (guard, jab, cross, hook, uppercut), and pickaxe, axe, shovel, hoe, trident, mace, fishing rod, thrown items and blocks each have their own motion. **Owner rule:** every new item or action gets a unique animation.
  - **Owner rule:** all our future mobs ship with full movement and attack animation sets (game-feel skill §8).
- **[Idea]** A dodge dash with i-frames needs the owner's approval. There is no separate stamina bar; cooldowns and mana are the commitment cost.

## 11. The System's feel (UI and messages)

- Blue translucent windows with cyan borders, all caps headers (status window is the reference). **[Built]**
- [System] chat lines, on-screen titles for level ups, quest windows that slide in. Quest windows **[Planned]**.
- **[Built] System notices** (`ui/SystemNotices`, `client/SystemNoticeOverlay`; owner idea 2026-11-27): small blue System panels that slide in at the **bottom right**, hold for a few seconds, then fade out. Several stack upward (at most 4), and the oldest leaves first. They're used for level ups, skills unlocked, quest updates, gate alerts (opened nearby, surge, rank up), item pickups worth noting (Shard Vessel tiers), and cooldown-ready pings (`≥10s` slotted skills). They use the same style as the status window, with a soft chime by type. Important ones (job change, dungeon break) still also get the center title. Chat lines become optional, behind `ui.noticeChatLines` in `projectzero-client.toml` (default `false`), so notices don't spam chat. Debug: `/pz notice <type> <text>`.
- **[Planned] Minimap, top right** (owner idea 2026-11-27): a round System-style map in the top-right corner, north-up with the player arrow in the middle. It shows terrain colors, nearby gates (rank-colored dots), known waypoint stones and civilizations. **Quest markers:** once you accept a quest, the map points to the next step (a marker on the target, or an arrow on the rim when it's off the map) and a one-line objective sits under it. A full-screen map on a key shows everything you've discovered. Our own code, no map library (adding one needs the owner's OK).
- Notification sound: a soft chime. Avoid vanilla sounds long term.
- Daily quest ("Preparation to Become Strong": push-ups, sit-ups, running) is a fun nod. **[Idea]**, only if the owner wants it.

## 12. Equipment and accessories

- **[Built]** (owner idea 2026-11-26; item art & worn gauntlet model **[Planned]** for Claude) Besides armor, mobs can drop **gauntlets, rings and accessories** (amulets, earrings, belts, charms). They have **stats**.
- **[Built] Slots:** gauntlets ×1, rings ×2, amulet ×1, earrings ×1, belt ×1, charms ×2 (8 active slots) + 1 spare locked Relic slot (`PlayerAccessories` in `projectzero:accessories`, opened via the `GEAR` button on the status window; dropped on death only when `keepInventory` is off).
- **[Built] Stats:** each piece stores rolled `AccessoryData` (data component): rarity, gate rank (`E → S`), flat points to the five stats, and at most one special line (`crit_chance`, `mana_regen`, `lifesteal`, `xp_bonus`, `damage_vs_undead`). Rolled when dropped, so two rings of the same name differ.
- **[Built] Rarity & Rank:** colored by rarity (`Common → Legendary`) and tagged with gate rank (`[C-Rank] Silver Ring`).
- **[Built] Drops:** via `AccessoryLootModifier` (`EquipmentRules`): vanilla hostile mobs 1.5% (Common/Uncommon, rank E), other mods' mobs 3%, gate monsters 8%, bosses 100%. Named boss **set pieces** are **[Planned]**.
- **[Built]** Stats from gear (and equipped Title) add to the status window as a separate `(+X)` (`SyncStatBonusesPacket`) so players see base vs. bonus.
- Gear later ties into item 15 (materials and crafting): materials can reforge or upgrade pieces.
- **[Planned] Backpacks** (owner idea 2026-11-27): wearable backpacks that add inventory rows, in tiers (leather satchel, hunter's pack, rank packs made from monster parts). Worn on the back and visible on the player model, opened with a key. Saved with the player. They share the chest slot problem with armor, so they get their own slot next to the accessory slots.

## 13. Summons and pets

- **[Planned]** (owner idea 2026-11-26) Two kinds of companions.
- **Summons (job-based, combat).** Summoner-type jobs (Summoner, Beastcaller, Gravewarden...) call creatures with a skill and mana. Each summon has its own level, stats and a few abilities, follows the player, fights, and returns to the player's **roster** when dismissed or killed (it comes back after a cooldown, never lost for good). Leveled summons evolve into stronger forms. This is also the road to the Monarch path (§8): a Shadow Monarch can raise defeated bosses as summons.
- **Pets (anyone, light).** Small creatures anyone can tame or hatch (eggs from gates and hidden dungeons). They follow you, give a small passive bonus (e.g. pick up XP orbs, glow in dark dungeons, warn of nearby gate monsters), and can wear one accessory. A few rare pets help a little in fights.
- Both are saved with the player, not the world, so they travel through gates and dimensions.
- Look: original creature designs only, with a System-style summon effect (a blue sigil on the ground, the creature rising out of it).
- **[Planned] Companions screen** (owner idea 2026-11-27): necromancers, summoners and beast tamers get a System-style window listing every summon and pet. Each entry shows its level, XP, stats and abilities, and what it has equipped.
- **[Planned] Companion stat points:** when a summon or pet levels up, its new stat points go to **random** stats. A craftable item (name to decide, e.g. a "Bond Crystal") used on a companion resets its points so the owner can redistribute them on the screen.
- **[Planned] Companion gear:** you can give summons and pets items, weapons and armor. Humanoid summons use normal gear slots; beasts use their own slots, such as a collar, barding or a charm. Gear shows on the model and adds to the companion's stats.

---

## 14. Technical design rules

- **Server is the source of truth.** The client only displays what the server sends. Every action the client asks for (spend a point, use a skill, reroll) is checked on the server.
- **One entry point per system.** Other systems call `LevelingService` (and later `JobService`, `SkillService`, `TitleService`) instead of editing player data directly.
- **Data-driven content where it is cheap.** Jobs, skills, titles and gate types should end up as JSON data (datapack folders) so new content does not need Java changes. Behaviour (what a skill does) stays in Java, referenced by id.
- **Balance numbers live in one place** per system (`LevelingRules` now; a config file later).
- **Player data** is stored in the player's persistent NBT under `projectzero:stats` and copied on death. If it grows, move to a Forge capability.

## 15. Biomes and world ambience

**Owner direction (2026-11-27): giant biomes, replace and improve vanilla, more extreme, huge fantasy trees, fantasy and real-life biomes.**

- **[Built] Giant biomes:** the default overworld uses the Large Biomes climate layout (`data/minecraft/worldgen/noise_settings/overworld.json`), so every biome is about 4× wider. Only new chunks change.
- **[Built] Taller, more extreme terrain:** the overworld is 448 blocks tall (y -64 to 383, `data/minecraft/dimension_type/overworld.json`). Terrain height is stretched 1.35× around the lowlands (`density_function/overworld_large_biomes/depth.json`), so plains stay near sea level, mountains climb to about 330 to 380, and oceans are deeper. The top fade moved from 240-256 up to 352-368. **Use a new world**: an old world's saved chunks don't match the new height.
- **[Built] Realistic vanilla biomes** (`tools/biomes/gen.py`, run with a folder of the vanilla 1.21.1 biome JSONs): 36 vanilla biomes are overridden (`data/minecraft/worldgen/biome/`) with ground detail. That means fallen logs (with stumps, moss and mushrooms), half-buried boulders, shrubs, undergrowth, fern carpets, wildflower clusters, dry grass, moss floors, and coarse dirt, podzol, mud or rooted dirt patches, chosen per biome. For example, forests get logs, shrubs and ferns, taigas get podzol and mossy boulders, savannas get acacia shrubs and dry grass, and beaches get driftwood. Vanilla's own features are untouched.
  - **Rule (crash guard):** never add, remove or reorder vanilla features inside a biome. Our features only go before all vanilla ones (ground patches) or after them, in the fixed order in `gen.py`. A conflicting order between any two biomes crashes world creation ("Feature order cycle found"). `gen.py` checks every biome, ours included, before writing. New biomes must keep vanilla's relative order for any vanilla features they share.

Our biomes are placed with **TerraBlender** (`worldgen/ProjectZeroRegion`, weight 30 against vanilla's 10, so about three quarters of the world is our region (owner's call after trying 60%: our biomes got too hard to find)). Inside our region the vanilla climate layout is kept, and vanilla biomes are swapped for ours, so each one sits where its vanilla cousin would. **Swap common biomes, not rare ones** (owner feedback 2026-11-27: swapping only Dark Forest, Windswept and Meadow made our biomes rare patches). Forest, Swamp and Jungle are swapped inside our region; Plains stay vanilla everywhere for villages. Each biome is data (`data/projectzero/worldgen/biome/`), with ground cover from `worldgen/ModSurfaceRules`. New biomes are tagged into `#minecraft:is_overworld` (so gates and ancient gates appear there) plus their kind (`is_forest` and so on).

- **[Built] Capwood** (giant-mushroom woods, replaces Dark Forest and Swamp in our region): **mega mushrooms** 28 to 48 blocks tall (`worldgen/MegaMushroomFeature`, 1 or 2 a chunk). They have bent, flared stems and three cap kinds: a red dome, a broad brown shelf and a violet dome (new block **Violet Cap**). Each has a glowing underside of **Glowcap Gills** (new block, light 12) with gill rings hanging below. Vanilla-size giant mushrooms grow between them. The floor is mostly mycelium with podzol and coarse dirt, with moss, ferns and small mushroom clumps. No dark oaks. Purple haze.
- **[Built] Elderwood** (elven woods, replaces Forest, Birch Forest and Old Growth Birch Forest in our region): giant **elder trees** (`worldgen/ElderTreeFeature`, about one every two chunks) 26 to 40 blocks tall. Each has a pale trunk that sways slightly, buttress roots that flare out and dive into the ground, upward-reaching branches ending in leaf clouds, a leafy crown, and glowing berry vines hanging from the canopy. Between them are tall birches, moss, ferns, lilies of the valley, alliums and bluets. Teal-green foliage, turquoise water, a pale mint haze and drifting motes.
- **[Built] Sky Pillars** (after Zhangjiajie; replaces the three Windswept biomes and Jungle, Sparse Jungle and Bamboo Jungle in our region): a crowded forest of sheer stone pillars 40 to 170 blocks tall (`worldgen/StoneSpireFeature`, 1 to 3 a chunk, mostly slender with some broad ones). Banded rock (stone, andesite, tuff, calcite), weathered outlines, mossy streaks, pines and leafy shrubs growing sideways out of cracks in the cliffs, a crowded cap of wind-bent pines and shrubs, and long vines down the cliffs. Below is a lush valley of jungle trees, pines and oaks. **Valley mist** (`client/BiomeMist`): fog closes in near the valley floor so rows of pillars fade into haze, and clears as you climb above the tops.
- **[Built] Skyfall Isles** (a rare biome of its own: replaces only Meadow in our region; villages can spawn here): a dense sky of floating islands 35 to 110 blocks up (`worldgen/FloatingIslandFeature`, 1 to 3 a chunk). Islands never hang over a neighbouring biome: the feature skips any island whose edge would cross the biome border. Each is an upside-down rocky cone with a grassy, flowered top, a skywood or two, hanging roots and glowing berry vines underneath, and sometimes a spring that pours a waterfall to the land below. The ground is lush and full of flowers.
- **[Built] World Tree** (`worldgen/WorldTreeStructure`, `WorldTreePiece`; structure set spacing 48 chunks, Elderwood only): a colossal elven tree 130 to 170 blocks tall, trunk flaring to about 26 across, giant roots arching out and diving into the ground, 14 to 20 branches spiralling up the trunk like a staircase with leaf clouds at their tips, a vast crown, and glowing berry vines. It's a structure drawn chunk by chunk from its seed, so it has no size limit. **Find one with `/locate structure projectzero:world_tree`.** Next: a hollow heart chamber, and a quest tie-in for the main path.
- **[Built] Skywood** (`ModNature`, textures from `tools/textures/nature.py`): the first tree species of our own, growing on and under the Skyfall Isles. Pale silver bark with sky-blue streaks, blue-white leaves that trail down in strands. Two shapes: a branching one with hanging leaves and a tall broad one (`configured_feature/skywood`, `skywood_tall`). Blocks: Skywood Log, Planks (4 from a log, counts as planks for sticks, tables and so on) and Leaves. **Next:** sapling, stripped log, wood, slabs, stairs, doors.
- **[Built] Roots on vanilla trees** (`worldgen/RootsDecorator`, configs from `tools/biomes/trees.py`): every vanilla tree (oak, birch, spruce, pine, dark oak, jungle, acacia, cherry, swamp oak, the mega and bee variants), natural or grown from a sapling, flares at the base and sends roots out that sink into the ground. Big 2x2 trunks get longer roots. Roots only take over dirt, sand, snow and plants.
- **[Planned] More of our own woods:** thin dead-branch blocks and more species (dead and twisted, lush and abundant) to replace vanilla trees in reworked biomes.
- **[Planned] Sky creatures for the Skyfall Isles and Sky Pillars:** **fairies** (small glowing flyers that drift around island flowers, scatter when you run at them, and may lead you to hidden things) and **harpies** (winged hunters that nest on pillar tops and isle cliffs and dive at you). Each needs its full animation set (idle hover, fly, dive or attack, hurt, death) per the animation rule.
- **[Planned] Biome roadmap** (owner approved all of them, 2026-11-27). Built in rounds. Each biome has its own features and new blocks where needed, and keeps a feature order that passes `gen.py`. When our region runs out of vanilla biomes to swap, add a second TerraBlender region.
  - **[Built] Round 3, real-world lands** (biomes made by `tools/biomes/ours.py` from vanilla templates, then checked by `gen.py`; new blocks Limestone, Baobab Log and Baobab Planks):
    - **Cloud Forest** (replaces Taiga): tall 2x2 oaks and big fancy oaks draped in vines, with hanging roots trailing from the leaves, moss ground around the trunks and buttress roots, between spruces, azalea shrubs, ferns and fallen logs. Forty extra springs a chunk on the slopes make waterfalls everywhere. Constant thick fog (`client/BiomeMist`).
    - **Baobab Savanna** (replaces Savanna): baobabs (`worldgen/BaobabFeature`), fat bottle trunks 12 to 22 tall with a flat crown of crooked branches and leaf pads, among acacias, termite mounds (`TermiteMoundFeature`), golden grass and patches of red earth.
    - **Tsingy Blade Forest** (replaces Badlands and Eroded Badlands): stands of 22 to 44 razor-thin limestone needles (`TsingyFeature`, one stand a chunk) up to about 26 tall with stone points, narrow slots between them, vines in the slots, and bare limestone ground.
    - **Karst Tower Bay** (replaces Warm and Lukewarm Ocean): limestone towers (`KarstTowerFeature`) rising 18 to 58 blocks out of emerald water, with a wave-cut notch at the waterline, sometimes a sea cave straight through, jungle shrubs on a rounded green top, and long vines. Light sea haze. Warm ocean ruins and shipwrecks still appear.
  - **[Built] Round 4, extremes** (new blocks Salt Crust and Block of Raw Mana Crystal):
    - **Salt Mirror Flats** (replaces Desert and Sunflower Plains, so it isn't tied to the far-apart hot zones; plain Plains stay vanilla): salt crust ground with hexagon ridges, and sheets of still water one block deep over every level stretch (`worldgen/SaltPoolFeature`), so the flats mirror the sky. Lonely rocks. Desert pyramids can still appear.
    - **Geyser Basin** (replaces Old Growth Pine Taiga): hot springs (`GeyserFeature`, about one every two chunks) with a pale blue centre ringed in yellow, orange and rust, on white sinter ground with sulfur patches, among the old pines. About half have a vent: a bubble column over soul sand that throws you upward. Steam wisps rise off the water and vents send up tall plumes (`client/GeyserSteam`), with a light haze.
    - **Ice Cathedral** (replaces Ice Spikes): ribbed arches of blue and packed ice 10 to 24 blocks across (`IceArchFeature`) among the spikes, frozen waterfalls hanging from their crowns, and sometimes bones and a skull frozen at an arch's foot. Snow over packed ice.
    - **Crystal Tundra** (replaces Snowy Plains): snow fields broken by outcrops of glowing mana-crystal shards (`CrystalOutcropFeature`) on calcite bases, with small mana crystals growing on them. Igloos and snowy villages can still appear.
  - **[Built] Round 5, fantasy:**
    - **Mana Wastes** (replaces Windswept Savanna): land around a long-dead gate. Ground crystallised in bands of amethyst, calcite and tuff; rocks torn loose and hanging in the air with mana crystals under them; broken gate rings standing upright in rubble; crystal bursts. Purple haze, rising portal motes (`worldgen/ManaWasteFeature`).
    - **Bloodmoon Steppe** (replaces Wooded Badlands and Savanna Plateau): dark red grass, dead and snapped trees (`DeadTreeFeature`), dead brush, under a blood-red sky with drifting red spores. Meant as a high-rank danger zone (stronger spawns: next step with the mob work).
    - **Spirit Bamboo Sea** (replaces Cherry Grove and Flower Forest, so it covers wide flat land and not just the cherry slopes): dense groves of giant bamboo (`GiantBambooFeature`), see the polish pass below. Bamboo shoots, the last cherry trees, drifting light motes. Home ground for a martial civilisation later.
    - **Sunken Ruins Marsh** (replaces Mangrove Swamp): half-drowned round towers with a loot chest at the bottom, toppled columns and leaning statues (`SunkenRuinFeature`) among the mangroves, mud ground, spore haze. Witch huts can still appear.
    - **Shattered Sky Plateau** (replaces Stony Peaks): chasms 45 to 95 deep split the plateau, following world-wide noise so they run on across chunks (`ShatteredPlateauFeature`), with broken-off slabs hovering over them.
  - **Not biomes, built with their systems:** the **Corrupted Zone** is the dungeon-break spread (section 7), made at runtime around a break and healing once it's cleared. The **Monarch's Graveyard** (colossal bones and broken weapons where a monarch fell) is a rare story structure for the main path (section 16).

- **[Next] Polish pass** (owner review 2026-11-27, overworld terraforming is otherwise done). **Owner direction 2026-11-27: polish every existing biome**, not only these two. Each gets denser, more varied features, its own ground detail and its own unique blocks (below). Bamboo and plateau are listed first because they were reviewed first:
  - **[Built] Spirit Bamboo Sea:** groves packed with 2x2 and 3x3 culms (3x3 ones rounded, with a full pale node ring every 4 to 6 blocks) plus a few young 1x1 ones, heights varying around a per-grove average, all leaning roughly the same way, side branches of bamboo fence ending in drooping leaf sprays, a leafy crown with a tapering tip. Narrow lanes wind through the biome along world-wide noise (continuous across chunks), paved with dirt path and flagstones, edged with moss, and lined with stone lanterns (walls, a lantern, a slab roof); moss patches and carpet underfoot. Culms keep off the lanes and off slopes. Biome enlarged by also replacing Flower Forest.
  - **[Built] Bloodmoon Steppe:** now also replaces Savanna Plateau (Wooded Badlands alone made it 10 to 18k blocks away), so it's no longer rare. Three dead-tree shapes (`DeadTreeFeature`): plain crooked ones, snapped stumps with the broken top lying beside them, and great dead trees (thick flared trunks 12 to 20 tall whose branches fork twice), all with roots crawling over the ground. Half-buried skeletons of something huge (`BoneRemainsFeature`): a spine, arching rib pairs (some broken) and a skull.
  - **[Built] Baobab Savanna:** kopjes (`KopjeFeature`), hills of huge rounded granite boulders piled up 6 to 15 tall with a balanced rock on top, grass and acacia tufts in the cracks.
  - **[Built] Salt Mirror Flats:** rows of raked salt cones standing in the mirror water like a drying harvest, and the odd big cone (`SaltMoundFeature`).
  - **[Built] Geyser Basin:** travertine terraces (`TravertineTerraceFeature`): cascades of 3 to 6 round, shallow pools stepping down the slope, white calcite rims streaked orange, each pool's water fully walled in.
  - **[Built] Crystal Tundra:** frozen ponds (`FrozenPondFeature`) on level ground: clear ice over water, mana crystal on the floor glowing up through the ice, snow patches on the ice and drifts piled along the shore.
  - **[Built] Mana Wastes:** blast craters (`ManaCraterFeature`): shallow bowls glassed over with tinted glass and amethyst, a thrown-out tuff lip, and a mana crystal heart budding crystals.
  - **[Built] Capwood:** fairy rings (`FairyRingFeature`): circles of small mushrooms on mycelium around a mossy middle, sometimes a half-buried shroomlight.
  - **[Built] Elderwood:** elven shrine ruins (`ElvenRuinFeature`): a ring of pale quartz and calcite columns, most broken, whole neighbours joined by curved arches, a mossy tiled floor, a fallen column drum and climbing vines.
  - **Shattered Sky Plateau:** the chasms read as a valley between pillars. Make it a flat, high table first (lift and level the ground in the biome), then cut narrower, sharper chasms with stepped ledges, fill them with low cloud and mist (`client/BiomeMist` by depth), and float more and bigger slabs, some linked by stone arches.
- **[Planned] Crater basins** (owner idea 2026-11-27): some biomes sit inside a ring of mountains, so from outside you see a wall of peaks and from the rim you look down into a bowl with the biome inside. Good fits: Spirit Bamboo Sea (a hidden valley for the martial order), Mana Wastes (the bowl the dead gate blasted out), Geyser Basin, Skyfall Isles. Built as a terrain shape per biome (raise the ground along the biome's edge, keep the inside low), read only through the biome source for anything outside the 3x3 chunks (AGENTS.md worldgen rule). One or two passes in, rim passes or a single gorge give a way in on foot.
- **[Planned] Unique blocks, plants, trees and foliage per biome** (owner direction 2026-11-27): every biome gets things that only grow or form there: at least one plant or flower, one foliage or ground cover, and where it fits one tree species and one stone or mineral. Examples: Bloodmoon Steppe gets blood grass and a red thornwood tree, Mana Wastes gets mana-glass and crystal moss, Salt Mirror Flats gets salt blocks and brine reeds, Spirit Bamboo Sea gets spirit bamboo planks and lotus pads, Capwood gets glowcap shelves. **Nothing is decoration only:** each new block is used in crafting (building sets like planks, slabs, stairs, walls; dyes; food; potions or alchemy; or a station recipe). Crafting recipes come later, together with the mob materials (section 18), so blocks get their full uses then.
- **[Planned] Rope bridges with physics** (owner idea 2026-11-27): plank-and-rope suspension bridges, like the swaying mountain bridges in China, across chasms, between Sky Pillars and in civilizations. Players can build them too: attach a rope between two anchor posts and the rope **sags** naturally (a catenary curve whose dip depends on length and slack), then lay planks along it. The bridge **sways and shakes** when you walk on it: each step sends a small bounce along the deck, running and jumping make it swing more, several players or mobs add up, and wind adds a slow sway on high crossings. Built as an entity or block-entity deck drawn by our own renderer (planks offset by the simulated rope), with the walking surface following the drawn deck so you never fall through. Rope and plank stats (length limit, how far it sags, how much it bounces) go in a `*Rules` class. Ideas for later: a bridge can snap if overloaded or cut, and bosses can fight on one.
- **[Planned] Underground biomes** (owner direction 2026-11-27). Placed as TerraBlender cave biomes (the same way as vanilla Lush Caves and Dripstone Caves), from the new deep layer at y -64 up to about y 0, plus a few placed by structure where they need to be huge. Each is a region to explore and the home or ruin of a people (section 16):
  - **Glowroot Hollows:** huge caverns lit by hanging glowing roots from the surface trees above, moss lakes, the elven underground (below Elderwood).
  - **Mycelial Deep:** cave-sized mushrooms, spore fog, the deep cousin of Capwood, home of a fungus folk.
  - **Crystal Veins:** caverns lined with mana crystal growing out of the walls, the main source of mana ore. Gate rifts open more often here.
  - **Magma Forges:** lava rivers and basalt halls, ruins of a dwarf-like forge people with great anvils and rail lines.
  - **Drowned Halls:** flooded cave cities under the Sunken Ruins Marsh, air pockets, sunken treasure.
  - **Bone Hollows:** colossal fossil ribs and skulls in the rock, the root of the Monarch's Graveyard story.
  - **The Abyss:** a rare bottomless shaft into a dark level with its own monsters, end-game only.
  Each needs its own blocks, ambient particles and sounds, and mobs with full animation sets.
- **[Built] Fireflies** (owner request 2026-11-27: dark biomes like Bloodmoon Steppe and dense woods needed night vision): a firefly bush block (`firefly_bush`) that gives soft light (level 6, shears/Silk Touch drop) and sends glowing fireflies (`FIREFLY` particle, `ambience.fireflies`) drifting and blinking around it at night (and at any time in dark biomes), scattered via Forge biome modifier through forests, swamps, Spirit Bamboo Sea, Bloodmoon Steppe, Sunken Ruins Marsh, Capwood, Elderwood, and Cloud Forest. Swarms over water at dusk (`12000..14000`). Placeholder bush texture (`TODO(claude-art)`).
- **[Built] Falling leaves** (`client/FallingLeaves`, `ManaParticle` LEAF): any leaf block with air under it sheds leaves, in every biome, so dense forests shed more than a lone tree. Each leaf takes the color the game draws that leaf block with, so it follows the biome's foliage tint, and cherry or azalea keep their own color. Leaves sway, tumble, rest on the ground for a moment, then fade. Rain shakes more loose. Client option `ambience.fallingLeaves`, and it respects the Particles video setting.
- **[Built] Leaves pulled into gates:** about half the leaves that drift within 9 blocks of an open rift get caught. They curve in with a little swirl, spin faster and shrink away into the core.

## 16. Progression and the main path

**[Planned]** (owner direction 2026-11-27): the mod should have a direction, not just random adventure. Civilizations, biomes, NPCs and quests give the player a road to follow. Gates and dungeons are the danger along that road.

- **Civilizations:** a few distinct peoples, each with its own home biome, building style, culture and NPCs. They range from small frontier camps to **advanced civilizations** (large cities with guilds, ranked hunters and rare services). Original designs only.
- **NPCs:** quest givers, traders, trainers and guild staff. **High-ranked NPCs** (top hunters, guild masters, elders) appear only in advanced civilizations. They are the gatekeepers of the late game.
- **Quests:** a main questline that leads the player from camp to city, and side quests per civilization. Quests use the System's quest windows (section 11). Rewards include XP, gear, reputation and access to new areas or services.
- **Main path outline (draft):**
  1. Awaken, level up and change job at level 10 (built).
  2. Find the first settlement and get registered as a hunter. Gates near settlements become the main source of work.
  3. Earn rank and reputation, which opens the road to the advanced civilizations.
  4. At level 100, a high-ranked NPC there gives the ceiling-break quest. Report back to them to break the level ceiling (section 2).
  5. Past the ceiling: S-rank gates, dungeon breaks that threaten cities, and the Monarch path (section 8) for the few who reach it.
- **Civilizations above and below** (owner direction 2026-11-27): each surface people has a home biome (the martial order in the Spirit Bamboo Sea, the elves in Elderwood around the World Tree, salt traders on the Salt Mirror Flats, sky folk on the Skyfall Isles), and some live underground (forge folk in the Magma Forges, fungus folk in the Mycelial Deep, the drowned city in the Drowned Halls).
- **Travel between civilizations** (owner direction 2026-11-27, first draft; superseded in part by the transport rules below): biomes are huge, so walking can't be the only way.
  - Earlier ideas kept as **[Idea]**: lift shafts from surface towns down to the underground cities, riverboats, sky ships or tamed flyers between the isles and the plateau, and **portal keys** (a key item, earned through quests or bought from a city's guild, that opens a fixed portal to a city). Some underground regions may only be reachable by a key at first.

### 16a. Civilization tiers and your start (owner direction 2026-11-27)

- Civilizations come in **tiers**, from the lowest (a frontier town) up to the highest (an advanced city). Better tiers have better NPCs, better stores and more services. The path through the tiers *is* the main path.
- **Spawn:** a new player starts in a lowest-tier civilization, in a **shabby shed**. Every house, the shed included, has a **waypoint stone** in it.
- **Awakening Stones:** every civilization has an Awakening Stone formation in its center (bigger city, bigger stone, section 6). That's where you reawaken.
- What a civilization has depends on its type and tier. Arenas, auctions and apartments are examples, not a fixed list per tier.

### 16b. NPCs (owner direction 2026-11-27)

- NPCs have **roles**, each with its own functions. Higher tiers have better versions (more stock, better work, rarer services).
  - **Blacksmith:** repairs your weapon for a fee, forges a new weapon from weapons you hand in, or enhances your weapon with materials you bring.
  - **Artisan:** crafts tools, furniture and accessories from materials.
  - **Merchant:** buys and sells goods.
  - **Realtor:** sells land plots in the civilization; in advanced civilizations also apartments.
  - **Builder:** sells building schematics and sends worker NPCs (16e).
  - **Guard:** keeps order, checks tickets at stations.
  - **Hunter's Association staff:** registration, licenses, quests, buying monster materials (16c).
  - More later (innkeeper, alchemist, trainer...).
- **Civilization-locked NPCs:** some roles exist only in certain peoples' cities. Elves in Elderwood can **enchant**; **priests** give **blessings** (timed buffs). Each people gets at least one role nobody else has, so every civilization is worth a visit.
- **Crafting stations:** besides the vanilla table, there are craftable stations for special items, for example a **Synthesizer** that fuses materials into new ones (monster parts plus mana crystals into an alloy, say). Some stations are only sold or used in certain civilizations. The skill workbench (section 4) is one of these.

### 16c. Hunter's Association (owner direction 2026-11-27)

- Every civilization has a **Hunter's Association** building. Register **inside the building** to get a **hunter license**. The license carries your hunter rank.
- **Higher rank, more benefits:** discounts with NPCs and on transport, access to harder quests, and rank-locked services.
- **Quests:** the Association hands out quests: hunts, gate clears, deliveries, escorts. Some send you somewhere you reach by **subway** (16f). Accepting a quest updates the minimap's quest marker (section 11).
- **Buying your loot:** the Association buys monster materials and mana crystals at good prices.

### 16d. Economy (owner direction 2026-11-27)

- **[Planned] Currency:** one money for everything (name to decide). You pay NPCs, stores, tickets, land and auctions with it.
- **Income:** selling monster drops pays well, and **monster crystals** (cores dropped by gate monsters, see section 18) pay the most.
- **Magic stores:** shops that sell magical items with rolled stats, like gear drops (section 12). Stock and quality grow with the civilization's tier.
- **Auctions** in bigger cities: buy rare items, put your own up for sale.
- **Arenas** in some civilizations: fights for money and rank. Conceptual for now; depends on the civilization type.

### 16e. Land, housing and builders (owner direction 2026-11-27)

- **Realtors** sell land plots inside a civilization, and **apartments** in advanced ones. You own and build on your plot.
- **Builders** sell **schematics**. Placing one on your plot shows the building as a **hologram** you can move and rotate until it doesn't overlap anything. Then you provide the listed materials, and **worker NPCs** come and build it block by block.
- Every house gets a waypoint stone.

### 16f. Transport (owner direction 2026-11-27)

| Way | Where it goes | How it runs | Unlock |
|---|---|---|---|
| **Train** | From one civilization to the next tier up | Waits for every ticket holder to board, then leaves | Lowest tier has only the train, and it's costly |
| **Subway** | Local areas inside a continent (other districts and towns away from your main line of cities) | Continuous: runs on a schedule and never waits | Bigger civilizations |
| **Plane** | Another continent | Like the train: waits for every ticket holder | Reach the highest civilization in your area; the city there has the airport |
| **Waypoint stone** | Any stone you've used before | Instant | Craft one, or use the ones in houses |

- **Riding the train:** pay at the station, the guards let you through the gate, you board and take the seat you chose. When every player who bought a ticket for that departure is aboard, the train leaves for the next-tier civilization. You can **skip the ride** (a short cutscene skip) or ride it out: sit and look out the window, or get up and walk the train.
- **Subways** look like Japanese subways: tiled stations, ticket gates, platform screen doors, **escalators**, maps on the walls.
- **Planes** work like the train with an airport instead of a station: ticket, gate, seat, then the flight (skippable).
- Hunter rank gives transport discounts (16c).
- **Waypoint stones [Planned]:** craftable and placeable, so you can return to places you've been. One in every house. **They can't be placed in a dungeon** (or any gate dimension).

### 16g. Continents (owner direction 2026-11-27)

- The world has several **continents**. **Each continent is its own dimension** (owner and coordinator agreed 2026-11-27) with its own terrain, sky and **biome set**: today's biomes are split between continents, and each continent gets new biomes over time. The normal overworld is the first continent (TerraBlender only places biomes there); every other continent is its own dimension with its own data-driven biome layout.
- You **choose which continent you spawn on**.
- Planes are the only regular way between continents. Trains and subways stay inside one.
- **5 mythical bosses per continent**, hidden: in ruins, in underground dungeons, under a civilization, under a shrine and so on. They are the hardest fights on each continent, Souls-style (section 10).

- **Open questions for the owner:** how many civilizations to start with, whether they can be hostile or at war, and whether reputation can be lost. New ones (2026-11-27): the currency's name; how many continents to start with; whether a train leaves after a time limit if a ticket holder never boards (multiplayer); whether land can be lost or taxed.

## 17. Art direction (owner direction 2026-11-27)

- **[Planned] After the biomes:** move every texture toward a more **cinematic** look and feel: richer shading and color ramps, more depth and contrast, sharper silhouettes, and lighting that sets a mood (warm sunsets, cold caves, the gate glow). Still 16x16 and readable at a distance; no copied resource packs.
- Order: new blocks first (so they set the style), then our existing blocks, then the items.

## 18. Monster materials and drops (owner direction 2026-11-27)

- **[Planned]** Our mobs drop an item plus **parts of their bodies**, and the parts are what crafting needs. A wolf type drops **teeth, claws and fur** (the most wanted parts) and **meat** you can cook and eat.
- **All parts sell** (Hunter's Association, merchants). Better and rarer parts from higher-rank monsters sell for more.
- Gate monsters also drop **monster crystals** (cores), worth the most money and used in stations like the Synthesizer.
- Recipes that use parts and biome blocks are designed together with the mobs (roadmap Phase 2 and 4).
