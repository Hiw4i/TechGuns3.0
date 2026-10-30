package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * The ears of the squad: port of the CE {@code ILivingSoldier} hearing. Every
 * 4 ticks the mob listens for players — sprinting carries 24 blocks, walking
 * 12, sneaking 3 — and for recent gunfire ({@link TGGunfireEvents}), reacting
 * even through walls, which the vanilla sight-based targeting never does.
 */
public final class TGHearingGoal extends TargetGoal {
    private static final int SCAN_INTERVAL = 4;
    /** Louder movement = larger hearing radius (sprint 24, walk 12, sneak 3). */
    private static final double SPRINT_RADIUS = 24.0;
    private static final double WALK_RADIUS = 12.0;
    private static final double SNEAK_RADIUS = 3.0;
    /** Horizontal speed squared tiers between the radius bands. */
    private static final double SPRINT_SPEED_SQ = 0.10;
    private static final double WALK_SPEED_SQ = 0.004;

    /** No sight or invisibility games: ears work through walls, full stop. */
    private static final TargetingConditions HEARD =
            TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final TGArmedMob mob;
    private int scanCooldown;

    public TGHearingGoal(TGArmedMob mob) {
        super(mob, false);
        this.mob = mob;
    }

    // Baseline lifecycle, kept verbatim: TargetGoal.canContinueToUse()
    // delegates to canUse(), so the scan cooldown advances through BOTH the
    // canUse polls and the tick gate below. Rescheduling this changes the
    // exact acquisition tick, which reshuffles the deterministic mob
    // self-test sequence (see docs/superpowers/plans).
    @Override
    public boolean canUse() {
        if (!TGConfig.mobsAiEnabled()) return false;
        MobProfile profile = mob.profile();
        if (profile == null || !profile.abilities().hearPlayers()) return false;
        return scanCooldown-- <= 0;
    }

    @Override
    public void start() {
        scanCooldown = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (--scanCooldown > 0) return;
        scanCooldown = SCAN_INTERVAL;
        if (!(mob.level() instanceof ServerLevel level)) return;

        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()) return;

        Player heard = hearPlayer(level);
        if (heard == null) {
            heard = hearGunfire(level);
        }
        if (heard != null && canAttack(heard, HEARD)) {
            mob.setTarget(heard);
        }
    }

    /** Louder movement = larger hearing radius (sprint 24, walk 12, sneak 3). */
    @Nullable
    private Player hearPlayer(ServerLevel level) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) continue;
            double radius = hearRadius(player);
            if (radius <= 0.0) continue;
            double distSq = player.distanceToSqr(mob);
            if (distSq <= radius * radius && distSq < bestDist
                    && canAttack(player, HEARD)) {
                best = player;
                bestDist = distSq;
            }
        }
        return best;
    }

    private double hearRadius(Player player) {
        double speedSq = player.getDeltaMovement().horizontalDistanceSqr();
        if (player.isSprinting() || speedSq > SPRINT_SPEED_SQ) return SPRINT_RADIUS;
        if (speedSq > WALK_SPEED_SQ) return WALK_RADIUS;
        if (player.isShiftKeyDown()) return SNEAK_RADIUS;
        return 0.0;
    }

    /** Any player-shot gun in the last second within 32 blocks gives them away. */
    @Nullable
    private Player hearGunfire(ServerLevel level) {
        for (TGGunfireEvents.Shot shot : TGGunfireEvents.recent(level, mob.position(), 32.0)) {
            if (shot.shooter() instanceof Player shooter && canAttack(shooter, HEARD)) {
                return shooter;
            }
        }
        return null;
    }
}
