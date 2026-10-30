package com.techguns.techguns3.ai;

import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Cover search: samples standing spots around the mob and keeps the nearest
 * one whose eye line to the target is blocked by a solid block — the "pop out,
 * burst, duck back in" position the combat goal walks to while reloading.
 */
public final class TGCoverFinder {
    private TGCoverFinder() {}

    @Nullable
    public static BlockPos findCover(TGArmedMob mob, LivingEntity target) {
        Vec3 targetEye = target.getEyePosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        var random = mob.getRandom();

        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(-6, 7);
            int dz = random.nextInt(-6, 7);
            if (Math.abs(dx) + Math.abs(dz) < 2) continue;
            BlockPos ground = findGround(mob, mob.blockPosition().offset(dx, 0, dz));
            if (ground == null) continue;
            BlockPos stand = ground.above();

            Vec3 eye = Vec3.atBottomCenterOf(stand).add(0, 1.5, 0);
            if (isCovered(mob, targetEye, eye)) {
                double dist = mob.blockPosition().distSqr(stand);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = ground;
                }
            }
        }
        return best;
    }

    /** First solid block below {@code around} (up to 3 down) with 2 air above. */
    @Nullable
    private static BlockPos findGround(TGArmedMob mob, BlockPos around) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = around.below(-dy);
            BlockState state = mob.level().getBlockState(pos);
            if (state.isSolidRender()) {
                return mob.level().getBlockState(pos.above()).isAir()
                        && mob.level().getBlockState(pos.above(2)).isAir() ? pos : null;
            }
        }
        return null;
    }

    /** True when a solid block sits on the straight line between the two eyes. */
    private static boolean isCovered(TGArmedMob mob, Vec3 from, Vec3 to) {
        double length = from.distanceTo(to);
        if (length < 1.5) return false;
        BlockHitResult hit = mob.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == BlockHitResult.Type.BLOCK
                && from.distanceTo(hit.getLocation()) < length - 0.5;
    }

    /**
     * First block that blocks the mob's eye-to-target eye line; the mine and
     * dynamite goals breach exactly there.
     */
    @Nullable
    public static BlockHitResult blockingHit(TGArmedMob mob, LivingEntity target) {
        BlockHitResult hit = mob.level().clip(new ClipContext(mob.getEyePosition(),
                target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == BlockHitResult.Type.BLOCK ? hit : null;
    }

    /** Offset of a hit position back toward the mob by one block (our side of a wall). */
    public static BlockPos towardMob(BlockHitResult hit, TGArmedMob mob) {
        Direction face = hit.getDirection();
        BlockPos pos = hit.getBlockPos().relative(face);
        return mob.level().getBlockState(pos).isAir() ? pos : hit.getBlockPos().above();
    }
}
