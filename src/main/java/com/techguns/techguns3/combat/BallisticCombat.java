package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.entity.ShellCasingEntity;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.fx.GunFxPresets;
import com.techguns.techguns3.item.GunStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-owned ballistic attack shared by players, NPCs and mounted guns.
 * Cinematic TG2 treatment, all compact by design: baked seeded flash at the
 * barrel tip, flare + forward-pushed smoke, brass eject, positional sound.
 * Nothing spawns inside the shooter's head.
 */
public final class BallisticCombat {
    private BallisticCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch, SoundSource source) {
        fire(level, shooter, stats, com.techguns.techguns3.item.GunExtras.DEFAULT,
                null, 0, 1.0f, -1, muzzle, yaw, pitch, source, 1.0f);
    }

    /** Entry point with an aim-state spread multiplier (zoomed shots group tighter). */
    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch, SoundSource source, float spreadMult) {
        fire(level, shooter, stats, com.techguns.techguns3.item.GunExtras.DEFAULT,
                null, 0, 1.0f, -1, muzzle, yaw, pitch, source, spreadMult);
    }

    /**
     * Full wave-2 entry point. {@code variantId} selects the loaded ammo variant
     * (nuke/hv/explosive/incendiary), {@code charge01} scales TFG damage/size,
     * {@code homingTargetId} enables guided steering.
     */
    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            com.techguns.techguns3.item.GunExtras extras, String variantId,
                            float charge01, float chargeScale, int homingTargetId,
                            Vec3 muzzle, float yaw, float pitch, SoundSource source, float spreadMult) {
        if (stats.projectileKind() == GunStats.ProjectileKind.ELECTRIC) {
            ElectricCombat.fire(level, shooter, stats, muzzle, yaw, pitch);
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    stats.fireSound().get(), source, 1.0f, 1.0f);
            return;
        }
        if (extras.beam() || stats.projectileKind() == GunStats.ProjectileKind.BEAM
                || stats.projectileKind() == GunStats.ProjectileKind.LASER) {
            BeamCombat.fire(level, shooter, stats, muzzle, yaw, pitch, extras);
            // Looping beams (NDR) play their loop from the hold handler instead.
            if (!extras.hasLoop()) {
                level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                        stats.fireSound().get(), source, 1.0f, 1.0f);
            }
            return;
        }
        var preset = GunFxPresets.forKind(stats.projectileKind(), stats.muzzleFlashScale());
        // Variant resolution: nuke > hv > explosive > incendiary > default.
        boolean nuke = variantId != null && variantId.contains("nuke");
        boolean hv = variantId != null && (variantId.contains("_hv") || variantId.contains("high_velocity"));
        boolean explosiveMag = variantId != null && variantId.contains("explosive");
        boolean incendiaryMag = variantId != null && variantId.contains("incendiary");
        float speed = stats.bulletSpeed() * (hv ? 2.0f : 1.0f) * chargeScale;
        float damage = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier()) * chargeScale;
        int fireTicks = stats.projectileKind() == GunStats.ProjectileKind.FIRE ? 80 : 0;
        if (incendiaryMag) fireTicks = 100;
        float expMax = 0, expMin = 0, expR1 = 0, expR2 = 0, expBlock = 0;
        boolean expNuke = false;
        if (extras.hasExplosive()) {
            var e = extras.explosive();
            if (nuke) {
                // CE RocketNuke: ~5x damage, ~5x radius + radiation.
                expMax = e.max() * 5.0f; expMin = e.min() * 5.0f;
                expR1 = e.r1() * 3.75f; expR2 = e.r2() * 3.125f;
                expBlock = 0.5f; expNuke = true;
            } else if (hv) {
                expMax = e.max(); expMin = e.min();
                expR1 = e.r1() * 0.75f; expR2 = e.r2() * 0.75f;
                expBlock = e.blockDamage();
            } else if (explosiveMag) {
                // AS50-explosive: small pop like GenericProjectileExplosive.
                expMax = damage * 0.8f; expMin = damage * 0.4f;
                expR1 = 2.0f; expR2 = 4.0f; expBlock = 0.2f;
            } else {
                expMax = (float) (e.max() * TGConfig.gunDamageMultiplier()) * chargeScale;
                expMin = (float) (e.min() * TGConfig.gunDamageMultiplier()) * chargeScale;
                expR1 = e.r1() * (charge01 > 0 ? (1.0f + charge01 * 2.0f) : 1.0f);
                expR2 = e.r2() * (charge01 > 0 ? (1.0f + charge01 * 2.0f) : 1.0f);
                expBlock = e.blockDamage();
                expNuke = e.nuke();
            }
        }
        int homingId = homingTargetId;
        BulletProjectile.Spec spec = new BulletProjectile.Spec(
                damage, speed, stats.maxRange(),
                stats.dropStart(), stats.dropEnd(), stats.dropMin(),
                stats.gravity(), stats.penetration(),
                0.0f, 0.0f, homingId, extras.guided() ? 0.16f : extras.homingTurn(),
                stats.slimeGrowthPerHit(),
                stats.projectileKind() == GunStats.ProjectileKind.BIO ? 100 : 0,
                stats.projectileKind() == GunStats.ProjectileKind.BIO ? 3 : 0,
                fireTicks, preset.tracerColor(), nuke ? 2.5f : stats.bulletBulk(),
                expMax, expMin, expR1, expR2, expBlock, expNuke,
                extras.knockback(), extras.ignoreIframes(), extras.radiationTicks(), false);
        // Pellets spawn at the tip itself (CE spawns at the gun offset, not
        // meters ahead). Point-blank cover comes from the close muzzle plus
        // the behind-sweep in BulletProjectile.tick, not from a forward nudge.
        Vec3 look = aimLook(yaw, pitch);
        Vec3 spawn = muzzle;
        boolean sonic = stats.projectileKind() == GunStats.ProjectileKind.SONIC;
        for (int i = 0; i < stats.pellets(); i++) {
            float spread = (i == 0 ? stats.spread() : stats.pelletSpread()) * spreadMult;
            BulletProjectile.Spec shot = (sonic && i == 0) ? spec.withSonicMain() : spec;
            level.addFreshEntity(new BulletProjectile(level, shooter, spawn, yaw, pitch, spread, shot));
        }
        boolean bio = stats.projectileKind() == GunStats.ProjectileKind.BIO;
        boolean fire = stats.projectileKind() == GunStats.ProjectileKind.FIRE;
        // CE muzzle art (ScreenEffect 1:1): textured flash sized by the gun's own
        // flash scale, blooming with TFG charge.
        float flashScale = stats.muzzleFlashScale() * (charge01 > 0.0f ? (0.6f + charge01) : 1.0f);
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                preset.muzzleColor(), flashScale,
                GunFxPresets.muzzleTexture(stats, extras)));
        FxParticles.muzzleFlare(level, muzzle, preset.flareColor(), 0.7f + 0.3f * stats.muzzleFlashScale());
        if (preset.smokeCount() > 0) {
            FxParticles.muzzleSmoke(level, muzzle.add(look.scale(0.4)), look, preset.smokeCount());
        }
        if (fire) {
            // Continuous stream feel: tongues of textured flame blown forward
            // plus embers, so the jet reads as fire from the first tick.
            FxParticles.flameJet(level, muzzle.add(look.scale(0.6)), look, 10);
            FxParticles.embers(level, muzzle.add(look.scale(0.8)), 3);
        }
        if (bio) {
            FxParticles.slimeBurst(level, muzzle.add(look.scale(0.5)), 2);
        }
        ejectShell(level, shooter, yaw);
        // Positional on the barrel, not at the feet: distant shots attenuate honestly.
        // Looping guns (flamethrower) play their loop from the hold handler instead.
        if (!extras.hasLoop()) {
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    stats.fireSound().get(), source, 1.0f, 1.0f);
        }
        if (stats.extraSound() != null && !extras.hasCharge() && !extras.hasLoop()) {
            // Charge/loop guns play extraSound as charge-loop/start on FireStart, not per shot.
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    stats.extraSound().get(), source, 1.0f, 1.0f);
        }
        if (stats.actionSound() != null && stats.actionDelayTicks() > 0) {
            // Pump / bolt cycle lands after the shot, like the original.
            GunActionSounds.schedule(level, muzzle, stats.actionSound(), source, stats.actionDelayTicks());
        }
    }

    private static Vec3 aimLook(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    /** Brass to the right + up + slightly back, visual only. */
    private static void ejectShell(ServerLevel level, LivingEntity shooter, float yaw) {
        if (!TGConfig.shellCasings() || !TGConfig.cinematicFx()) return;
        double yawRad = Math.toRadians(yaw);
        // Right vector on the ground plane + up kick.
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 eye = shooter.getEyePosition();
        Vec3 at = eye.add(right.scale(0.25)).add(0, -0.05, 0);
        Vec3 vel = right.scale(0.9 + level.getRandom().nextDouble() * 0.5)
                .add(0, 1.1 + level.getRandom().nextDouble() * 0.5, 0)
                .add(shooter.getDeltaMovement().scale(0.3));
        level.addFreshEntity(new ShellCasingEntity(level, at, vel,
                level.getRandom().nextFloat() * (float) (Math.PI * 2)));
    }
}
