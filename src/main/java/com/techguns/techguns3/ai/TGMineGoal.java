package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Breaks through walls that block the line of fire: digs out the first block
 * on the eye-to-eye line (doors, windows, sandbag gaps all count), timed by
 * block hardness / {@code block_break_speed}. The mined block goes straight
 * into the mob's build materials — the CE miner loop of "dig here, stack there".
 *
 * <p>Reach guard: only digs blocks within ~4.2 blocks; otherwise paths to the
 * wall first. Bedrock-level ({@code destroySpeed < 0}) blocks abort the goal
 * with a cooldown so the mob does not grind on unbreakables.</p>
 */
public final class TGMineGoal extends Goal {
    private static final double REACH_SQ = 4.2 * 4.2;
    private static final int NO_LOS_TO_DIG = 30;
    private static final int ABORT_COOLDOWN = 200;

    private final TGArmedMob mob;
    private int cooldown;
    @Nullable
    private BlockPos miningPos;
    private int progress;
    private int totalTicks;

    public TGMineGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean basicsAllow() {
        return TGConfig.mobsAiEnabled() && TGConfig.mobsGriefing();
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
        if (profile == null || !profile.abilities().mineBlocks()
                || target == null || !target.isAlive()) return false;
        double digRange = profile.abilities().digRange();
        if (mob.distanceToSqr(target) > digRange * digRange) return false;
        // Combat-goal handoff or a long blind streak both start the dig.
        return mob.memory().requestDig || mob.memory().noLosTicks > NO_LOS_TO_DIG;
    }

    @Override
    public boolean canContinueToUse() {
        if (!basicsAllow()) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Job done: the hole opened and line of sight came back.
        return mob.memory().requestDig || mob.memory().noLosTicks > 4;
    }

    @Override
    public void stop() {
        miningPos = null;
        mob.memory().requestDig = false;
        cooldown = ABORT_COOLDOWN / 4;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        BlockHitResult hit = TGCoverFinder.blockingHit(mob, target);
        if (hit == null) {
            cooldown = 40;
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0.0f || state.isAir()) {
            // Unbreakable: give up for a long while.
            cooldown = ABORT_COOLDOWN;
            return;
        }

        if (mob.blockPosition().distSqr(pos) > REACH_SQ) {
            BlockPos standPos = TGCoverFinder.towardMob(hit, mob);
            mob.getNavigation().moveTo(standPos.getX() + 0.5, standPos.getY() + 1.0,
                    standPos.getZ() + 0.5, 1.15);
            return;
        }

        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (!pos.equals(miningPos)) {
            miningPos = pos;
            progress = 0;
            MobProfile profile = mob.profile();
            double speed = profile != null ? profile.stats().blockBreakSpeed() : 1.0;
            totalTicks = Math.max(8, (int) Math.ceil(destroySpeed * 20.0 / Math.max(0.05, speed)));
        }

        progress++;
        mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        if (progress % 6 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.4, 0.4, 0.4, 0.05);
        }

        if (progress >= totalTicks) {
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos,
                    net.minecraft.world.level.block.Block.getId(state));
            level.destroyBlock(pos, false, mob, 512);
            mob.addBuildMaterials(1);
            miningPos = null;
            cooldown = 8;
        }
    }
}
