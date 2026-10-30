package com.techguns.techguns3.turret;

import com.techguns.techguns3.combat.BallisticCombat;
import com.techguns.techguns3.combat.GunServerLogic;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.world.item.Item;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Damageable, anchored turret head. Combat and ammunition remain server authoritative. */
public final class TurretHeadEntity extends Monster implements GeoEntity {
    /** Acquisition range in blocks (also the query box half-size). */
    private static final double TARGET_RANGE = 32;
    /** Armor plating factor: an iron plate absorbs a quarter of the damage. */
    private static final float TURRET_ARMOR_DR = 0.75f;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos anchor = BlockPos.ZERO;

    public TurretHeadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    public BlockPos anchor() { return anchor; }
    public void setAnchor(BlockPos anchor) { this.anchor = anchor.immutable(); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(anchor) instanceof TurretBaseBlockEntity base)) {
            discard();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        setPos(anchor.getX() + 0.5, anchor.getY() + 0.25, anchor.getZ() + 0.5);
        if (base.reloadTicks() > 0) {
            base.setReloadTicks(base.reloadTicks() - 1);
            return;
        }
        if (base.fireTicks() > 0) {
            base.setFireTicks(base.fireTicks() - 1);
            return;
        }
        if (level.hasNeighborSignal(anchor)) return;
        GenericGunItem gun = base.gun();
        if (gun == null) return;
        var stats = gun.stats();
        LivingEntity target = findTarget(level, base);
        if (target == null) return;
        if (base.loadedRounds() <= 0) {
            if (!reload(base, gun)) return;
            base.setReloadTicks(stats.reloadTimeTicks());
            level.playSound(null, getX(), getY(), getZ(), stats.reloadSound().get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        // Barrel tip ahead of the housing: never spawn the flash inside the head.
        Vec3 muzzle = GunServerLogic.muzzlePos(this);
        Vec3 delta = target.getEyePosition().subtract(muzzle);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(delta.y, horizontal));
        setYRot(yaw);
        setXRot(pitch);
        BallisticCombat.fire(level, this, stats, gun.extras(), null, 0.0f, 1.0f, -1,
                muzzle, yaw, pitch, SoundSource.BLOCKS, 1.0f);
        base.setLoadedRounds(base.loadedRounds() - 1);
        base.setFireTicks(stats.fireCooldownTicks());
    }

    private LivingEntity findTarget(ServerLevel level, TurretBaseBlockEntity base) {
        LivingEntity best = null;
        double bestDistance = TARGET_RANGE * TARGET_RANGE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(anchor).inflate(TARGET_RANGE), c -> canAttackTarget(c, base))) {
            double distance = distanceToSqr(candidate);
            if (distance < bestDistance && hasLineOfSight(candidate)) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Apply the same target policy to direct bullets and chained electrical arcs. */
    public boolean canAttackTarget(LivingEntity candidate) {
        return level().getBlockEntity(anchor) instanceof TurretBaseBlockEntity base
                && canAttackTarget(candidate, base);
    }

    private boolean canAttackTarget(LivingEntity candidate, TurretBaseBlockEntity base) {
        if (!candidate.isAlive() || candidate == this) return false;
        if (candidate instanceof Player player) {
            return base.pvp() && !player.isCreative() && !player.isSpectator()
                    && !player.getUUID().equals(base.owner());
        }
        if (candidate instanceof Animal) return base.attackAnimals();
        return candidate instanceof Monster && !(candidate instanceof TurretHeadEntity);
    }

    private static boolean reload(TurretBaseBlockEntity base, GenericGunItem gun) {
        var stats = gun.stats();
        Item empty = gun.emptyMagazineItem();
        if (stats.magazineFed() && empty != null && !base.canStoreOutput(empty)) return false;
        int wanted = stats.magazineFed() ? 1 : stats.magazineSize();
        int loaded = 0;
        for (int i = 0; i < TurretBaseBlockEntity.INPUT_END && loaded < wanted; i++) {
            ItemStack stack = base.getItem(i);
            if (!stack.isEmpty() && stack.is(gun.ammoItem())) {
                int take = Math.min(stack.getCount(), wanted - loaded);
                stack.shrink(take);
                loaded += take;
            }
        }
        if (loaded == 0) return false;
        if (stats.magazineFed() && empty != null) base.storeOutput(empty);
        base.setLoadedRounds(stats.magazineFed() ? stats.magazineSize() : loaded);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (level.getBlockEntity(anchor) instanceof TurretBaseBlockEntity base) {
            ItemStack armor = base.getItem(TurretBaseBlockEntity.ARMOR_SLOT);
            if (armor.is(TGItems.TURRET_ARMOR_IRON.get())) {
                amount *= TURRET_ARMOR_DR;
                int nextDamage = armor.getDamageValue() + 1;
                if (nextDamage >= armor.getMaxDamage()) armor.shrink(1);
                else armor.setDamageValue(nextDamage);
                base.setChanged();
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && level.getBlockEntity(anchor) instanceof TurretBaseBlockEntity base) {
            net.minecraft.world.Containers.dropContents(level, anchor, base);
            base.removeHead(level);
            level.destroyBlock(anchor, false);
        }
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("AnchorX", anchor.getX());
        output.putInt("AnchorY", anchor.getY());
        output.putInt("AnchorZ", anchor.getZ());
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        anchor = new BlockPos(input.getIntOr("AnchorX", 0), input.getIntOr("AnchorY", 0), input.getIntOr("AnchorZ", 0));
    }
}
