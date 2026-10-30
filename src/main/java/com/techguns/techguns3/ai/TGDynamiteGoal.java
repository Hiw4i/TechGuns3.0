package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Smart TNT usage (breach charge + suicide rush), ported in spirit from the CE
 * NPC dynamite logic:
 * <ul>
 *   <li><b>Breach:</b> target walled in — walk the charge to the wall's face on
 *       the mob's side, prime it, then retreat at least 8 blocks before it
 *       blows.</li>
 *   <li><b>Suicide:</b> cornered and dying with {@code dynamite_suicide} —
 *       prime TNT under own feet and sprint at the target.</li>
 * </ul>
 * Priming skips placing the block: {@link PrimedTnt} is the same visual and
 * damage either way, and no {@link TntBlock} state flash desyncs the client.
 */
public final class TGDynamiteGoal extends Goal {
    private static final int RETREAT_TICKS = 90;
    private static final int BREACH_COOLDOWN = 200;

    private final TGArmedMob mob;
    private int cooldown;
    private boolean suicidePhase;
    private boolean retreating;
    @Nullable
    private BlockPos chargePos;

    public TGDynamiteGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean basicsAllow() {
        return TGConfig.mobsAiEnabled() && TGConfig.mobsDynamite() && mob.dynamiteLeft() > 0;
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!basicsAllow()) return false;
        MobProfile profile = mob.profile();
        LivingEntity target = mob.getTarget();
        if (profile == null || profile.abilities().dynamite() <= 0
                || target == null || !target.isAlive()) return false;
        TGCombatMemory memory = mob.memory();
        if (memory.requestSuicideTNT) return true;
        double distSq = mob.distanceToSqr(target);
        return (memory.requestTNT || memory.noLosTicks > 50)
                && distSq <= profile.abilities().dynamiteRange() * profile.abilities().dynamiteRange();
    }

    @Override
    public boolean canContinueToUse() {
        if (!basicsAllow()) return false;
        if (suicidePhase) return mob.isAlive();
        return retreating || mob.memory().requestTNT;
    }

    @Override
    public void start() {
        suicidePhase = false;
        retreating = false;
        chargePos = null;
    }

    @Override
    public void stop() {
        if (!retreating) {
            // Finished or aborted without a detonation: back off politely.
            mob.memory().requestTNT = false;
            mob.memory().requestSuicideTNT = false;
            cooldown = BREACH_COOLDOWN;
        }
        chargePos = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        LivingEntity target = mob.getTarget();

        if (suicidePhase) {
            if (target != null && target.isAlive()) {
                mob.getNavigation().moveTo(target, 1.5);
            }
            return;
        }

        if (retreating) {
            if (mob.memory().dynamiteRetreatTicks <= 0 || mob.memory().dynamiteRetreatTicks > RETREAT_TICKS) {
                retreating = false;
                cooldown = BREACH_COOLDOWN;
            }
            return;
        }

        if (mob.memory().requestSuicideTNT) {
            prime(level, mob.blockPosition());
            mob.consumeDynamite();
            mob.memory().requestSuicideTNT = false;
            mob.memory().dynamiteCooldown = 600;
            suicidePhase = true;
            return;
        }

        if (target == null) return;
        BlockHitResult wallHit = TGCoverFinder.blockingHit(mob, target);
        if (wallHit == null) {
            mob.memory().requestTNT = false;
            cooldown = 60;
            return;
        }
        BlockPos place = TGCoverFinder.towardMob(wallHit, mob);
        if (mob.blockPosition().distSqr(place) > 4) {
            mob.getNavigation().moveTo(place.getX() + 0.5, place.getY() + 1.0, place.getZ() + 0.5, 1.2);
            return;
        }

        // At the wall: plant, prime, run.
        prime(level, place);
        mob.consumeDynamite();
        mob.memory().requestTNT = false;
        mob.memory().dynamiteCooldown = BREACH_COOLDOWN;

        Vec3 away = mob.position().subtract(target.position());
        Vec3 dest = away.lengthSqr() < 0.01
                ? mob.position().add(mob.getRandom().nextInt(-10, 11), 0, mob.getRandom().nextInt(-10, 11))
                : mob.position().add(away.normalize().scale(10.0));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.35);
        mob.memory().dynamiteRetreatTicks = RETREAT_TICKS;
        retreating = true;
    }

    private void prime(ServerLevel level, BlockPos pos) {
        TntBlock.prime(level, pos, mob, ItemStack.EMPTY);
    }
}
