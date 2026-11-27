---
name: game-feel
description: Make a skill, attack, boss move or mob animation feel weighty and readable (Souls-like timing plus hit juice) without breaking the Minecraft look.
---

# Game feel: Souls timing + hit juice, the Minecraft way

Use this for any skill, weapon swing, mob attack or boss move. Two halves:
**timing** (Souls-like readability, so fights are fair and learnable) and
**juice** (what sells a hit, so it feels powerful). Then the **Minecraft
guard rails**, so it still feels like Minecraft.

Sources: the owner's "learning sets" (short how-to videos on magic attacks,
anime hits, sword swipes, impact and juice), plus FromSoftware-style design
writeups (see the end).

## 1. Every attack has five phases

| Phase | What happens | Hitbox | Rule of thumb (20 TPS) |
|---|---|---|---|
| Opening pose | Wind-up from idle. A readable silhouette and charge FX. | off | Bosses: 8 to 20 ticks |
| Signal | The last beat before the strike: the body pose plus a sound. No glows or warning symbols. | off | Signal + strike must be at least 7 ticks (~340 ms) for a fair reaction |
| Strike | Fast and snappy. It bends physics. | **on for 1 to 3 ticks** | Short and decisive |
| End pose | Hold the finish so the player can confirm it ended. | off | At least 4 ticks; 5+ if it may chain |
| Return | Back to idle. **This is the punish window.** | off | The longest phase. Scale with attack strength |

- Difficulty comes from **timing**, not damage. To make it harder, shorten the signal, use fewer or shorter returns, or add a delayed strike (hold the wind-up longer than the player expects). Never make the wind-up invisible.
- A delayed strike is fair only if the opening pose and signal are still shown.
- Combo chains cancel from the end pose. Give the last hit of a chain the biggest return.
- The **player's own** skills get a short wind-up (2 to 6 ticks). Their power comes from juice (section 3), not from long commitment. Ultimates can take longer if the player has i-frames or is rooted with a shield.

## 2. Readability rules (bosses and mobs)

- **No warning overlays.** Like vanilla mobs (the creeper hiss, the ravager roar, the warden's charge), the warning comes from the **body and sound only**: the wind-up pose, the weapon pulling back, a growl or swish. There are no red glows, glints, danger symbols or ground markers. The owner decided this to keep the Minecraft feel (2026-11-27).
- Pair **sound** with each signal. Players learn boss rhythms by ear.
- Show area attacks through the body: the boss rears up, lifts its weapon high or gathers particles into itself. You read the size of the attack from the size of the wind-up.
- Introduce patterns one at a time. Later phases remix known moves (faster, chained, mirrored) before they add new ones.
- Damage should match how scary the attack looks. A huge slam that tickles kills the menace.
- Downtime between patterns should match the punish window you give.
- Poise/stagger: the boss takes enough hits within a time window, plays its stagger animation, and opens a long punish. Reset poise on a timer. Show stagger with a visible pose and sound, not only a bar.
- Hitboxes can be a bit generous for **player** attacks (power fantasy). For enemy attacks, keep them close to what is drawn, so dodges feel earned.

## 3. Juice: what sells a hit (from the learning sets)

Build effects in this order. Each step is cheap; together they turn "it works" into "it feels good".

1. **Charge / anticipation.** Pull energy in before the release (inflow motes, Fx.inflow), a swelling core in the hand, and a slight crouch or lean back. "There's nothing there to throw" is the failure mode: never spawn a projectile out of thin air.
2. **Core.** A bright, readable centre (a hot ball, a blade glint). Use a white-hot centre with coloured edges.
3. **Trail.** Streaks behind fast things (Fx.streak). Trace the **tip** of the blade, not the hilt.
4. **Arc for swings.** A crescent mesh along the tip's path. Alpha is 0 at the start and fades in toward the leading edge; the tail fades out over 3 to 5 ticks. Tile a slash texture along it for detail.
5. **Burst on contact.** A flash plus directional sparks pointing *away* from the attacker (Fx.spark, Fx.shard).
6. **Reaction.** The victim flashes white for 1 to 2 ticks, flinches (a small knockback along the hit direction) and plays its hurt sound.
7. **Hitstop.** Freeze attacker and victim on contact. The rest of the world keeps going. Light hit: 1 to 2 ticks. Heavy: 3 to 4. Finisher: 5 to 6. Keep the tiers consistent so players feel which hit landed.
8. **Sound on the impact frame**, not on the swing start. Play one swish on release and one heavy hit on contact. A sound one tick late feels wrong.
9. **Aftermath.** Lingering embers, smoke, a scorch or crack decal for 1 to 3 s, so the player sees how big the hit was.
10. **Numbers / feedback.** Small and punchy, popping up and out. Crits are bigger and coloured. Keep them optional (config).
11. **Kill pop.** A slightly bigger burst plus a sound when a hit kills.

"Anime hit" extras for **finishers and ultimates only** (save them so they stay special):
- **Freeze on contact** for longer (6 to 10 ticks).
- **Invert / silhouette frame:** 1 to 2 ticks of a high-contrast overlay (dark screen, white shapes) right on contact.
- **Burst lines:** radial speed lines, as a screen overlay or as particles.
- **Punch-in:** a tiny FOV pinch on contact that snaps back over 4 to 6 ticks, with colour returning.

## 4. Souls-like movement, adapted

- **Commitment:** once an attack starts, the attacker cannot turn freely. Enemies track during the wind-up, then lock direction at the signal. This gives the dodge-sideways counterplay.
- **Root motion:** big attacks lunge forward during the strike (a short velocity impulse), not during the wind-up.
- **Dodge:** a short dash with i-frames for ~8 ticks, plus a recovery. Only if the owner approves a dodge key. Vanilla players can still sprint, jump, use a shield and strafe, so design bosses that are beatable with those.
- **Stamina:** do **not** add a separate stamina bar unless the owner asks. Use cooldowns and mana (already in the System) as the commitment cost.
- **Lock-on:** optional, later. A soft aim assist on skill targeting is enough for now.

## 5. Minecraft guard rails

Keep it Minecraft:
- **Blocky models.** Animate by rotating model parts (ModelPart.xRot/yRot/zRot, keyframed from a tick counter), not with smooth deforming meshes. GeckoLib is allowed later for bosses only, and only after the owner approves the dependency.
- **Pixel-art FX.** Our particles use 8x8/16x16 sprites (fx/ package). No high-res textures and no photographic smoke.
- **Small camera effects.** Shake has an amplitude of at most ~0.5° of pitch/yaw and lasts at most 6 ticks. Never shake for ordinary hits; use it for heavy and boss hits only. Add a config toggle for shake and flashes (accessibility).
- **Hitstop must not desync.** The server decides hits. The client plays the freeze visually (hold the renderer's limb/attack animation and skip particle ticks for those entities). Never actually pause the server tick.
- **Vanilla still works.** Swords, axes, shields and bows must stay valid ways to fight. Our juice adds to vanilla hits; it does not replace them.
- **Performance.** Keep each hit to at most ~40 particles, with fewer at a distance. A boss arena should stay at 60 FPS on a mid-range PC.

## 6. Forge 1.21.1 implementation notes

- **Phases:** use a server-side state machine on the mob, with `int phaseTick` and an enum `Phase {OPENING, SIGNAL, STRIKE, END, RETURN}`. Sync the current move id and phase start tick to clients with `SynchedEntityData`, so the renderer can animate the pose from `(tickCount - phaseStart + partialTick)`.
- **Hit flash:** in the entity renderer, pass an overlay that turns white for N ticks (or reuse `hurtTime` with a custom white overlay via `OverlayTexture.pack`).
- **Hitstop:**
  - Send a `SkillVisualPacket` kind `HITSTOP(ticks)` to the attacker, the victim and anyone tracking them.
  - On the client, `ClientSkillVisuals` keeps `freezeUntil` per entity id.
  - The renderer uses a frozen partialTick and walk animation for those entities.
  - Particles spawned on the impact tick still play.
- **Shake / punch-in:** `ViewportEvent.ComputeCameraAngles` (shake) and `ViewportEvent.ComputeFov` (punch-in), driven by a client timer.
- **Slash arcs:** draw in `SkillVisualRenderer` (RenderLevelStageEvent AFTER_PARTICLES). Build a triangle strip of quads along the arc, with alpha from 0 to 1 along it and fading over the lifetime. Use an additive or eyes render type.
- **Directional sparks:** spawn velocity = hit direction × 0.3 + a random cone. Use Fx.spark or Fx.shard with the skill's rgb.
- **Telegraphs:** keyframed wind-up poses plus a charge sound. An Fx.inflow pull toward the attacker is allowed, because it is part of the attack itself, not a separate warning.
- **Sound:** register our own SoundEvents in sounds.json, with separate swing, impact and charge sounds. Play impact sounds from the server on the hit tick, at the victim's position.

## 7. Checklist before calling an attack done

- [ ] All five phases exist. Signal + strike is at least 7 ticks for enemy attacks.
- [ ] The return window matches the attack's strength.
- [ ] The warning is only the wind-up pose and sound, with no warning overlays.
- [ ] There is a charge or anticipation before anything is released.
- [ ] Contact has a flash, directional sparks, victim flash and flinch, and a sound on the impact tick.
- [ ] The hitstop tier matches the hit weight (light, heavy or finisher).
- [ ] Anime extras appear only on finishers and ultimates.
- [ ] Camera shake is at most 0.5° and at most 6 ticks, and can be turned off in config.
- [ ] It still reads as Minecraft: blocky, pixel FX, vanilla weapons still matter.
- [ ] Particle budget is respected. Tested in runClient on the owner's PC.

## Sources

- Owner's learning sets (9 short videos on game juice, magic attacks, anime hits and sword swipes).
- "Anatomy of an Enemy Attack in Dark Souls 3", Game Developer: https://www.gamedeveloper.com/game-platforms/anatomy-of-an-enemy-attack-in-dark-souls-3
- "Designing for Difficulty: Readability in ARPGs", Game Developer: https://www.gamedeveloper.com/game-platforms/designing-for-difficulty-readability-in-arpgs
- Jason de Heras, thread on hit pause, slowdown and freeze: https://threadreaderapp.com/thread/1385104062157254660
