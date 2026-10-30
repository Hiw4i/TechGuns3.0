package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The core Techguns NPC fire discipline: an ACQUIRE → AIM → FIRE → RELOAD →
 * REPOSITION loop with cover usage, flanking sidesteps, reaction delay,
 * burst-fire pacing and velocity lead (CE {@code useTargetOffsetPathing}
 * behaviour folded into the strafe phase).
 *
 * <p>Flag model: this goal owns MOVE+LOOK while the target is engageable and
 * yields by stepping aside when a siege goal should take over — a walled-in
 * target sets {@link TGCombatMemory#requestTNT}/{@link TGCombatMemory#requestDig},
 * an unreachable one sets {@link TGCombatMemory#requestBuild}. The request
 * flags are checked in {@code canUse()} (not just {@code canContinueToUse()})
 * so the higher-priority combat goal cannot preempt the siege goal every
 * other tick and starve it.</p>
 */
public final class TGGunCombatGoal extends Goal {
    /** Ticks of no line-of-sight before the mob gives up the chase entirely. */
    private static final int GIVE_UP_NO_LOS = 120;
    /** Ticks of aiming before the first shot leaves the barrel. */
    private static final int AIM_SETTLE_TICKS = 4;
    /** No-LOS ticks before handing the situation to the siege goals. */
    private static final int SIEGE_NO_LOS = 40;
    /** Strafe-side flip timer, opening burst vs mid-fight band (deliberately different). */
    private static final int FLIP_TICKS_OPENING = 40;
    private static final int FLIP_JITTER_OPENING = 40;
    private static final int FLIP_TICKS_BAND = 40;
    private static final int FLIP_JITTER_BAND = 50;
    /** Ticks between failed-path re-paths while chasing. */
    private static final int REPATH_TICKS = 5;
    /** Stop walking at the cover block (squared; BlockPos distances are integers). */
    private static final double AT_COVER_DIST_SQ = 2.5;
    /** Reload-length wait before giving a failed cover search another try. */
    private static final int COVER_SCAN_RETRY_TICKS = 20;

    private final TGArmedMob mob;
    /** Tick of the last shot; -1 sentinel avoids int overflow vs MIN_VALUE. */
    private int lastShotTick = -1;
    private int repathCooldown;
    /** Ticks until a failed cover search may run again (no cover found backoff). */
    private int coverScanCooldown;
    @Nullable
    private Vec3 lastKnownTargetPos;

    public TGGunCombatGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean basicsAllow() {
        if (!TGConfig.mobsAiEnabled()) return false;
        MobProfile profile = mob.profile();
        return profile != null && profile.armed()
                && mob.heldGun() != null && !mob.weaponDry();
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return basicsAllow() && target != null && target.isAlive()
                && !mob.memory().anySiegeRequest();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (!basicsAllow() || target == null || !target.isAlive()) return false;
        TGCombatMemory memory = mob.memory();
        return !memory.anySiegeRequest() && memory.noLosTicks <= GIVE_UP_NO_LOS;
    }

    @Override
    public void start() {
        TGCombatMemory memory = mob.memory();
        memory.aimTicks = 0;
        memory.burstShots = 0;
        memory.strafeFlipTicks = FLIP_TICKS_OPENING + mob.getRandom().nextInt(FLIP_JITTER_OPENING);
        repathCooldown = 0;
        coverScanCooldown = 0;
    }

    @Override
    public void stop() {
        TGCombatMemory memory = mob.memory();
        if (memory.noLosTicks > GIVE_UP_NO_LOS) {
            // Hunt cold: drop the stale target so hearing/re-aggro can restart.
            mob.setTarget(null);
            memory.clearTargetState();
        }
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        GenericGunItem gun = mob.heldGun();
        MobProfile profile = mob.profile();
        if (gun == null || profile == null) return;
        GunStats stats = gun.stats();
        TGCombatMemory memory = mob.memory();

        double dist = mob.distanceTo(target);
        boolean los = memory.hasLos;
        if (los) {
            lastKnownTargetPos = target.position();
        }
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // --- siege handoff: target walled in ---
        if (!los && memory.noLosTicks > SIEGE_NO_LOS) {
            MobProfile.Abilities abilities = profile.abilities();
            if (TGConfig.mobsDynamite() && mob.dynamiteLeft() > 0
                    && dist <= abilities.dynamiteRange()) {
                memory.requestTNT = true;
                return;
            }
            if (TGConfig.mobsGriefing() && abilities.mineBlocks()
                    && dist <= abilities.digRange()) {
                memory.requestDig = true;
                return;
            }
            if (canBridge(profile) && lastKnownTargetPos != null) {
                memory.requestBuild = true;
                return;
            }
        }

        // --- reload cycle ---
        if (mob.isReloading()) {
            hideBehindCover(target, profile);
            return;
        }
        if (mob.loadedRounds() == 0) {
            mob.tryStartReload();
            hideBehindCover(target, profile);
            return;
        }

        // --- shooting ---
        if (los && dist <= profile.combat().maxRange()) {
            if (memory.reactionTicks <= 0 && memory.aimTicks >= AIM_SETTLE_TICKS
                    && memory.burstPauseTicks <= 0
                    && mob.tickCount - lastShotTick >= stats.fireCooldownTicks()) {
                if (memory.burstShots >= profile.combat().burst()) {
                    memory.burstPauseTicks = profile.combat().burstPauseTicks();
                    memory.burstShots = 0;
                } else {
                    mob.fireGunAt(target);
                    memory.burstShots++;
                    lastShotTick = mob.tickCount;
                }
            } else if (memory.reactionTicks <= 0) {
                memory.aimTicks++;
            }
        }

        // --- movement ---
        move(dist, los, profile, target);
    }

    /** Range keeping + flanking sidestep; hides behind cover between bursts. */
    private void move(double dist, boolean los, MobProfile profile, LivingEntity target) {
        TGCombatMemory memory = mob.memory();
        double preferred = profile.combat().preferredRange();

        if (--repathCooldown > 0) return;
        repathCooldown = REPATH_TICKS;

        // Chase when out of range or blind to the target.
        if (dist > profile.combat().maxRange() || !los) {
            boolean moving = mob.getNavigation().moveTo(target, 1.05);
            if (!moving && los && canBridge(profile) && memory.buildCooldown <= 0) {
                // Pathfinder says no route (gap/cliff): ask the builder to bridge.
                // Suppressed while the builder cools down, or the request would
                // keep the combat goal yielded with nobody to serve it.
                memory.requestBuild = true;
            }
            return;
        }

        // Too close: back off while firing.
        if (dist < preferred * 0.4) {
            if (mob.getNavigation().isDone()) {
                mob.getMoveControl().strafe(-0.5f, 0.0f);
            }
            return;
        }

        // Above the preferred band but inside range: close in; an unreachable
        // target here (canyon, moat) is also the builder's problem.
        if (dist > preferred) {
            if (mob.getNavigation().isDone()) {
                boolean moving = mob.getNavigation().moveTo(target, 1.0);
                if (!moving && canBridge(profile) && memory.buildCooldown <= 0) {
                    memory.requestBuild = true;
                }
            }
            return;
        }

        // Inside the band: flank sideways, flipping sides periodically.
        if (memory.strafeFlipTicks <= 0) {
            memory.flankSide = -memory.flankSide;
            memory.strafeFlipTicks = FLIP_TICKS_BAND + mob.getRandom().nextInt(FLIP_JITTER_BAND);
        }
        if (mob.getNavigation().isDone()) {
            mob.getMoveControl().strafe(0.0f, 0.6f * memory.flankSide);
        }
    }

    private boolean canBridge(MobProfile profile) {
        return TGConfig.mobsGriefing() && profile.abilities().build()
                && mob.buildMaterials() > 0;
    }

    /** Walk to (and wait at) a cover block while the weapon cycles. */
    private void hideBehindCover(LivingEntity target, MobProfile profile) {
        TGCombatMemory memory = mob.memory();
        if (!profile.abilities().cover()) return;
        BlockPos cover = memory.coverPos;
        if (cover == null) {
            // Nothing found yet: retry on a backoff, a multi-second reload must
            // not run a full raytrace sweep every tick.
            if (coverScanCooldown > 0) {
                coverScanCooldown--;
            } else {
                memory.coverPos = TGCoverFinder.findCover(mob, target);
                memory.coverPosAge = 0;
                cover = memory.coverPos;
                coverScanCooldown = COVER_SCAN_RETRY_TICKS;
            }
        } else if (memory.coverPosAge > 100
                || !mob.level().getBlockState(cover).isSolidRender()) {
            // The spot went stale (mined, despawned): replace it immediately.
            memory.coverPos = TGCoverFinder.findCover(mob, target);
            memory.coverPosAge = 0;
            cover = memory.coverPos;
        }
        if (cover != null) {
            if (mob.blockPosition().distSqr(cover) > AT_COVER_DIST_SQ) {
                mob.getNavigation().moveTo(cover.getX() + 0.5, cover.getY() + 1.0,
                        cover.getZ() + 0.5, 1.2);
            } else {
                mob.getNavigation().stop();
            }
        }
    }
}
