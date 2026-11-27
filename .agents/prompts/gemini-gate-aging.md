# Gemini task: gate aging (backend)

Read `AGENTS.md`, `GEMINI.md`, `HANDOFF.md` and **DESIGN-DOCUMENT.md §9 "Gate aging"** first. That section is the spec. The rules from `.agents/prompts/gemini-12h-backlog.md` ("Rules for every task") still apply: build with the JDK 21 path, test in a loaded world, commits authored by Jaeyu022105 with no co-author or AI lines, no new dependencies, numbers in `GateRules`.

## What to build

1. `GateRules`: `AGING_SURGE_TICKS = 7 * 24000`, `AGING_MAX_SURGES = 3`, `AGING_SURGE_MOB_LEVELS = 3`, `AGING_SURGE_LOOT_BONUS = 0.05`, `AGING_RANKUP_EXTRA_MOBS = 0.25`, gamerules `projectzero:gateAging` (default true) and `projectzero:dungeonBreaks` (default false, no behaviour yet).
2. `GateBlockEntity`: store `surges` (int) and `lastAgeCheck` (long). Add a server ticker (`EntityBlock#getTicker`) that only the anchor block runs, once every 1200 ticks: compute surges from `level.getGameTime() - openTime`, apply new surges / rank-ups.
   - A rank-up changes `GateBlock.RANK` on **every** portal block of that gate, resets surges, updates `DungeonSavedData` (record rank), and sends `setBlocksDirty`/block updates so clients redraw the ring color.
   - If the dungeon is already built: rescale living gate monsters in that slot with `MobScaling.apply` to the new rank, and spawn 25% more using the same spawn rules as `GateDungeonArena.populate` (add a small public helper there rather than copying code; keep the generator's layout untouched).
   - If not built yet: nothing else; the generator reads `rec.rank` on first entry.
3. Surges: store on the record; `GateDungeonArena`/`MobScaling` add `surges * 3` levels (capped at `rank.maxLevel()`) when spawning; chest loot bonus is just an extra roll chance on the gear/shard lines in `placeChest`.
4. Client feel (tiny hook only): sync `surges` in the block entity update tag (`getUpdateTag`/`getUpdatePacket`) so `GateRiftRenderer` can read it later. Do not change the renderer; Claude will.
5. Messages: [System] line to players within 128 blocks on a surge, title + subtitle on rank-up (see `GateService.system`).
6. Command `/pz gate age <days>`: on the gate block the player is looking at (raycast 32 blocks), move `openTime` back by `days * 24000` and run the check now.

## Test

- `/pz gate spawn e small`, look at it, `/pz gate age 7` → surge message. `/pz gate age 21` → rank-up to D, ring turns green.
- Enter it before aging, leave, age it 28 days, re-enter: monsters show higher levels, more of them.
- `/gamerule projectzero:gateAging false` stops it.
- HANDOFF entry, mark §9 "Gate aging" `[Built]`.
