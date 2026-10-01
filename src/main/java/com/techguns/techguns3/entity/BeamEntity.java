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
import org.joml.Vector3f;

/**
 * Straight hitscan beam link (NDR / laser). Same sync pattern as
 * {@link TeslaArcEntity} (static geometry, seeded once) but rendered as a
 * straight colored quad, not a fractal arc.
 */
public class BeamEntity extends Entity {
    private static final EntityDataAccessor<org.joml.Vector3fc> END =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);

    private int life = 5;

    public BeamEntity(EntityType<BeamEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public BeamEntity(Level level, Vec3 from, Vec3 to, int color) {
        this(TGEntities.BEAM.get(), level);
        setPos(from.x, from.y, from.z);
        entityData.set(END, new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z)));
        entityData.set(COLOR, color);
    }

    public org.joml.Vector3fc endOffset() { return entityData.get(END); }
    public int color() { return entityData.get(COLOR); }
    public float fade() { return Math.max(0.0f, Math.min(1.0f, life / 5.0f)); }

    /**
     * Sustained-beam refresh (NDRProjectile teleport 1:1): re-anchor at the
     * shooter's muzzle, re-aim at the new end, reset life so the beam never
     * gaps while held. Synced data carries it to every viewer.
     */
    public void updateEnd(Vec3 from, Vec3 to) {
        setPos(from.x, from.y, from.z);
        entityData.set(END, new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z)));
        life = 5;
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
        builder.define(END, new Vector3f());
        builder.define(COLOR, 0xFF2A1A);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", 5);
        entityData.set(END, new Vector3f(
                input.getFloatOr("EndX", 0), input.getFloatOr("EndY", 0), input.getFloatOr("EndZ", 0)));
        entityData.set(COLOR, input.getIntOr("Color", 0xFF2A1A));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        var end = endOffset();
        output.putFloat("EndX", end.x());
        output.putFloat("EndY", end.y());
        output.putFloat("EndZ", end.z());
        output.putInt("Color", color());
    }
}
