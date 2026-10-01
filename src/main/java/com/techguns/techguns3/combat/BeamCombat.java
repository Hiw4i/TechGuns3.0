package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.BeamEntity;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Hitscan beam behavior (AbstractBeamProjectile.trace): instant wall-clipped
 * ray with a visible beam entity, no travel time. Covers NDR (radiation),
 * lasergun and laserpistol. Damage has NO falloff (RangeTooltipType.NO_DROP).
 */
public final class BeamCombat {
    private BeamCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch,
                            com.techguns.techguns3.item.GunExtras extras) {
        Trace trace = trace(level, shooter, stats, extras, muzzle, yaw, pitch);
        drawBeam(level, muzzle, trace.beamEnd, extras);
        if (trace.victim != null) {
            hitVictim(level, shooter, stats, extras, trace.victim, trace.beamEnd,
                    trace.beamEnd.subtract(muzzle).normalize());
        } else {
            FxParticles.glow(level, trace.beamEnd.x, trace.beamEnd.y, trace.beamEnd.z, 4,
                    0xFFFFFF, beamColor(extras), 0.12f, 0.02f, 12, 0.02f, 0.94f, 0.8, 1.6);
        }
    }

    /**
     * Sustained-beam tick (NDRProjectile.onUpdate 1:1): trace + full damage +
     * radiation every tick while held. Callers own the persistent visual beam
     * and ammo drain; this only deals damage and impact FX.
     */
    public static void tickDamage(ServerLevel level, LivingEntity shooter, GunStats stats,
                                  com.techguns.techguns3.item.GunExtras extras,
                                  Vec3 muzzle, float yaw, float pitch) {
        Trace trace = trace(level, shooter, stats, extras, muzzle, yaw, pitch);
        if (trace.victim != null) {
            hitVictim(level, shooter, stats, extras, trace.victim, trace.beamEnd,
                    trace.beamEnd.subtract(muzzle).normalize());
        }
        // Per-tick impact glint at the beam end (BeamGunImpactFX).
        FxParticles.glow(level, trace.beamEnd.x, trace.beamEnd.y, trace.beamEnd.z, 2,
                0xFFFFFF, beamColor(extras), 0.10f, 0.02f, 8, 0.02f, 0.94f, 0.6, 1.4);
        com.techguns.techguns3.fx.FxParticles.teslaSparks(level, trace.beamEnd, 1);
    }

    private record Trace(Vec3 beamEnd, LivingEntity victim) {}

    private static Trace trace(ServerLevel level, LivingEntity shooter, GunStats stats,
                               com.techguns.techguns3.item.GunExtras extras,
                               Vec3 muzzle, float yaw, float pitch) {
        Vec3 dir = aimDir(yaw, pitch);
        double range = Math.max(10.0, stats.maxRange() * stats.bulletSpeed());
        Vec3 end = muzzle.add(dir.scale(range));
        HitResult wall = level.clip(new ClipContext(muzzle, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
        LivingEntity victim = findVictim(level, shooter, muzzle, dir, range, wallEnd);
        Vec3 beamEnd = victim != null ? victim.getEyePosition().subtract(0, 0.2, 0) : wallEnd;
        return new Trace(beamEnd, victim);
    }

    private static int beamColor(com.techguns.techguns3.item.GunExtras extras) {
        boolean ndr = extras.radiationTicks() > 0;
        return ndr ? com.techguns.techguns3.fx.GunFxPresets.TRACER_NDR
                : com.techguns.techguns3.fx.GunFxPresets.TRACER_LASER;
    }

    private static void drawBeam(ServerLevel level, Vec3 muzzle, Vec3 beamEnd,
                                 com.techguns.techguns3.item.GunExtras extras) {
        int color = beamColor(extras);
        boolean ndr = extras.radiationTicks() > 0;
        int flash = extras.muzzleFlash() != com.techguns.techguns3.item.GunExtras.FLASH_AUTO
                ? extras.muzzleFlash()
                : (ndr ? com.techguns.techguns3.item.GunExtras.FLASH_NUKEBEAM
                        : com.techguns.techguns3.item.GunExtras.FLASH_LASER);
        level.addFreshEntity(new BeamEntity(level, muzzle, beamEnd, color));
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                color, 0.65f, flash));
        level.addFreshEntity(new MuzzleFlashEntity(level, beamEnd.x, beamEnd.y, beamEnd.z,
                MuzzleFlashEntity.WHITE, 0.22f));
        FxParticles.glow(level, beamEnd.x, beamEnd.y, beamEnd.z, 4,
                0xFFFFFF, color, 0.12f, 0.02f, 12, 0.02f, 0.94f, 0.8, 1.6);
    }

    private static void hitVictim(ServerLevel level, LivingEntity shooter, GunStats stats,
                                  com.techguns.techguns3.item.GunExtras extras,
                                  LivingEntity victim, Vec3 beamEnd, Vec3 dir) {
        float dmg = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());
        victim.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), dmg);
        if (extras.radiationTicks() > 0) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.techguns.techguns3.registry.TGEffects.RADIATION,
                    extras.radiationTicks(), 1));
        }
        if (!victim.isAlive()) {
            com.techguns.techguns3.damagesystem.GoreHandler.onKill(level, victim, dir,
                    1.0f, com.techguns.techguns3.damagesystem.GoreHandler.USE_VICTIM_COLOR);
        } else {
            com.techguns.techguns3.fx.BloodFx.hitBurst(level, beamEnd, dir, dmg,
                    com.techguns.techguns3.damagesystem.GoreRegistry.bloodColor(victim));
        }
    }

    private static LivingEntity findVictim(ServerLevel level, LivingEntity shooter,
                                           Vec3 muzzle, Vec3 dir, double range, Vec3 wallEnd) {
        double wallDist = wallEnd.distanceTo(muzzle);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(muzzle, muzzle.add(dir.scale(range))).inflate(2.0),
                e -> e != shooter && e.isAlive() && !shooter.isAlliedTo(e)
                        && (!(shooter instanceof TurretHeadEntity turret) || turret.canAttackTarget(e)));
        candidates.sort(Comparator.comparingDouble(e -> e.getEyePosition().distanceToSqr(muzzle)));
        for (LivingEntity candidate : candidates) {
            Vec3 eye = candidate.getEyePosition();
            double dist = eye.distanceTo(muzzle);
            if (dist > wallDist || dist > range) continue;
            Vec3 toTarget = eye.subtract(muzzle).normalize();
            // Generous beam cone (6° half-angle): the muzzle sits ~0.4m off the
            // eye-aim axis, so a tighter cone would reject dead-on shots at range.
            if (toTarget.dot(dir) < Math.cos(Math.toRadians(6.0))) continue;
            // Expand by bounding box so limbs count.
            if (candidate.getBoundingBox().inflate(0.4).clip(muzzle, wallEnd).isPresent()) {
                return candidate;
            }
        }
        return null;
    }

    private static Vec3 aimDir(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }
}
