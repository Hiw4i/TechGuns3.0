package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.damagesystem.GoreHandler;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.entity.SlimeBlobEntity;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.fx.GunFxPresets;
import com.techguns.techguns3.item.GunExtras;
import com.techguns.techguns3.item.GunStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fractal tesla behavior: up to {@value #PRIMARY_TARGETS} simultaneous main
 * channels grab the nearest targets in the aim cone, then each chains further
 * into a web. Visuals are {@link TeslaArcEntity} links (seeded, deterministic);
 * damage falls off per chain link.
 */
public final class ElectricCombat {
    private static final double CHAIN_RANGE = 8.0;
    private static final int CHAIN_TARGETS = 4;
    private static final float CHAIN_FACTOR = 0.75f;
    private static final int PRIMARY_TARGETS = 1;
    private static final double CONE_COS = Math.cos(Math.toRadians(12.0));
    private static final float ARC_JITTER = 0.2f;
    /** Arcs end at the chest, not the eyes: an arc ending IN the eye reads as a giant flash inside the head. */
    private static final double ARC_END_EYE_OFFSET = 0.3;
    private static final double TARGET_SEARCH_GROW = 2;
    /** Micro-jitter scale ramp: full jitter at this distance, near-zero in someone's face. */
    private static final double JITTER_FULL_DISTANCE = 6.0;
    private static final float JITTER_MIN_SCALE = 0.15f;

    private ElectricCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch) {
        Vec3 direction = CombatTargeting.aimFrom(yaw, pitch);
        Vec3 farEnd = muzzle.add(direction.scale(stats.maxRange()));
        HitResult wall = CombatTargeting.clipWall(level, shooter, muzzle, farEnd);
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? farEnd : wall.getLocation();
        List<LivingEntity> primaries = findPrimaries(level, shooter, muzzle, direction, stats.maxRange());

        Set<UUID> hit = new HashSet<>();
        hit.add(shooter.getUUID());
        float damage = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());

        Vec3 tip = muzzle.add(direction.scale(0.5));
        // CE tesla muzzle flare (muzzleFlashLightning) at the barrel tip.
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                MuzzleFlashEntity.CYAN, stats.muzzleFlashScale(),
                level.getRandom().nextInt(),
                GunExtras.FLASH_LIGHTNING,
                GunFxPresets.drawsOwnFlash(shooter)));
        if (primaries.isEmpty()) {
            drawArc(level, tip, wallEnd);
            if (wall instanceof net.minecraft.world.phys.BlockHitResult block
                    && stats.slimeGrowthPerHit() > 0.0f) {
                SlimeBlobEntity.growAt(level, block.getBlockPos(), block.getDirection(),
                        stats.slimeGrowthPerHit());
            }
            return;
        }
        for (LivingEntity primary : primaries) {
            Vec3 targetEye = primary.getEyePosition().subtract(0, ARC_END_EYE_OFFSET, 0);
            drawArc(level, tip, targetEye);
            impactFlash(level, targetEye);
            hit.add(primary.getUUID());
            Vec3 killDir = targetEye.subtract(tip).normalize();
            primary.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), damage);
            HitFeedback.hit(level, shooter, primary);
            CombatTargeting.hitFxBloodAlways(level, primary, targetEye, killDir, damage,
                    GoreHandler.TESLA_GORE_CHANCE);
            chainFrom(level, shooter, primary, hit, damage * CHAIN_FACTOR);
        }
    }

    /**
     * Visual-only arc along the current aim (no damage). Called between damage
     * shots while the trigger is held so the bolt flickers continuously.
     */
    public static void drawArcOnly(ServerLevel level, LivingEntity shooter, GunStats stats,
                                   Vec3 muzzle, float yaw, float pitch) {
        Vec3 direction = CombatTargeting.aimFrom(yaw, pitch);
        Vec3 farEnd = muzzle.add(direction.scale(stats.maxRange()));
        HitResult wall = CombatTargeting.clipWall(level, shooter, muzzle, farEnd);
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? farEnd : wall.getLocation();
        List<LivingEntity> primaries = findPrimaries(level, shooter, muzzle, direction, stats.maxRange());
        Vec3 tip = muzzle.add(direction.scale(0.5));
        if (primaries.isEmpty()) {
            drawArc(level, tip, wallEnd);
            return;
        }
        for (LivingEntity primary : primaries) {
            drawArc(level, tip, primary.getEyePosition().subtract(0, ARC_END_EYE_OFFSET, 0));
        }
    }

    private static void chainFrom(ServerLevel level, LivingEntity shooter, LivingEntity source,
                                  Set<UUID> hit, float damage) {
        LivingEntity current = nearestChainTarget(level, shooter, source, hit);
        Vec3 origin = source.getEyePosition();
        float dmg = damage;
        for (int chain = 0; chain < CHAIN_TARGETS && current != null; chain++) {
            hit.add(current.getUUID());
            Vec3 eye = current.getEyePosition();
            Vec3 killDir = eye.subtract(origin).normalize();
            current.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), dmg);
            CombatTargeting.hitFxBloodAlways(level, current, eye, killDir, dmg,
                    GoreHandler.TESLA_GORE_CHANCE);
            drawArc(level, origin, eye);
            origin = eye;
            dmg *= CHAIN_FACTOR;
            current = nearestChainTarget(level, shooter, current, hit);
        }
    }

    /** Up to N nearest valid targets inside a narrow cone around the aim, wall-checked. */
    private static List<LivingEntity> findPrimaries(ServerLevel level, LivingEntity shooter,
                                                    Vec3 muzzle, Vec3 direction, double range) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(muzzle, muzzle.add(direction.scale(range))).inflate(TARGET_SEARCH_GROW),
                target -> CombatTargeting.isEngageable(shooter, target)
                        && CombatTargeting.respectsTurretFilters(shooter, target))) {
            Vec3 eye = candidate.getEyePosition();
            Vec3 toTarget = eye.subtract(muzzle);
            double dist = toTarget.length();
            if (dist > range) continue;
            if (toTarget.normalize().dot(direction) < CONE_COS) continue;
            HitResult wall = CombatTargeting.clipWall(level, shooter, muzzle, eye);
            if (wall.getType() != HitResult.Type.MISS
                    && wall.getLocation().distanceToSqr(muzzle) < eye.distanceToSqr(muzzle) - 0.25) {
                continue;
            }
            found.add(candidate);
        }
        found.sort(Comparator.comparingDouble(e -> e.getEyePosition().distanceToSqr(muzzle)));
        return found.subList(0, Math.min(PRIMARY_TARGETS, found.size()));
    }

    private static LivingEntity nearestChainTarget(ServerLevel level, LivingEntity shooter,
                                                   LivingEntity source, Set<UUID> hit) {
        LivingEntity best = null;
        double bestDistance = CHAIN_RANGE * CHAIN_RANGE;
        Vec3 start = source.getEyePosition();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, start).inflate(CHAIN_RANGE), entity -> !hit.contains(entity.getUUID())
                        && CombatTargeting.isEngageable(shooter, entity)
                        && CombatTargeting.respectsTurretFilters(shooter, entity))) {
            Vec3 end = candidate.getEyePosition();
            double distance = start.distanceToSqr(end);
            if (distance >= bestDistance) continue;
            if (CombatTargeting.clipWall(level, shooter, start, end).getType() == HitResult.Type.MISS) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void drawArc(ServerLevel level, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        int seed = Float.floatToIntBits((float) from.x * 0.13f + (float) from.y)
                ^ Float.floatToIntBits((float) to.z * 0.71f - (float) to.x)
                ^ (int) level.getGameTime()
                ^ (int) (dist * 13.7f);
        // Micro-jitter scaled by distance: full jitter at range, near-zero in
        // someone's face so close arcs don't thrash inside the head.
        float jitterScale = (float) Math.min(1.0,
                Math.max(JITTER_MIN_SCALE, dist / JITTER_FULL_DISTANCE));
        float jx = (hash01(seed) - 0.5f) * ARC_JITTER * jitterScale;
        float jy = (hash01(seed ^ 0x9E3779B9) - 0.5f) * ARC_JITTER * jitterScale;
        float jz = (hash01(seed ^ 0x85EBCA6B) - 0.5f) * ARC_JITTER * jitterScale;
        level.addFreshEntity(new TeslaArcEntity(level,
                from.x + jx, from.y + jy, from.z + jz, to.x, to.y, to.z, seed));
    }

    private static void impactFlash(ServerLevel level, Vec3 at) {
        level.addFreshEntity(new MuzzleFlashEntity(level, at.x, at.y, at.z,
                MuzzleFlashEntity.WHITE, 0.20f));
        FxParticles.teslaSparks(level, at, 4);
    }

    private static float hash01(int seed) {
        int h = seed ^ (seed >>> 16);
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
