package com.techguns.techguns3.ai;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Scavenger loop: walks to dropped Techguns guns and matching magazines,
 * swapping when the pickup beats the held weapon by score
 * (damage × pellets, magazine as tiebreak) — the CE "NPCs upgrade their gun"
 * behaviour. Runs idle (stroll-level priority) and urgently when the current
 * weapon is dry.
 */
public final class TGPickupWeaponGoal extends Goal {
    /** Held weapon must beat a candidate by this factor to keep it. */
    private static final double UPGRADE_FACTOR = 1.2;
    private static final int COOLDOWN = 60;

    private final TGArmedMob mob;
    private int cooldown;
    @Nullable
    private ItemEntity wanted;

    public TGPickupWeaponGoal(TGArmedMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!TGConfig.mobsAiEnabled() || !TGConfig.mobsPickupWeapons()) return false;
        MobProfile profile = mob.profile();
        if (profile == null || !profile.abilities().pickupWeapons()) return false;

        boolean urgent = mob.heldGun() == null || mob.weaponDry();
        // Upgrade scans only while not fighting; urgent grabs always run.
        if (!urgent && mob.getTarget() != null) return false;

        wanted = findBest(profile);
        return wanted != null;
    }

    @Override
    public boolean canContinueToUse() {
        return wanted != null && wanted.isAlive() && !wanted.isRemoved()
                && TGConfig.mobsPickupWeapons();
    }

    @Override
    public void stop() {
        cooldown = COOLDOWN;
        wanted = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (wanted == null) return;
        if (mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(wanted, 1.1);
        }
        if (mob.distanceToSqr(wanted) < 2.2) {
            grab(wanted);
            wanted = null;
        }
    }

    @Nullable
    private ItemEntity findBest(MobProfile profile) {
        double radius = profile.abilities().pickupRadius();
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class,
                mob.getBoundingBox().inflate(radius));
        GenericGunItem held = mob.heldGun();
        double heldScore = held != null ? gunScore(held.stats()) : 0.0;

        ItemEntity best = null;
        double bestScore = urgentThreshold(heldScore);
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            if (stack.getItem() instanceof GenericGunItem gun) {
                double score = gunScore(gun.stats());
                if (score > bestScore) {
                    bestScore = score;
                    best = entity;
                }
            } else if (held != null && stack.is(held.ammoItem()) && stack.getCount() > 0) {
                // Magazine restock beats nothing when spares are finite and low.
                if (best == null && mob.spareMagazines() == 0) {
                    best = entity;
                }
            }
        }
        return best;
    }

    private double urgentThreshold(double heldScore) {
        // Unarmed or dry: any gun. Otherwise require a real upgrade.
        boolean urgent = mob.heldGun() == null || mob.weaponDry();
        return urgent ? 0.0 : heldScore * UPGRADE_FACTOR;
    }

    private void grab(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof GenericGunItem gun) {
            ItemStack held = mob.getMainHandItem();
            if (held.getItem() instanceof GenericGunItem) {
                // Swap: drop the old iron where it lies.
                if (mob.level() instanceof ServerLevel dropLevel) {
                    mob.spawnAtLocation(dropLevel, held.copy());
                }
            }
            ItemStack taken = stack.copy();
            taken.setCount(1);
            if (!taken.has(com.techguns.techguns3.registry.TGDataComponents.AMMO.get())) {
                taken.set(com.techguns.techguns3.registry.TGDataComponents.AMMO.get(),
                        gun.def().magazine());
            }
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, taken);
            stack.shrink(1);
            if (stack.isEmpty()) entity.discard();
        } else {
            GenericGunItem held = mob.heldGun();
            if (held != null && stack.is(held.ammoItem())) {
                mob.addSpareMagazines(stack.getCount());
                entity.discard();
            }
        }
    }

    static double gunScore(GunStats stats) {
        return stats.baseDamage() * stats.pellets() * (1.0 + stats.magazineSize() / 64.0);
    }
}
