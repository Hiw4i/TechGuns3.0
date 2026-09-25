package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Source Tesla behavior: instant first hit, then up to four nearby chain targets. */
public final class ElectricCombat {
    private static final double CHAIN_RANGE = 8.0;
    private static final int CHAIN_TARGETS = 4;
    private static final float CHAIN_FACTOR = 0.75f;

    private ElectricCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        Vec3 direction = new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p),
                Math.cos(y) * Math.cos(p));
        Vec3 end = muzzle.add(direction.scale(stats.maxRange()));
        HitResult wall = level.clip(new ClipContext(muzzle, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();

        LivingEntity first = firstTarget(level, shooter, muzzle, end);
        Vec3 firstEnd = first == null ? end : first.getEyePosition();
        drawArc(level, muzzle, firstEnd);
        if (first == null) return;

        Set<UUID> hit = new HashSet<>();
        hit.add(shooter.getUUID());
        LivingEntity current = first;
        Vec3 origin = muzzle;
        float damage = (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());
        for (int chain = 0; chain <= CHAIN_TARGETS && current != null; chain++) {
            hit.add(current.getUUID());
            current.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), damage);
            origin = current.getEyePosition();
            damage *= CHAIN_FACTOR;
            LivingEntity next = nearestChainTarget(level, shooter, current, hit);
            if (next != null) drawArc(level, origin, next.getEyePosition());
            current = next;
        }
    }

    private static LivingEntity firstTarget(ServerLevel level, LivingEntity shooter, Vec3 start, Vec3 end) {
        LivingEntity best = null;
        double bestDistance = start.distanceToSqr(end);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(1), candidate -> validTarget(shooter, candidate))) {
            var intersection = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance < bestDistance) {
                    best = entity;
                    bestDistance = distance;
                }
            }
        }
        return best;
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
                ^ (int) level.getGameTime();
        level.addFreshEntity(new TeslaArcEntity(level,
                from.x, from.y, from.z, to.x, to.y, to.z, seed));
    }
}
