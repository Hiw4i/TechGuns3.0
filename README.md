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

Open a creative world, search the TechGuns 3 tab, and test the pistol, AK-47, combat shotgun, minigun and Tesla Gun. Their ammunition is `pistol_magazine`, `assault_rifle_magazine`, `shotgun_rounds`, `minigun_drum` and `energy_cell` respectively. Hold right-click for the AK-47, minigun and Tesla Gun; click once for the pistol and shotgun. When empty, right-click with matching ammunition in inventory to reload. Magazine-fed guns return an empty magazine or cell. Tesla Gun hits the first target in its path and can arc to four nearby targets.

Spawn the two enemies with `/summon techguns3:zombie_soldier` and `/summon techguns3:bandit`. They also spawn naturally in the overworld. Place `techguns3:turret_base` and right-click it to open its control screen. The left 3x3 slots take matching loaded magazines or loose shells; the right 3x3 slots receive empty magazines. The middle slots take a gun and optional `techguns3:turret_armor_iron`. The screen shows loaded rounds, head health and redstone status, with buttons for PvP and animal targeting. The turret attacks permitted targets within 32 blocks. A redstone signal disables firing. An iron ingot used on the base repairs the damaged head. The turret never uses energy.

## Test feedback to collect

- Startup or world-load crash: attach `run/logs/latest.log` and the crash report.
- Weapon: say which gun, ammunition and first/third-person view. Check geometry, reload/shot animation, sound, ammo count and hit registration.
- Mobs: check silhouette, skin, gun use, targeting, drops and natural spawn rate.
- Turret: check gun position, target selection, ammo use and outputs, settings, damage, repair, chunk unload/reload and redstone.

This is the first test slice, not the completed Community Edition port. Projectile collision and turret target filtering have been corrected, and cartridge sprites in flight have been replaced by particles. The original 3D geometry and textures are converted into GeckoLib models, with new firing and reload tracks and display scale matching the original renderer settings. First-person two-arm posing, exact animation and UV parity, muzzle effects, a dedicated reload key, HUD beyond the ammo bar, full faction rules, damage penetration and two-client play still require in-game verification and improvement. The four pre-existing IDs `revolver`, `m4`, `thompson` and `bolt_action` remain registered but are hidden from the creative tab and have no crafting recipes until their quality pass.
