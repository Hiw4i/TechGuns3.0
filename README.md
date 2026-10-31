# TechGuns 3 — first playable slice

Private NeoForge port of [Techguns2-CE at `50f8a026`](https://github.com/TheSlize/Techguns2-CE/tree/50f8a02678415f1238f1a1aa164a949b0aab5a46) to Minecraft Java 26.3. Uses Java 25, NeoForge 26.3.0.16-beta and GeckoLib 5.5.7. The original license is kept in `THIRD_PARTY_LICENSES/Techguns2-CE-LICENSE.txt`.

## Launch

Use Java 25. From this directory:

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

The build output is `build/libs/techguns3-1.0-SNAPSHOT.jar`. To test a dedicated server, run `.\gradlew.bat runServer` in a second terminal after closing the local client world.

## Playable test

Open a creative world, search the TechGuns 3 tab, and test the pistol, AK-47, combat shotgun, minigun and Tesla Gun. Their ammunition is `pistol_magazine`, `assault_rifle_magazine`, `shotgun_rounds`, `minigun_drum` and `energy_cell` respectively. Hold LMB for the AK-47, minigun and Tesla Gun; click once for the pistol and shotgun. Hold RMB to aim (tighter spread, scope zoom on rifles). When empty, press R with matching ammunition in inventory to reload. Magazine-fed guns return an empty magazine or cell. Tesla Gun hits the first target in its path and can arc to four nearby targets. Shots originate at the barrel tip (1.12 offsets), so point-blank hits register instead of flying through the target.

## Armed NPCs (the Techguns mob pack)

Twelve profile-driven hostile NPCs — the CE roster ported as data: `zombie_soldier`, `bandit`, `army_soldier`, `commando`, `outcast`, `zombie_policeman`, `zombie_miner`, `psycho_steve`, `skeleton_soldier`, `zombie_pigman_soldier` (Nether), `super_mutant` (1.3x scale) and `zombie_farmer`. All share one entity class (`TGArmedMob`); every stat, weapon pool and ability lives in `data/techguns3/mobs/<id>.json` and hot-reloads with `/reload` — no code changes needed to rebalance.

The AI is a goal stack in `com.techguns.techguns3.ai`: burst fire with velocity lead, reaction delay, accuracy spread and reloads behind cover (`TGGunCombatGoal` + `TGCoverFinder`); flanking sidesteps; retreat below a health threshold with parting shots (`TGFleeGoal`); smart TNT breaches — charge planted at the wall face, then an 8-block retreat, plus cornered suicide rushes for profiles with `dynamite_suicide` (`TGDynamiteGoal`); wall mining that stocks the builder (`TGMineGoal`); gap bridging and stair building from mined blocks (`TGBuildGoal`); weapon scavenging — unarmed or dry NPCs pick up dropped guns and matching magazines (`TGPickupWeaponGoal`); and hearing — sprinting players carry 24 blocks, walking 12, sneaking 3, and any gunshot within 32 blocks gives the shooter away through walls (`TGHearingGoal` fed by `TGGunfireEvents`).

Global switches live in `techguns-common.toml` under `mobs.*`: master enable, AI enable, griefing (mine/build), dynamite, weapon pickup, guaranteed gun drops, a difficulty slider that scales accuracy and reaction speed, and a natural-spawn rate multiplier. Attributes are datapack-applied at spawn (registration only carries safe defaults) — the JSON is the source of truth. NPC skins are palette remaps of the bandit base; regenerate with `python tools/make_mob_skins.py`. The headless self-test is `TECHGUNS3_SELFTEST=mobs .\gradlew.bat runServer` (or `/tgtest mobs` in game): it verifies profile application, aimed fire, TNT breaching, wall mining, weapon scavenging, trench bridging and gunfire hearing, then halts the server.

Wave-2 exotics (all in the same tab): `tfg` (hold LMB to charge up to 3s — growing plasma orb, HUD charge bar with MAX mark, rising whine; release for a charge-scaled green explosion, 1..5 cells per shot), `nucleardeathray` (hold LMB for a continuous radiation beam with a rotating spiral, 1 cell per 5 ticks — same tick rhythm as the original), `gaussrifle` (bolt railgun, 90 damage, strong zoom), `rocketlauncher` (rockets; carries `rocket_nuke`/`rocket_hv` auto-load first for nuke blast + radiation or double speed), `guidedmissilelauncher` (single shot, snap lock-on) and `grimreaper` (hold LMB, 4-rockets lock-on volley, beeps while locking), `sonicshotgun` (16-blade cone, heavy knockback), `lasergun`/`laserpistol` (hitscan beams, pistol runs on `redstone_battery`), `pulserifle` (3-round burst), `vector`/`pdw` (1200/min hoses), `as50` (8x sniper; explosive/incendiary magazines auto-load first). New ammunition: `nuclear_powercell`, `rocket`, `advanced_magazine` (+`advanced_rounds`), `as50_magazine` (+incendiary/explosive, +`sniper_rounds` variants), `gauss_magazine` (+`gauss_slugs`), `redstone_battery` — all with empty returns and crafting recipes. Explosive guns damage blocks (toggle `explosionsDamageBlocks`); the TFG echo and nuke blasts are the loudest sounds in the pack.

Visuals are ported from the original assets: per-gun muzzle flashes (`muzzleflashnew_add`, minigun, blue, laserflare, teslaflare, sonicwave, nukebeamflare, tfg_flare, flamethrower), textured projectiles (`fireball`, `bioblob`, `bullet_blue`, animated `tfg_flare`, `rocket`/`rocket_nuke`/`rocket_hv` by variant), scrolling textured laser (`laser3`) and 17-frame NDR (`nukebeam`) beams, sonic shockwave rings, 16-frame explosion billboards, CE recoil curves per gun, and looped flamethrower/beam sounds with a persistent flame tongue while the flamethrower trigger is held.

## Weapon feel and effects

The held weapon is a view model with its own animation state, not a static prop:

- The muzzle flash is drawn **on the weapon model** (every gun model carries a `muzzle` anchor bone), turned into a camera facing billboard with a white hot core and a random roll per shot. It travels with the barrel while the weapon recoils. The world flash is only used for everybody watching the shooter.
- The weapon reacts on the **same frame** as the trigger pull: the shooter predicts the fire animation locally and the server broadcast skips them, so an automatic never lags behind the mouse.
- Recoil is a **stack of kicks**, not a snap: every shot adds its own kick and the kicks overlap while a burst is firing, so a fast weapon climbs further and further. Every shot is a little different and a little noisy, a kick that is exactly the same every time reads as an animation.
- The weapon has **weight**: mouse sway, walk bob, a sprint pose that drops it out of the way and an aim pose that brings it up to the shoulder.
- **Aiming is aligned to the sights.** While the weapon is at the hip the renderer measures where its rear sight really ends up on the screen and caches that position. Aiming slides the weapon by exactly that distance, so the sight ends up in the middle of the screen while the weapon is shouldered. The measurement is the only place where the real position is known - it depends on the model, on the display transform of the item and on the hand placement of the game. The log prints the measured position of the sight of every weapon that is taken into the hand.
- The walk bob follows the **walk animation of the player**, the same value that drives the legs, so the weapon does not slide against the stride. Every spring is integrated in fixed sub steps, so the motion is the same at 30, 60 and 240 frames per second.
- All animation tracks have **easing curves** - the recoil rises fast and settles slowly, reloads ease in and out, mechanical parts (slide, bolt, pump, magazine) stay linear and the barrel spin stays linear so it does not stutter.
- Every weapon also has the tracks that are the same for all of them: it is **drawn** into the hand, it can be **looked over** (key `V`) and it **clicks** instead of kicking when the trigger is pulled with an empty magazine. These tracks are additive, so a weapon that is looked over still recoils.
- The effects are drawn through **own core shaders** (`assets/techguns3/shaders/core`), built for the effect pipelines of the mod: a soft radial edge so a glow does not end in a hard square, a fog that dims the light instead of tinting it, a feathered cross section for the beams and a flowing plasma for the charge orb. The shaders are written in the dialect of the vanilla ones, so the game compiles them through shaderc into SPIR-V and the same source runs on the Vulkan and on the OpenGL backend. The vanilla pipelines stay as a fallback (`enhancedFx`).
- A bullet leaves a **hole** in the surface: a flat quad on the face that was hit, lit by the block, gone when the block is gone. The impact looks like the material it hit - metal throws bright sparks, wood throws splinters, loose material throws dust.
- A tracer keeps the same width on the screen, no matter how far away it is.
- A **scope** is a lens with a vignette and a reticle, and it opens up while the weapon is raised. A scoped weapon is not drawn while the player looks through the scope, the tube would stand in the middle of the lens.
- The **HUD** is a gun metal plate in the bottom right with the name of the weapon, the fire mode, the rounds in the magazine, the rounds in the inventory, the icon of the ammunition and the progress of the reload. The counter pulses when the magazine is nearly empty. The crosshair opens up with the spread of the weapon, fades out while the weapon is aimed, and a hit marker flashes when a shot connects.

Three client settings let you tune the feel without touching the code (see `config/techguns3-client.toml`):

| Option | Default | Meaning |
| --- | --- | --- |
| `adsPose` | 1.0 | How far the weapon comes up to the shoulder while aiming. 0 disables the aim pose. |
| `weaponSway` | 1.0 | Mouse sway of the held weapon. 0 disables it. |
| `weaponBob` | 1.0 | Walk bob of the held weapon. 0 disables it. |
| `enhancedFx` | true | Use the own effect shaders. While an Iris shader pack is active the effects switch to the vanilla-shader path automatically. Turn off to always use the vanilla path. |

Spawn the two enemies with `/summon techguns3:zombie_soldier` and `/summon techguns3:bandit`. They also spawn naturally in the overworld. Place `techguns3:turret_base` and right-click it to open its control screen. The left 3x3 slots take matching loaded magazines or loose shells; the right 3x3 slots receive empty magazines. The middle slots take a gun and optional `techguns3:turret_armor_iron`. The screen shows loaded rounds, head health and redstone status, with buttons for PvP and animal targeting. The turret attacks permitted targets within 32 blocks. A redstone signal disables firing. An iron ingot used on the base repairs the damaged head. The turret never uses energy.

## Test feedback to collect

- Startup or world-load crash: attach `run/logs/latest.log` and the crash report.
- Weapon: say which gun, ammunition and first/third-person view. Check geometry, reload/shot animation, sound, ammo count and hit registration.
- Mobs: check silhouette, skin, gun use, targeting, drops and natural spawn rate.
- Turret: check gun position, target selection, ammo use and outputs, settings, damage, repair, chunk unload/reload and redstone.

## Test checklist for the visual pass

The visual pass changed the rendering, the animation and the feel of the weapons. It cannot be
checked by the build, it has to be looked at:

1. Fire the AK-47 in full auto: the recoil climbs further and further, every shot is a little
   different, the weapon does not twitch.
2. Shoot at a wall: the holes sit flat on the surface, the impact looks like the material
   (sparks on stone, dust on dirt, splinters on wood) and a hole disappears when the block is
   broken.
3. Walk and run: the weapon moves with the legs, it does not slide against the stride.
4. Aim with a scoped weapon (AS50, bolt action): the lens opens up, the reticle is there, the
   weapon is not in the way of the lens.
5. Aim with iron sights: the sight of the weapon comes to the crosshair, the crosshair of the
   screen fades away.
6. Switch weapons: the weapon comes up into the hand.
7. Press `V`: the weapon is looked over and comes back.
8. Pull the trigger with an empty magazine: the weapon clicks instead of kicking, and the
   reload starts.
9. Look at the log: no `Couldn't parse GLSL` and no exception. Every weapon that is taken into the
   hand prints the position of its sight (`the sight of techguns3:ak47 is at (...) on the screen`).

## Known gaps

- The aim pose is the slide that brings the sight to the crosshair, not a rotation of the
  weapon around the sight: the models of the port point forward already, so there is no angle
  to correct.
- The shell casings are spawned in the world, not in the view model: the shooter sees them at
  the edge of the screen instead of at the ejection port of the weapon.
- The scope is a screen mask, not a stencil mask of the lens: the weapon is hidden instead of
  being cut away by the tube of the scope.
- The weapon is only drawn with an animation on the screen of the player who switched to it.
  Everybody else sees the weapon appear, because the switch itself is not sent to the server.
- There is no animation for putting the weapon away: the item is swapped in the inventory before
  the model could be lowered.

This is the first test slice, not the completed Community Edition port. Projectile collision and turret target filtering have been corrected, and cartridge sprites in flight have been replaced by particles. The original 3D geometry and textures are converted into GeckoLib models, with new firing and reload tracks and display scale matching the original renderer settings. The four weapons that were still flat sprites (revolver, M4, Thompson, bolt action rifle) now have real 3D models, animations and recipes; they were converted from the original 1.12 java models by `tools/convert_legacy_models.py`, which reproduces the already ported AK-47 exactly. First-person two-arm posing, exact animation and UV parity, HUD beyond the ammo bar, full faction rules, damage penetration and two-client play still require in-game verification and improvement.
