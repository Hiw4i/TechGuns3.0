package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.BeamEntity;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.fx.GunFxPresets;
import com.techguns.techguns3.item.GunExtras;
import com.techguns.techguns3.item.GunStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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

    /** Beam endpoint sits slightly below the eye: a beam ending IN the eye reads as a full-screen flash. */
    private static final double BEAM_END_EYE_OFFSET = 0.2;
    /** Expand the victim test by the bounding box so limbs count. */
    private static final double VICTIM_BOX_GROW = 0.4;
    private static final double VICTIM_SEARCH_GROW = 2.0;

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch, GunExtras extras) {
        Trace trace = trace(level, shooter, stats, extras, muzzle, yaw, pitch);
        drawBeam(level, shooter, muzzle, trace.beamEnd, extras);
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
                                  GunExtras extras, Vec3 muzzle, float yaw, float pitch) {
        Trace trace = trace(level, shooter, stats, extras, muzzle, yaw, pitch);
        if (trace.victim != null) {
            hitVictim(level, shooter, stats, extras, trace.victim, trace.beamEnd,
                    trace.beamEnd.subtract(muzzle).normalize());
        }
        // Per-tick impact glint at the beam end (BeamGunImpactFX).
        FxParticles.glow(level, trace.beamEnd.x, trace.beamEnd.y, trace.beamEnd.z, 2,
                0xFFFFFF, beamColor(extras), 0.10f, 0.02f, 8, 0.02f, 0.94f, 0.6, 1.4);
        FxParticles.teslaSparks(level, trace.beamEnd, 1);
    }

    private record Trace(Vec3 beamEnd, LivingEntity victim) {}

    private static Trace trace(ServerLevel level, LivingEntity shooter, GunStats stats,
                               GunExtras extras, Vec3 muzzle, float yaw, float pitch) {
        Vec3 dir = CombatTargeting.aimFrom(yaw, pitch);
        double range = CombatTargeting.beamRange(stats);
        Vec3 end = muzzle.add(dir.scale(range));
        HitResult wall = CombatTargeting.clipWall(level, shooter, muzzle, end);
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
        LivingEntity victim = findVictim(level, shooter, muzzle, dir, range, wallEnd);
        Vec3 beamEnd = victim != null ? victim.getEyePosition().subtract(0, BEAM_END_EYE_OFFSET, 0) : wallEnd;
        return new Trace(beamEnd, victim);
    }

    private static int beamColor(GunExtras extras) {
        return extras.radiationTicks() > 0 ? GunFxPresets.TRACER_NDR : GunFxPresets.TRACER_LASER;
    }

    private static void drawBeam(ServerLevel level, LivingEntity shooter, Vec3 muzzle, Vec3 beamEnd,
                                 GunExtras extras) {
        int color = beamColor(extras);
        boolean ndr = extras.radiationTicks() > 0;
        int flash = extras.muzzleFlash() != GunExtras.FLASH_AUTO
                ? extras.muzzleFlash()
                : (ndr ? GunExtras.FLASH_NUKEBEAM : GunExtras.FLASH_LASER);
        level.addFreshEntity(new BeamEntity(level, muzzle, beamEnd, color));
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                color, 0.65f, level.getRandom().nextInt(), flash,
                GunFxPresets.drawsOwnFlash(shooter)));
        level.addFreshEntity(new MuzzleFlashEntity(level, beamEnd.x, beamEnd.y, beamEnd.z,
                MuzzleFlashEntity.WHITE, 0.22f));
        FxParticles.glow(level, beamEnd.x, beamEnd.y, beamEnd.z, 4,
                0xFFFFFF, color, 0.12f, 0.02f, 12, 0.02f, 0.94f, 0.8, 1.6);
    }

    private static void hitVictim(ServerLevel level, LivingEntity shooter, GunStats stats,
                                  GunExtras extras, LivingEntity victim, Vec3 beamEnd, Vec3 dir) {
        float dmg = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());
        victim.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), dmg);
        HitFeedback.hit(level, shooter, victim);
        if (extras.radiationTicks() > 0) {
            CombatTargeting.applyRadiation(victim, extras.radiationTicks(), 1);
        }
        CombatTargeting.hitFx(level, victim, beamEnd, dir, dmg, 1.0f);
    }

    private static LivingEntity findVictim(ServerLevel level, LivingEntity shooter,
                                           Vec3 muzzle, Vec3 dir, double range, Vec3 wallEnd) {
        double wallDist = wallEnd.distanceTo(muzzle);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(muzzle, muzzle.add(dir.scale(range))).inflate(VICTIM_SEARCH_GROW),
                e -> CombatTargeting.isEngageable(shooter, e)
                        && CombatTargeting.respectsTurretFilters(shooter, e));
        candidates.sort(Comparator.comparingDouble(e -> e.getEyePosition().distanceToSqr(muzzle)));
        for (LivingEntity candidate : candidates) {
            Vec3 eye = candidate.getEyePosition();
            double dist = eye.distanceTo(muzzle);
            if (dist > wallDist || dist > range) continue;
            Vec3 toTarget = eye.subtract(muzzle).normalize();
            // Generous beam cone (6° half-angle): the muzzle sits ~0.4m off the
            // eye-aim axis, so a tighter cone would reject dead-on shots at range.
            if (toTarget.dot(dir) < Math.cos(Math.toRadians(CombatTargeting.AIM_CONE_DEGREES))) continue;
            if (candidate.getBoundingBox().inflate(VICTIM_BOX_GROW).clip(muzzle, wallEnd).isPresent()) {
                return candidate;
            }
        }
        return null;
    }
}
