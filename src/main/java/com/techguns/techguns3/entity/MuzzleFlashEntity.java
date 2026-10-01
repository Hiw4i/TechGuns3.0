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
 * Brief star-flash at a muzzle or impact point. Baked TG2-style variation:
 * every flash carries a synced random seed so each viewer rebuilds the same
 * jittered star (roll + arm lengths), and a short 5-tick pop-and-fade so the
 * flash never hangs as a giant quad in someone's face.
 */
public class MuzzleFlashEntity extends Entity {
    public static final int WARM = 0xFFD96A;
    public static final int WHITE = 0xFFFFFF;
    public static final int CYAN = 0x7FEBFF;

    /** Visible lifetime: pop on tick 0, gone after tick 5. */
    public static final int MAX_LIFE = 5;

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.INT);
    /**
     * CE muzzle texture id (GunExtras.FLASH_*). 0 = procedural star (impacts).
     * Synced once like color/scale/seed.
     */
    private static final EntityDataAccessor<Integer> TEXTURE =
            SynchedEntityData.defineId(MuzzleFlashEntity.class, EntityDataSerializers.INT);

    private int life = MAX_LIFE;

    public MuzzleFlashEntity(EntityType<MuzzleFlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public MuzzleFlashEntity(Level level, double x, double y, double z, int color, float scale) {
        this(level, x, y, z, color, scale, level.getRandom().nextInt(), 0);
    }

    /** Muzzle flash with CE texture id (GunExtras.FLASH_*). */
    public MuzzleFlashEntity(Level level, double x, double y, double z, int color, float scale, int texture) {
        this(level, x, y, z, color, scale, level.getRandom().nextInt(), texture);
    }

    public MuzzleFlashEntity(Level level, double x, double y, double z, int color, float scale, int seed, int texture) {
        this(TGEntities.MUZZLE_FLASH.get(), level);
        setPos(x, y, z);
        entityData.set(COLOR, color);
        entityData.set(SCALE, scale);
        entityData.set(SEED, seed);
        entityData.set(TEXTURE, texture);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public float scale() {
        return entityData.get(SCALE);
    }

    public int seed() {
        return entityData.get(SEED);
    }

    public int texture() {
        return entityData.get(TEXTURE);
    }

    /** 0..life remaining; renderers derive pop scale + alpha from this. */
    public int lifeLeft() {
        return Math.max(0, life);
    }

    /** 1 at spawn, 0 when gone. Client and server tick symmetrically. */
    public float fade() {
        return Math.max(0.0f, Math.min(1.0f, life / (float) MAX_LIFE));
    }

    @Override
    public void tick() {
        super.tick();
        if (--life <= 0) discard();
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
        builder.define(COLOR, WHITE);
        builder.define(SCALE, 0.5f);
        builder.define(SEED, 0);
        builder.define(TEXTURE, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", MAX_LIFE);
        entityData.set(COLOR, input.getIntOr("Color", WHITE));
        entityData.set(SCALE, input.getFloatOr("Scale", 0.5f));
        entityData.set(SEED, input.getIntOr("Seed", 0));
        entityData.set(TEXTURE, input.getIntOr("MuzzleTexture", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putInt("Color", color());
        output.putFloat("Scale", scale());
        output.putInt("Seed", seed());
        output.putInt("MuzzleTexture", texture());
    }
}
