package com.techguns.techguns3.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Per-mob combat state shared by the Techguns goals: current combat phase,
 * burst/reaction timers, the cover spot the reposition phase prefers, and the
 * handoff flags the combat goal uses to yield control to the siege goals
 * (dynamite, mining, building).
 *
 * <p>Server-authoritative. Timers tick in {@code TGArmedMob.aiStep()} so they
 * advance even while a different goal owns the mob; nothing here is saved to
 * NBT except the resource counters (carried by the entity itself), because a
 * half-finished burst or an obsolete cover pos is worthless after a reload.</p>
 *
 * <p>{@link #hasLos} is refreshed by the same raytrace that feeds
 * {@code noLosTicks}, so goals read it instead of re-raytracing.
 * {@link #buildCooldown} is deliberately shared: the combat goal suppresses
 * bridge requests while the builder is cooling down, so the two must agree
 * on one timer.</p>
 */
public final class TGCombatMemory {
    /** Ticks since the mob last had line of sight to its target. */
    public int noLosTicks;
    /** Line of sight to the current target, refreshed once per tick by aiStep. */
    public boolean hasLos;
    /** Reaction delay left before the first shot may go out. */
    public int reactionTicks;
    /** Shots fired in the current burst. */
    public int burstShots;
    /** Ticks left of the pause between bursts. */
    public int burstPauseTicks;
    /** Ticks left of the weapon reload. */
    public int reloadTicks;
    /** Aim settle accumulator: grows while the gun tracks the target. */
    public int aimTicks;
    /** Strafe direction flip timer so the sidestep pattern is not epileptic. */
    public int strafeFlipTicks;
    /** Flank side: -1 left, 1 right; flipped on timer or when blocked. */
    public int flankSide = 1;
    /** Ticks until the builder may take another bridging job (shared with the combat goal). */
    public int buildCooldown;
    /** Ticks the dynamite goal has spent retreating from its own charge. */
    public int dynamiteRetreatTicks;

    /** Cover spot the reposition phase walks to; cleared when reached/stale. */
    @Nullable
    public BlockPos coverPos;
    /** Ticks the current cover pos stays valid before re-planning. */
    public int coverPosAge;

    /** Set by the combat goal when it wants the dynamite goal to take over. */
    public boolean requestTNT;
    /** Suicide-breach request (cornered + low HP + profile allows it). */
    public boolean requestSuicideTNT;
    /** Set when the builder noticed a gap/step it wants to bridge. */
    public boolean requestBuild;
    /** Set when the target is walled in and the miner should dig through. */
    public boolean requestDig;

    /** True while any siege goal holds the floor (combat goal must stay off). */
    public boolean anySiegeRequest() {
        return requestTNT || requestSuicideTNT || requestBuild || requestDig;
    }

    public void tickTimers() {
        if (noLosTicks < Integer.MAX_VALUE) noLosTicks++;
        if (reactionTicks > 0) reactionTicks--;
        if (burstPauseTicks > 0) burstPauseTicks--;
        if (reloadTicks > 0) reloadTicks--;
        if (strafeFlipTicks > 0) strafeFlipTicks--;
        if (buildCooldown > 0) buildCooldown--;
        if (dynamiteRetreatTicks > 0) dynamiteRetreatTicks--;
        coverPosAge++;
    }

    public void clearTargetState() {
        noLosTicks = 0;
        reactionTicks = 0;
        burstShots = 0;
        burstPauseTicks = 0;
        aimTicks = 0;
        coverPos = null;
        coverPosAge = 0;
        requestTNT = false;
        requestSuicideTNT = false;
        requestBuild = false;
        requestDig = false;
        hasLos = false;
    }

    /** @param target may be null (idle timers still advance). */
    public void onTickWithTarget(@Nullable LivingEntity target, boolean hasLos) {
        tickTimers();
        if (target == null) {
            clearTargetState();
            return;
        }
        this.hasLos = hasLos;
        if (hasLos) {
            noLosTicks = 0;
            // Hole opened / corner turned: the dig request has served its purpose.
            requestDig = false;
        }
    }
}
