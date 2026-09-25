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

/**
 * Brief star-flash at a muzzle or impact point. 3 ticks, orientation-free
 * crossed quads, warm for powder guns and cyan for the tesla.
 */
public class MuzzleFlashEntity extends Entity {
    public static final int WARM = 0xFFD96A;
    public static final int WHITE = 0xFFFFFF;
    public static final int CYAN = 0x7FEBFF;

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.FLOAT);

    private int life = 3;

    public MuzzleFlashEntity(EntityType<MuzzleFlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public MuzzleFlashEntity(Level level, double x, double y, double z, int color, float scale) {
        this(TGEntities.MUZZLE_FLASH.get(), level);
        setPos(x, y, z);
        entityData.set(COLOR, color);
        entityData.set(SCALE, scale);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public float scale() {
        return entityData.get(SCALE);
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
        builder.define(COLOR, WHITE);
        builder.define(SCALE, 0.5f);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", 3);
        entityData.set(COLOR, input.getIntOr("Color", WHITE));
        entityData.set(SCALE, input.getFloatOr("Scale", 0.5f));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putInt("Color", color());
        output.putFloat("Scale", scale());
    }
}
