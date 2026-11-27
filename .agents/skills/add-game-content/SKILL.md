---
name: add-game-content
description: Add or change game content in Project Zero, such as a job, skill, title, milestone, stat effect, gate monster or gate type, following the design doc.
---

# Adding game content

1. **Find it in `DESIGN-DOCUMENT.md`.** If the content is not there, or contradicts it, stop and ask the owner (or note it for the reviewer). Do not invent major mechanics silently.
2. **Original names only.** Inspired by Solo Leveling and Souls games, never copied: no Sung Jinwoo, Igris, Beru, Kamish, Ashborn, no FromSoftware boss names.
3. **Numbers go in the feature's `*Rules` class**, with a comment on the intent ("about 3 hits to kill an E-rank player").
4. **Text goes in `en_us.json`.** Keys: `job.projectzero.<id>`, `skill.projectzero.<id>`, `skill.projectzero.<id>.desc`, `title.projectzero.<id>`, `title.projectzero.<id>.desc`.
5. Content-specific steps:
   - **Gate monster:** add the entity type to `data/projectzero/tags/entity_type/gate_monsters.json` so it gives ×3 XP. Bosses also go in `forge:bosses`.
   - **Our own mob (dungeon or overworld): owner rule, 2026-11-27. Every mob ships with a full animation set.** Follow `.agents/skills/game-feel/SKILL.md` §8. Give it idle, walk, run, jump or leap, land, hurt flinch, death, and every attack in five phases. Pick its blood type in `EnemyReactions.bloodOf`, and a boss kind if it is a boss. A mob that slides around in a T-pose is not done.
   - **Fixed XP mob:** `XpRewards.register(type, xp)` during common setup.
   - **Stat effect:** add to `StatEffects.apply` with a new modifier id; document it in the design doc's stats table and in the stat's `.desc` lang entry.
   - **Job / skill / title (once those systems exist):** prefer a JSON data entry over new Java. Only the behaviour of an active skill belongs in Java, referenced by id.
6. **Balance check:** write in the session log how the new thing compares to existing ones (damage, XP, rarity).
7. Update the design doc status tag (**[Planned]** → **[Built]**).
