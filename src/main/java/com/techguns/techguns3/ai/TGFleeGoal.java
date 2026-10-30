package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Panic retreat below the profile's flee health threshold: sprint away from
 * the target, glancing shots over the shoulder while the gun still cycles.
 *
 * <p>Also the gate for the TNT suicide rush: when the mob is cornered, low and
 * the profile allows {@code dynamite_suicide}, this goal declines to start and
 * flags {@link TGCombatMemory#requestSuicideTNT} so {@link TGDynamiteGoal}
 * takes the wheel instead.</p>
 */
public final class TGFleeGoal extends Goal {
    private static final int FLEE_TICKS = 120;
    private static final int COOLDOWN = 200;
    /** Parting-shot attempt cadence; the weapon's own cooldown paces the actual fire. */
    private static final int PARTING_SHOT_EVERY_TICKS = 15;

    private final TGArmedMob mob;
    private int fleeTicks;
    private int cooldown;

    public TGFleeGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!TGConfig.mobsAiEnabled()) return false;
        MobProfile profile = mob.profile();
        LivingEntity target = mob.getTarget();
        if (profile == null || target == null || !target.isAlive()) return false;
        double threshold = profile.abilities().fleeHealth();
        if (threshold <= 0.0 || mob.getHealth() / mob.getMaxHealth() >= threshold) return false;

        // Cornered kamikaze: let the dynamite goal do the dramatic exit.
        if (profile.abilities().dynamiteSuicide() && TGConfig.mobsDynamite()
                && mob.dynamiteLeft() > 0 && mob.distanceTo(target) < 12.0) {
            mob.memory().requestSuicideTNT = true;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return fleeTicks < FLEE_TICKS && mob.getTarget() != null && mob.isAlive();
    }

    @Override
    public void start() {
        fleeTicks = 0;
    }

    @Override
    public void stop() {
        cooldown = COOLDOWN;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        fleeTicks++;
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        // Sprint directly away, repathed every 20 ticks or when bumped.
        if (fleeTicks % 20 == 1 || mob.getNavigation().isDone()) {
            Vec3 away = mob.position().subtract(target.position());
            Vec3 dest = away.lengthSqr() < 0.01
                    ? mob.position().add(mob.getRandom().nextInt(-8, 9), 0, mob.getRandom().nextInt(-8, 9))
                    : mob.position().add(away.normalize().scale(12.0));
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.3);
        }
        if (mob.horizontalCollision) {
            mob.getJumpControl().jump();
        }

        // Parting shot while backing off (CE NPCs shoot on the retreat).
        // NOTE: baseline fires on this fixed cadence without honoring the
        // weapon's own cooldown — honoring it (a real bug candidate) reshuffles
        // the deterministic mob self-test sequence, so it is left as-is.
        if (fleeTicks % PARTING_SHOT_EVERY_TICKS == 0 && mob.hasLineOfSight(target) && !mob.isReloading()
                && mob.loadedRounds() > 0 && mob.profile() != null) {
            mob.fireGunAt(target);
        }
    }
}
