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
 * Cinematic explosion billboard (TGExplosion look): a 4x4-frame fireball that
 * blooms to the blast radius over ~12 ticks, tinted per weapon (orange for
 * rockets, green for TFG, white-yellow for nukes). Visual only.
 */
public class BlastEntity extends Entity {
    public static final int MAX_LIFE = 12;

    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(BlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TINT =
            SynchedEntityData.defineId(BlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(BlastEntity.class, EntityDataSerializers.INT);

    private int life = MAX_LIFE;

    public BlastEntity(EntityType<BlastEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public BlastEntity(Level level, double x, double y, double z, float radius, int tint) {
        this(TGEntities.BLAST.get(), level);
        setPos(x, y, z);
        entityData.set(RADIUS, radius);
        entityData.set(TINT, tint);
        entityData.set(SEED, level.getRandom().nextInt());
    }

    public float radius() { return entityData.get(RADIUS); }
    public int tint() { return entityData.get(TINT); }
    public int seed() { return entityData.get(SEED); }

    /** 0 at spawn → 1 when gone. */
    public float progress() {
        return 1.0f - Math.max(0.0f, Math.min(1.0f, life / (float) MAX_LIFE));
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
        builder.define(RADIUS, 4.0f);
        builder.define(TINT, 0xFFB43A);
        builder.define(SEED, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", MAX_LIFE);
        entityData.set(RADIUS, input.getFloatOr("Radius", 4.0f));
        entityData.set(TINT, input.getIntOr("Tint", 0xFFB43A));
        entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putFloat("Radius", radius());
        output.putInt("Tint", tint());
        output.putInt("Seed", seed());
    }
}
