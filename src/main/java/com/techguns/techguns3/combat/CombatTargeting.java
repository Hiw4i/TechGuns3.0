package com.techguns.techguns3.combat;

import com.techguns.techguns3.damagesystem.GoreHandler;
import com.techguns.techguns3.damagesystem.GoreRegistry;
import com.techguns.techguns3.fx.BloodFx;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.registry.TGEffects;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Aim and target math shared by every combat style (ballistic, beam, tesla,
 * guided lock-on). One copy of each formula so the styles can never drift:
 * the aim vector, the beam range floor, the engagement predicates, the wall
 * clip and the flesh response to a hit.
 */
public final class CombatTargeting {
    private CombatTargeting() {}

    /** Half-angle of the generous aim cone shared by beams and guided lock-on. */
    public static final double AIM_CONE_DEGREES = 6.0;
    /** Beams never fall below this range, even at point-blank stats. */
    private static final double MIN_BEAM_RANGE = 10.0;

    /** Aim vector from yaw/pitch, same convention as the vanilla look angle. */
    public static Vec3 aimFrom(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    /** Hitscan range in blocks: stat range scaled by the projectile speed. */
    public static double beamRange(GunStats stats) {
        return Math.max(MIN_BEAM_RANGE, stats.maxRange() * stats.bulletSpeed());
    }

    /** Base engagement predicate: alive, not the shooter, not an ally. */
    public static boolean isEngageable(LivingEntity shooter, LivingEntity target) {
        return target != shooter && target.isAlive() && !shooter.isAlliedTo(target);
    }

    /** Mounted guns must also respect the PvP/animal switches of their base. */
    public static boolean respectsTurretFilters(LivingEntity shooter, LivingEntity target) {
        return !(shooter instanceof TurretHeadEntity turret) || turret.canAttackTarget(target);
    }

    /** Wall clip used identically by every style: solid blocks, no fluids. */
    public static HitResult clipWall(Level level, LivingEntity shooter, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
    }

    /** Radiation dose shared by beams, nukes and bio projectiles. */
    public static void applyRadiation(LivingEntity victim, int ticks, int amplifier) {
        victim.addEffect(new MobEffectInstance(TGEffects.RADIATION, ticks, amplifier));
    }

    /** Flesh response to a hit: gore gib on kill, blood burst otherwise. */
    public static void hitFx(ServerLevel level, LivingEntity victim, Vec3 at, Vec3 direction,
                             float damage, float goreChance) {
        if (!victim.isAlive()) {
            goreOnKill(level, victim, direction, goreChance);
        } else {
            bloodBurst(level, victim, at, direction, damage);
        }
    }

    /** Tesla variant: arcs always throw blood, the gib stacks on top of a kill. */
    public static void hitFxBloodAlways(ServerLevel level, LivingEntity victim, Vec3 at, Vec3 direction,
                                        float damage, float goreChance) {
        if (!victim.isAlive()) {
            goreOnKill(level, victim, direction, goreChance);
        }
        bloodBurst(level, victim, at, direction, damage);
    }

    public static void goreOnKill(ServerLevel level, LivingEntity victim, Vec3 direction, float goreChance) {
        GoreHandler.onKill(level, victim, direction, goreChance, GoreHandler.USE_VICTIM_COLOR);
    }

    public static void bloodBurst(ServerLevel level, LivingEntity victim, Vec3 at, Vec3 direction, float damage) {
        BloodFx.hitBurst(level, at, direction, damage, GoreRegistry.bloodColor(victim));
    }
}
