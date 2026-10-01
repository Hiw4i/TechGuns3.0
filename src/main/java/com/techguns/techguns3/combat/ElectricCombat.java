package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
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

    private ElectricCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch) {
        Vec3 direction = aimDir(yaw, pitch);
        Vec3 farEnd = muzzle.add(direction.scale(stats.maxRange()));
        HitResult wall = clipWall(level, shooter, muzzle, farEnd);
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? farEnd : wall.getLocation();
        List<LivingEntity> primaries = findPrimaries(level, shooter, muzzle, direction, stats.maxRange());

        Set<UUID> hit = new HashSet<>();
        hit.add(shooter.getUUID());
        float damage = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());

        Vec3 tip = muzzle.add(direction.scale(0.5));
        // CE tesla muzzle flare (muzzleFlashLightning) at the barrel tip.
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                MuzzleFlashEntity.CYAN, stats.muzzleFlashScale(),
                com.techguns.techguns3.item.GunExtras.FLASH_LIGHTNING));
        if (primaries.isEmpty()) {
            drawArc(level, tip, wallEnd);
            if (wall instanceof net.minecraft.world.phys.BlockHitResult block
                    && stats.slimeGrowthPerHit() > 0.0f) {
                com.techguns.techguns3.entity.SlimeBlobEntity.growAt(
                        level, block.getBlockPos(), block.getDirection(), stats.slimeGrowthPerHit());
            }
            return;
        }
        for (LivingEntity primary : primaries) {
            // Chest hit, not the eyes: arcs ending exactly in the eye read as a
            // giant flash inside the victim's head from every angle.
            Vec3 targetEye = primary.getEyePosition().subtract(0, 0.3, 0);
            drawArc(level, tip, targetEye);
            impactFlash(level, targetEye);
            hit.add(primary.getUUID());
            Vec3 killDir = targetEye.subtract(tip).normalize();
            primary.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), damage);
            if (!primary.isAlive()) {
                com.techguns.techguns3.damagesystem.GoreHandler.onKill(level, primary, killDir,
                        com.techguns.techguns3.damagesystem.GoreHandler.TESLA_GORE_CHANCE,
                        com.techguns.techguns3.damagesystem.GoreHandler.USE_VICTIM_COLOR);
            }
            com.techguns.techguns3.fx.BloodFx.hitBurst(level, targetEye, killDir, damage,
                    com.techguns.techguns3.damagesystem.GoreRegistry.bloodColor(primary));
            chainFrom(level, shooter, primary, hit, damage * CHAIN_FACTOR);
        }
    }

    /**
     * Visual-only arc along the current aim (no damage). Called between damage
     * shots while the trigger is held so the bolt flickers continuously.
     */
    public static void drawArcOnly(ServerLevel level, LivingEntity shooter, GunStats stats,
                                   Vec3 muzzle, float yaw, float pitch) {
        Vec3 direction = aimDir(yaw, pitch);
        Vec3 farEnd = muzzle.add(direction.scale(stats.maxRange()));
        HitResult wall = clipWall(level, shooter, muzzle, farEnd);
        Vec3 wallEnd = wall.getType() == HitResult.Type.MISS ? farEnd : wall.getLocation();
        List<LivingEntity> primaries = findPrimaries(level, shooter, muzzle, direction, stats.maxRange());
        Vec3 tip = muzzle.add(direction.scale(0.5));
        if (primaries.isEmpty()) {
            drawArc(level, tip, wallEnd);
            return;
        }
        for (LivingEntity primary : primaries) {
            drawArc(level, tip, primary.getEyePosition().subtract(0, 0.3, 0));
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
            if (!current.isAlive()) {
                com.techguns.techguns3.damagesystem.GoreHandler.onKill(level, current, killDir,
                        com.techguns.techguns3.damagesystem.GoreHandler.TESLA_GORE_CHANCE,
                        com.techguns.techguns3.damagesystem.GoreHandler.USE_VICTIM_COLOR);
            }
            com.techguns.techguns3.fx.BloodFx.hitBurst(level, eye, killDir, dmg,
                    com.techguns.techguns3.damagesystem.GoreRegistry.bloodColor(current));
            drawArc(level, origin, eye);
            origin = eye;
            dmg *= CHAIN_FACTOR;
            current = nearestChainTarget(level, shooter, current, hit);
        }
    }

    private static Vec3 aimDir(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    private static HitResult clipWall(ServerLevel level, LivingEntity shooter, Vec3 muzzle, Vec3 end) {
        return level.clip(new ClipContext(muzzle, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
    }

    /** Up to N nearest valid targets inside a narrow cone around the aim, wall-checked. */
    private static List<LivingEntity> findPrimaries(ServerLevel level, LivingEntity shooter,
                                                    Vec3 muzzle, Vec3 direction, double range) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(muzzle, muzzle.add(direction.scale(range))).inflate(2),
                target -> validTarget(shooter, target))) {
            Vec3 toTarget = candidate.getEyePosition().subtract(muzzle);
            double dist = toTarget.length();
            if (dist > range) continue;
            if (toTarget.normalize().dot(direction) < CONE_COS) continue;
            Vec3 eye = candidate.getEyePosition();
            HitResult wall = level.clip(new ClipContext(muzzle, eye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
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
                        && validTarget(shooter, entity))) {
            Vec3 end = candidate.getEyePosition();
            double distance = start.distanceToSqr(end);
            if (distance >= bestDistance) continue;
            HitResult wall = level.clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            if (wall.getType() == HitResult.Type.MISS) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean validTarget(LivingEntity shooter, LivingEntity candidate) {
        return candidate != shooter && candidate.isAlive() && !shooter.isAlliedTo(candidate)
                && (!(shooter instanceof TurretHeadEntity turret) || turret.canAttackTarget(candidate));
    }

    private static void drawArc(ServerLevel level, Vec3 from, Vec3 to) {
        int seed = Float.floatToIntBits((float) from.x * 0.13f + (float) from.y)
                ^ Float.floatToIntBits((float) to.z * 0.71f - (float) to.x)
                ^ (int) level.getGameTime()
                ^ (int) (from.distanceTo(to) * 13.7f);
        // Micro-jitter scaled by distance: full jitter at range, near-zero in
        // someone's face so close arcs don't thrash inside the head.
        double dist = from.distanceTo(to);
        float jitterScale = (float) Math.min(1.0, Math.max(0.15, dist / 6.0));
        float jx = (hash01(seed) - 0.5f) * ARC_JITTER * jitterScale;
        float jy = (hash01(seed ^ 0x9E3779B9) - 0.5f) * ARC_JITTER * jitterScale;
        float jz = (hash01(seed ^ 0x85EBCA6B) - 0.5f) * ARC_JITTER * jitterScale;
        level.addFreshEntity(new TeslaArcEntity(level,
                from.x + jx, from.y + jy, from.z + jz, to.x, to.y, to.z, seed));
    }

    private static void impactFlash(ServerLevel level, Vec3 at) {
        level.addFreshEntity(new MuzzleFlashEntity(level, at.x, at.y, at.z,
                MuzzleFlashEntity.WHITE, 0.20f));
        com.techguns.techguns3.fx.FxParticles.teslaSparks(level, at, 4);
    }

    private static float hash01(int seed) {
        int h = seed ^ (seed >>> 16);
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
