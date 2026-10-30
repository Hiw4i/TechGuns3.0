package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Field engineering: when the combat goal cannot path to the target (canyon,
 * water gap, cliff) it flags {@link TGCombatMemory#requestBuild} and this goal
 * bridges ahead of the mob with the profile's build material — ascending steps
 * toward elevated targets and a causeway over gaps. Blocks come from the
 * mob's material counter (stocked at spawn and replenished by
 * {@link TGMineGoal}).
 */
public final class TGBuildGoal extends Goal {
    private static final int MAX_BUILD_TICKS = 300;
    private static final int COOLDOWN = 100;

    private final TGArmedMob mob;
    private int cooldown;
    private int buildTicks;
    private int placeCooldown;

    public TGBuildGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    private boolean basicsAllow() {
        return TGConfig.mobsAiEnabled() && TGConfig.mobsGriefing() && mob.buildMaterials() > 0;
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
        return profile != null && profile.abilities().build()
                && target != null && target.isAlive()
                && mob.memory().requestBuild;
    }

    @Override
    public boolean canContinueToUse() {
        if (!basicsAllow()) return false;
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive()
                && buildTicks < MAX_BUILD_TICKS && mob.memory().requestBuild;
    }

    @Override
    public void start() {
        buildTicks = 0;
        placeCooldown = 0;
    }

    @Override
    public void stop() {
        mob.memory().requestBuild = false;
        cooldown = COOLDOWN;
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
        buildTicks++;
        if (placeCooldown > 0) placeCooldown--;

        // Keep walking toward the target while the blocks appear underfoot.
        if (mob.getNavigation().isDone() || buildTicks % 30 == 0) {
            mob.getNavigation().moveTo(target, 1.05);
        }

        if (placeCooldown > 0) return;

        // Dominant horizontal direction to the target.
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        BlockPos feet = mob.blockPosition();
        BlockPos ahead = feet.relative(dir);
        BlockPos aheadGround = ahead.below();

        boolean elevated = target.getY() - mob.getY() >= 2;
        BlockPos place = elevated ? ahead : aheadGround;

        if (tryPlace(level, place)) {
            mob.consumeBuildMaterial();
            placeCooldown = 8;
            if (elevated) {
                mob.getJumpControl().jump();
            }
            return;
        }
        // Step already solid: nothing to do here this tick; walking continues.
    }

    private boolean tryPlace(ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced()) return false;
        MobProfile profile = mob.profile();
        if (profile == null) return false;
        var material = profile.buildMaterialItem();
        if (material.isEmpty() || !(material.get() instanceof BlockItem blockItem)) return false;
        ItemStack stack = new ItemStack(blockItem, 1);
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (!level.setBlock(pos, placeState, 3)) return false;
        level.playSound(null, pos, placeState.getSoundType(level, pos, mob).getPlaceSound(),
                mob.getSoundSource(), 0.8f, 1.0f);
        return true;
    }
}
