package com.techguns.techguns3.entity;

import com.techguns.techguns3.registry.TGEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Ejected brass: tiny tumbling box with gravity + ground bounce, visual only.
 * 30 ticks max, never collides, never persists — TG2 shell-eject feel without
 * touching gameplay.
 */
public class ShellCasingEntity extends Entity {
    private static final EntityDataAccessor<Float> SPIN =
            SynchedEntityData.defineId(ShellCasingEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFE = 30;
    private int age;

    public ShellCasingEntity(EntityType<ShellCasingEntity> type, Level level) {
        super(type, level);
        // Visual only: never run physics simulation. Both sides integrate the
        // toss manually with setPos; Entity.move() is forbidden on the client
        // and crashes the game (IllegalStateException on first casing tick).
        noPhysics = true;
    }

    public ShellCasingEntity(Level level, Vec3 pos, Vec3 velocity, float spin) {
        this(TGEntities.SHELL_CASING.get(), level);
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(velocity);
        entityData.set(SPIN, spin);
    }

    public float spin() {
        return entityData.get(SPIN);
    }

    public float ageFade() {
        return 1.0f - age / (float) MAX_LIFE;
    }

    @Override
    public void tick() {
        super.tick();
        if (++age >= (level().isClientSide() ? MAX_LIFE + 10 : MAX_LIFE)) {
            discard();
            return;
        }
        // Brass gravity + air drag, integrated manually. Entity.move() may not
        // be called on the client, so ground contact is tested with noCollision
        // and the position is applied with setPos on both sides.
        Vec3 motion = getDeltaMovement().scale(0.98).add(0, -0.06, 0);
        double nx = getX() + motion.x;
        double ny = getY() + motion.y;
        double nz = getZ() + motion.z;
        if (!level().noCollision(this, casingBox(nx, ny, nz))) {
            if (motion.y < -0.08) {
                motion = new Vec3(motion.x * 0.5, -motion.y * 0.35, motion.z * 0.5);
            } else {
                motion = new Vec3(motion.x * 0.5, 0, motion.z * 0.5);
                if (motion.lengthSqr() < 0.0004) motion = Vec3.ZERO;
            }
            nx = getX() + motion.x;
            ny = getY() + motion.y;
            nz = getZ() + motion.z;
            if (!level().noCollision(this, casingBox(nx, ny, nz))) {
                setDeltaMovement(motion);
                return;
            }
        }
        setDeltaMovement(motion);
        setPos(nx, ny, nz);
    }

    private static net.minecraft.world.phys.AABB casingBox(double x, double y, double z) {
        return new net.minecraft.world.phys.AABB(x - 0.06, y - 0.03, z - 0.06, x + 0.06, y + 0.03, z + 0.06);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPIN, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getIntOr("Age", 0);
        entityData.set(SPIN, input.getFloatOr("Spin", 0.0f));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", age);
        output.putFloat("Spin", spin());
    }
}
