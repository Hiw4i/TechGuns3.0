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
import org.joml.Vector3f;

/**
 * One tesla arc link as a 4-tick entity (muzzle to target, or target to chain).
 * The renderer rebuilds the identical jittered polyline from the synced seed,
 * so arcs look the same for every viewer with zero per-tick traffic.
 */
public class TeslaArcEntity extends Entity {
    private static final EntityDataAccessor<org.joml.Vector3fc> END =
            SynchedEntityData.defineId(TeslaArcEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(TeslaArcEntity.class, EntityDataSerializers.INT);

    private int life = 4;

    public TeslaArcEntity(EntityType<TeslaArcEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public TeslaArcEntity(Level level, double x, double y, double z,
                          double endX, double endY, double endZ, int seed) {
        this(TGEntities.TESLA_ARC.get(), level);
        setPos(x, y, z);
        entityData.set(END, new Vector3f((float) (endX - x), (float) (endY - y), (float) (endZ - z)));
        entityData.set(SEED, seed);
    }

    public org.joml.Vector3fc endOffset() {
        return entityData.get(END);
    }

    public int seed() {
        return entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && --life <= 0) discard();
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(END, new Vector3f());
        builder.define(SEED, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", 4);
        entityData.set(END, new Vector3f(
                input.getFloatOr("EndX", 0), input.getFloatOr("EndY", 0), input.getFloatOr("EndZ", 0)));
        entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        org.joml.Vector3fc end = endOffset();
        output.putFloat("EndX", end.x());
        output.putFloat("EndY", end.y());
        output.putFloat("EndZ", end.z());
        output.putInt("Seed", seed());
    }
}
