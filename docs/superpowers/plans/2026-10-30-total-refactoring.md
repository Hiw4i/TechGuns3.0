# TechGuns 3 — Total Behavior-Preserving Refactoring Plan

**Goal:** Bring the whole mod to production quality (SoC, DRY, validation, error handling, performance) **without changing gameplay, AI behavior, animation feel or visuals** — except for a short, explicitly-listed set of genuine bug fixes.

**Architecture:** Package-by-package refactoring on top of five full-package audits. Shared byte-identical logic moves into dedicated helpers (`util/`, `client/fx/FxGeometry`, combat helpers). Hot paths lose allocations without changing computed values.

**Tech Stack:** Java 25, NeoForge 26.3, GeckoLib 5, Gradle.

**Spec:** user request "тотальный рефакторинг" + constraint "не сломать анимации и логику". Baseline: commit `db4d62c`, `./gradlew compileJava` green, `TECHGUNS3_SELFTEST=mobs runServer` baseline captured.

## Global Constraints

1. **Behavior lock:** no change to gun ballistics numbers, AI timings, spring/curve/spring constants, Random seeds/consumption order, vertex emission order, blend states, packet protocol field order. The **only** allowed behavior changes are the 8 documented bug fixes in §Bug Fixes.
2. Every task ends with `./gradlew compileJava` (or `build`) green before commit.
3. Commit after each task, conventional style matching history (`refactor(scope): …`, `fix(scope): …`).
4. Client package: only changes marked SAFE by the client audit (identical-value extraction, dead code, allocation hoisting with bit-identical output, renames). Never touch RISKY-listed constants.
5. No new dependencies. No over-DRY: extract only byte-identical or provably-equivalent logic.

## Bug Fixes (documented, intentional) — final status after verification

| # | Where | What | Status |
|---|-------|------|--------|
| B1 | `GunServerLogic` + `BallisticCombat` | Explosive spec stores unmultiplied damage; `ExplosiveCombat.explode` is the single application point of `gunDamageMultiplier()` (was config²) | **APPLIED** |
| B2 | `BulletProjectile` | Bullet-hole decals via `ServerLevel.sendParticles` (server-side `addParticle` was a client-only no-op → decals never rendered) | **APPLIED** |
| B3 | `TGCombatMemory.clearTargetState` | Also clear `requestBuild` | **REVERTED** — perturbs the deterministic AI sequence; documented in code as a candidate fix |
| B4 | `TGFleeGoal` | Parting shots honor `stats.fireCooldownTicks()` | **REVERTED** — same reason; documented in code |
| B5 | `TGHearingGoal` | Single `scanCooldown` decrement site | **REVERTED** — the TargetGoal lifecycle couples the two decrements; rescheduling changes the acquisition tick. Documented |
| B6 | `TGPickupWeaponGoal` | Ammo restock scored by distance | **APPLIED** (verified through all self-test runs) |
| B7 | `TGBuildGoal` | Clear `requestBuild` when rejecting on cooldown + shared cooldown | **REVERTED** — same reason as B3/B5 |
| B8 | `GunSelfTestCommand` | Aggregate failures into `finish(ok)` | **APPLIED** |
| B9 | Turret head search | One `findHeads` helper, radius 2.0 everywhere | **APPLIED** |
| B10 | `GunActionSounds` | Play order by `dueTick` (priority queue) | **APPLIED** |
| — | `TeslaArcRenderer` | Shared scratch vectors captured by deferred submit lambdas (review finding; visible with 2+ arcs/frame) | **FIXED** — per-submit locals restored |
| — | Gun self-test world | `run/world` persists across runs; old TNT craters under the stage change outcomes | **DISCOVERED** — comparable runs require a fresh world |

Everything else flagged by audits as "behavior-visible if changed" (TTL off-by-one, explosive+poison precedence, lock-acquisition cadence, fxDensity config max, guided-lock throttle) is **left as-is** and listed in the final report.

## Task 1 — Shared combat helpers + GunServerLogic core

**Files:** modify `combat/GunServerLogic.java`, `BallisticCombat.java`, `BeamCombat.java`, `ElectricCombat.java`, `ExplosiveCombat.java`, `GunActionSounds.java`; create `combat/CombatTargeting.java`.
**Changes:** hoist `DEFAULT_STATE`; extract `dryFire()`, `releaseHoldState()`; extract `CombatTargeting.aimFrom(yaw,pitch)` (3 copies), `validTarget(shooter,e)` (3), `beamRange(stats)` (2), `applyHitFx(level, victim, dir, goreChance, shooter)` (3), shared `NOISE_CONE` (2); constants for `now+5`, `% 4/% 12`, `>= 12`; fix B1; fix B10; remove unreachable try-catch (registry getValue); rename `TESLA_REGEN_TICKS→TESLA_FLICKER_TICKS`, `dist→distSqr`; hoist `DamageSource` out of entity loop; static comparator in `BeamCombat`; apply RADIATION effect only when absent/weaker.
**Verify:** compileJava; grep no leftover duplicate blocks.

## Task 2 — damagesystem + network

**Files:** `damagesystem/GoreRegistry.java`, `GoreHandler.java`, `RadiationHandler.java`, `DeathType.java`; `network/GunPackets.java`.
**Changes:** drop unreachable try-catch + dead `snowman` branch; `GIB_ORDER` constant; name radiation constants; reorder cheap guards first; `DeathType` keep + document as reserved API (author comment); GunSync payload: cap entries/chars on decode (hardening, trusted-server unaffected).
**Verify:** compileJava.

## Task 3 — entity package

**Files:** `entity/BulletProjectile.java`, `ChargeOrbEntity.java`, `BeamEntity.java`, `TeslaArcEntity.java`, `MuzzleFlashEntity.java`, `BlastEntity.java`, `SlimeBlobEntity.java`, `FlyingGibEntity.java`, `ShellCasingEntity.java`.
**Changes:** fix B2; `SPREAD_DEGREES`, `DEFAULT_HOMING_TURN`; tracer color → `GunFxPresets.TRACER_WARM` (3 in-file + renderer); one `dir` normalize local; rename `f`→`cosPitch`, `c`→`color`; remove unreachable catches (trail sprite, igniteAt → narrow); `MAX_LIFE` constants (Beam 5, TeslaArc 4, ChargeOrb 8 + drop dead `SEED` accessor); `MERGE_RADIUS_SQ`/`POP_*` naming in SlimeBlob; clamp `part` on NBT load; drop dead 7-arg MuzzleFlash ctor.
**Verify:** compileJava.

## Task 4 — turret package

**Files:** `turret/*`.
**Changes:** fix B9 (`TurretHeadLookup.find(level,pos)` shared, radius 2.0); `TARGET_RANGE` constant; cache anchor BE per ready-tick in `TurretHeadEntity`; `CONTAINER_SIZE`/`DATA_COUNT`/slot constants used by menu; merge `canStoreOutput`/`storeOutput` scan; remove unused imports; log corrupt UUID at debug.
**Verify:** compileJava.

## Task 5 — item + registry: data model & hot-path caching

**Files:** `item/GunDefinition.java`, `item/GenericGunItem.java`, `item/GunStats.java`, `item/GunExtras.java`, `item/GunDefProvider.java`, `registry/GunRegistry.java`, `registry/TGItems.java`, `registry/TGEntities.java`, `registry/TGParticles.java`, `registry/TGDataComponents.java`, `resource/GunDataLoader.java`, `resource/MobDataLoader.java`.
**Changes:**
- `GunDefinition` record → final class; lazily cached `stats`/`extras` (defs replaced wholesale on /reload → per-instance cache is inherently fresh); `soundSupplier` warns once per id; `toStats` logs warn-once instead of silent BALLISTIC fallback; extended `validate()` (speed>0, zoom_fov>0, r1/r2>0, block 0..1, ammo_base/full>0, drop_end≥drop_start); `GUIDED_DEFAULT_TURN` constant; `extraAmmo` carried as `List<Identifier>` end-to-end (fix namespace loss; shipped data all `techguns3:` → identical behavior).
- `GenericGunItem`: `emptyMagazineItem` via `TGItems.emptyMagazineOf(ammo)` map; delete dead `isReloading`; named constants (72000, bar colors/widths); reuse helpers in tooltip.
- `GunRegistry`: EMERGENCY dedupe via shared pistol builder; `defaults()` tail for the repeated 17-arg default block.
- `TGItems`: `spawnEgg()` helper ×12; `TGEntities`: `humanoidMob()` helper; `TGDataComponents`: `Codec.intRange(0, MAX)` for AMMO.
- `TGParticles`: one shared particle-options record + generic `TGParticleType<T>` replacing 4 identical records + 4 identical type classes (codec field names unchanged); `BulletHoleOptions.face` range-checked codec 0..5; move `BloodOptions` next to records.
- `resource/`: extract `DataLoaders.loadValidated(...)` generic helper (Gun+Mob); duplicate id → last-wins `put` + warn (aligns doc).

**Verify:** compileJava; `python tools/validate_resources.py`; JSON codec field names byte-identical (diff processResources output).

## Task 6 — mob + ai packages (behavior-identical AI, cheaper ticks)

**Files:** `mob/MobProfile.java`, `mob/MobRegistry.java`, `mob/TGArmedMob.java`, `ai/*`.
**Changes:**
- `TGCombatMemory`: add `boolean hasLos` (set by `TGArmedMob.aiStep`'s existing raytrace); remove dead `dynamiteCooldown`/`targetAgeTicks`; fix B3.
- `TGArmedMob`: cache LOS into memory (goals read it → one raytrace per mob per tick instead of two); `setLoadedRounds(stack,n)` helper; null-guard reload sound; name constants (jitter 6°, spread bounds, lead 0.85, damage mult 0.5, reaction floor, caps 64/256); local difficulty clamp; `navigateTo(BlockPos,speed)` helper; FQNs → imports.
- `TGGunCombatGoal`: read `memory.hasLos` instead of second raytrace; cover-scan backoff (retry ≤ every 20 ticks when no cover found — perf, not strategy change); single strafe-flip constants; `canBridge(profile)` reuse.
- `TGDynamiteGoal`: name cooldowns; remove dead condition; keep 50-tick threshold but named `SIEGE_HANDOFF_NO_LOS` + comment on divergence from 40 (values NOT unified — behavior lock).
- `TGMineGoal`: throttle blocking raytrace to every 10 ticks while far; named constants.
- `TGHearingGoal`: fix B5; scan inside `canUse` idiom (goal no longer starts/stops in 4-tick churn); missing `super.start()` restored.
- `TGFleeGoal`: fix B4.
- `TGPickupWeaponGoal`: fix B6; `urgent()` helper; FQNs → imports; named 64.0.
- `TGBuildGoal`: fix B7; named cadences.
- `TGGunfireEvents`: bound-check stale entries across world reloads; cached empty list.
- `MobProfile`: validate `attack_damage>0`, `armor 0..30`, `knockback_resistance 0..1`, `step_height>0`, spawn `weight>0`, `min<=max`; `pickWeapon` empty-pool guard; named validation bounds.

**Verify:** compileJava; then `TECHGUNS3_SELFTEST=mobs runServer` — PASS line set must equal baseline (this is the AI behavior gate).

## Task 7 — client package (SAFE list only)

**Files:** `client/fx/FxGeometry.java` (+ shared constants), `client/BulletRenderer.java`, `BeamRenderer.java`, `TeslaArcRenderer.java`, `MuzzleFlashRenderer.java`, `MuzzleFlashFx.java` (comment only), `BlastRenderer.java`, `ChargeOrbRenderer.java`, `MuzzleFlashLayer.java`, `TechGunsGeoItemRenderer.java`, `GunClientHandler.java`, `GunAimHandler.java`, `GunHudOverlay.java`, `FxRenderTypes.java`, `CurveParticle.java`, `BulletHoleParticle.java`, `FlyingGibRenderer.java`, `ShellCasingRenderer.java`, `GunClientCache.java`, `MuzzleFlashArt.java`, `TGShake.java`, `BlastFlashOverlay.java`, `GunSoldierRenderer.java`.
**Changes (all audited SAFE):** shared constants `SCOPE_ZOOM_FOV`, `FX_UP_THRESHOLD`, `MIN_VISIBLE_ALPHA`, `CORE_SCALE`, `TWO_PI`, `MIN_EFFECT_LENGTH`, `FxGeometry.FULLBRIGHT` reuse; `FxGeometry.atlasCell`, `starRolled`, `cameraDistanceTo` (per-site thresholds stay), `isFirstPerson` util; dead code removal (`BulletRenderer.id/dropTrail`, `FxGeometry.box/polyline`, `GunClientCache.snapshot`, `MuzzleFlashArt.id`); unused import + `item→gunItem` rename; Tesla scratch vectors + explicit single channel offset (values identical); FLASH_ART dedupe; BulletRenderer trail-loop dedupe keeping ONE batch + `clampTrail` without per-frame array churn (identical output order) + mutable `Trail.lastGameTime`; BeamRenderer dead-defensive catch → plain code; FxRenderTypes cached render types (same instances); HUD icon cache; `GunAimHandler` explicit `applied` flag; `@EventBusSubscriber` modid consistency; `TGShake` evict-oldest; gib/casing renderer scratch matrices; comment fix 50ms→70ms; `GunSoldierRenderer` unmodifiable texture map.
**Never touched:** GunFeel/Spring/SmoothNoise internals, MuzzleFlashFx statics/Random, BlastRenderer `3.14159f`, all curve/width/alpha constants, vertex emission orders, `Random` seeds and call order, `RenderFrameEvent` ordering, flame drag `0.94f`, aim-measure gate `0.02f` semantics.
**Verify:** compileJava + `build`.

## Task 8 — root + test packages

**Files:** `TechGuns3.java`, `TGConfig.java`, `TGTabs.java`, `test/GunSelfTestCommand.java`, `test/MobSelfTestCommand.java`, new `test/TGSelfTestHarness.java`, `test/TGSelfTests.java`.
**Changes:** `PROFILED_NPCS` single list (attributes + spawn placements); unused imports; self-test env switch moved to `TGSelfTests.maybeRun(server)`; fix B8; shared harness (registration, platform build, chunk pin, golem spawn, sweep, finish + pass/fail accounting) used by both commands; `aimGolem(IronGolem)` dedupe; `aroundFake()` AABB helper; phase windows list; `farmerMiner` naming; comment/code fix on the 8°-aim phase; `TGConfig` — only doc comments (no bound changes).
**Verify:** compileJava.

## Task 9 — Self code-review (hard)

Fresh-eyes pass over the full diff: (1) hunt accidental behavior drift (diff every constant extraction — value must be identical), (2) hunt over-DRY (any helper used once = inline it back), (3) re-run all 5 audits' HIGH/MED list and tick off each. Then a fresh reviewer agent on the diff. Find ≥3 weak points, fix them.

## Task 10 — Final verification

`./gradlew clean build` green; `python tools/validate_resources.py` clean; `TECHGUNS3_SELFTEST=mobs runServer` PASS set == baseline; push.

## Review Focus

1. **Codec/JSON stability** — any rename of codec field names breaks every datapack → verified by diffing `build/resources`.
2. **Config-thread volatility** — TGConfig volatile mirrors must stay volatile.
3. **GeckoLib spin/charge ids** — `spinId == 0` sentinel kept, not "improved".
4. **Server/client side confusion** — `level().isClientSide` branches must keep their side.
5. **AI timing constants** — values may be named, never re-valued (40 vs 50 divergence is kept deliberately).
