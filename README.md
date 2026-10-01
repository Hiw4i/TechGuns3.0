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

Wave-2 exotics (all in the same tab): `tfg` (hold LMB to charge up to 3s — growing plasma orb, HUD charge bar with MAX mark, rising whine; release for a charge-scaled green explosion, 1..5 cells per shot), `nucleardeathray` (hold LMB for a continuous radiation beam with a rotating spiral, 1 cell per 5 ticks — same tick rhythm as the original), `gaussrifle` (bolt railgun, 90 damage, strong zoom), `rocketlauncher` (rockets; carries `rocket_nuke`/`rocket_hv` auto-load first for nuke blast + radiation or double speed), `guidedmissilelauncher` (single shot, snap lock-on) and `grimreaper` (hold LMB, 4-rockets lock-on volley, beeps while locking), `sonicshotgun` (16-blade cone, heavy knockback), `lasergun`/`laserpistol` (hitscan beams, pistol runs on `redstone_battery`), `pulserifle` (3-round burst), `vector`/`pdw` (1200/min hoses), `as50` (8x sniper; explosive/incendiary magazines auto-load first). New ammunition: `nuclear_powercell`, `rocket`, `advanced_magazine` (+`advanced_rounds`), `as50_magazine` (+incendiary/explosive, +`sniper_rounds` variants), `gauss_magazine` (+`gauss_slugs`), `redstone_battery` — all with empty returns and crafting recipes. Explosive guns damage blocks (toggle `explosionsDamageBlocks`); the TFG echo and nuke blasts are the loudest sounds in the pack.

Visuals are ported from the original assets: per-gun muzzle flashes (`muzzleflashnew_add`, minigun, blue, laserflare, teslaflare, sonicwave, nukebeamflare, tfg_flare, flamethrower), textured projectiles (`fireball`, `bioblob`, `bullet_blue`, animated `tfg_flare`, `rocket`/`rocket_nuke`/`rocket_hv` by variant), scrolling textured laser (`laser3`) and 17-frame NDR (`nukebeam`) beams, sonic shockwave rings, 16-frame explosion billboards, CE recoil curves per gun, and looped flamethrower/beam sounds with a persistent flame tongue while the flamethrower trigger is held.

Spawn the two enemies with `/summon techguns3:zombie_soldier` and `/summon techguns3:bandit`. They also spawn naturally in the overworld. Place `techguns3:turret_base` and right-click it to open its control screen. The left 3x3 slots take matching loaded magazines or loose shells; the right 3x3 slots receive empty magazines. The middle slots take a gun and optional `techguns3:turret_armor_iron`. The screen shows loaded rounds, head health and redstone status, with buttons for PvP and animal targeting. The turret attacks permitted targets within 32 blocks. A redstone signal disables firing. An iron ingot used on the base repairs the damaged head. The turret never uses energy.

## Test feedback to collect

- Startup or world-load crash: attach `run/logs/latest.log` and the crash report.
- Weapon: say which gun, ammunition and first/third-person view. Check geometry, reload/shot animation, sound, ammo count and hit registration.
- Mobs: check silhouette, skin, gun use, targeting, drops and natural spawn rate.
- Turret: check gun position, target selection, ammo use and outputs, settings, damage, repair, chunk unload/reload and redstone.

This is the first test slice, not the completed Community Edition port. Projectile collision and turret target filtering have been corrected, and cartridge sprites in flight have been replaced by particles. The original 3D geometry and textures are converted into GeckoLib models, with new firing and reload tracks and display scale matching the original renderer settings. First-person two-arm posing, exact animation and UV parity, muzzle effects, a dedicated reload key, HUD beyond the ammo bar, full faction rules, damage penetration and two-client play still require in-game verification and improvement. The four pre-existing IDs `revolver`, `m4`, `thompson` and `bolt_action` remain registered but are hidden from the creative tab and have no crafting recipes until their quality pass.
