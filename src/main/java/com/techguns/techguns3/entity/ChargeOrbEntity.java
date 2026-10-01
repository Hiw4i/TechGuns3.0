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
 * TFG charge orb (TFGChargeStart FX 1:1 in spirit): a green plasma ball
 * hovering at the muzzle while LMB is held, growing with charge 0..1.
 * The server spawns it on charge start, re-anchors + rescales it every tick,
 * and discards it on release (the shot itself carries the scaled damage).
 */
public class ChargeOrbEntity extends Entity {
    private static final EntityDataAccessor<Float> CHARGE =
            SynchedEntityData.defineId(ChargeOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(ChargeOrbEntity.class, EntityDataSerializers.INT);

    private int life = 8;

    public ChargeOrbEntity(EntityType<ChargeOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ChargeOrbEntity(Level level, Vec3 at, float charge) {
        this(TGEntities.CHARGE_ORB.get(), level);
        setPos(at.x, at.y, at.z);
        entityData.set(CHARGE, charge);
        entityData.set(SEED, level.getRandom().nextInt());
    }

    public float charge() {
        return entityData.get(CHARGE);
    }

    /** Re-anchor at the muzzle + update growth. Resets life so it never gaps. */
    public void updateCharge(Vec3 at, float charge) {
        setPos(at.x, at.y, at.z);
        entityData.set(CHARGE, charge);
        life = 8;
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
    public boolean canBeHitByProjectile() { return false; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE, 0.0f);
        builder.define(SEED, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", 8);
        entityData.set(CHARGE, input.getFloatOr("Charge", 0.0f));
        entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putFloat("Charge", charge());
        output.putInt("Seed", entityData.get(SEED));
    }
}
