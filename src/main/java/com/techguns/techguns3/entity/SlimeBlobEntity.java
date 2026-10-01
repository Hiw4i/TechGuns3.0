package com.techguns.techguns3.entity;

import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.registry.TGEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Slime blob anchored to a block face. Sustained fire into the spot grows it;
 * at critical mass it pops into a block-damaging explosion.
 */
public class SlimeBlobEntity extends Entity {
    private static final EntityDataAccessor<Integer> FACE =
            SynchedEntityData.defineId(SlimeBlobEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GROWTH =
            SynchedEntityData.defineId(SlimeBlobEntity.class, EntityDataSerializers.FLOAT);

    public SlimeBlobEntity(EntityType<SlimeBlobEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public SlimeBlobEntity(Level level, BlockPos block, Direction face, float growth) {
        this(TGEntities.SLIME_BLOB.get(), level);
        Vec3 center = Vec3.atCenterOf(block)
                .add(face.getStepX() * 0.55, face.getStepY() * 0.55, face.getStepZ() * 0.55);
        setPos(center.x, center.y, center.z);
        entityData.set(FACE, face.ordinal());
        entityData.set(GROWTH, growth);
    }

    public Direction face() {
        Direction[] faces = Direction.values();
        int i = entityData.get(FACE);
        return faces[Math.max(0, Math.min(faces.length - 1, i))];
    }

    public float growth() {
        return entityData.get(GROWTH);
    }

    /**
     * Feed slime at a block face. Stacks onto a nearby blob or seeds a new one;
     * pops when growth reaches 1. Returns true if a pop happened.
     */
    public static boolean growAt(ServerLevel level, BlockPos block, Direction face, float amount) {
        if (amount <= 0.0f) return false;
        Vec3 anchor = Vec3.atCenterOf(block)
                .add(face.getStepX() * 0.55, face.getStepY() * 0.55, face.getStepZ() * 0.55);
        SlimeBlobEntity existing = null;
        double best = 1.44;
        for (SlimeBlobEntity blob : level.getEntitiesOfClass(SlimeBlobEntity.class,
                new AABB(anchor.x - 1, anchor.y - 1, anchor.z - 1,
                        anchor.x + 1, anchor.y + 1, anchor.z + 1))) {
            if (blob.face() != face) continue;
            double d = blob.position().distanceToSqr(anchor);
            if (d < best) {
                best = d;
                existing = blob;
            }
        }
        if (existing != null) {
            float grown = existing.growth() + amount;
            if (grown >= 1.0f) {
                pop(level, existing);
                return true;
            }
            existing.entityData.set(GROWTH, grown);
            return false;
        }
        level.addFreshEntity(new SlimeBlobEntity(level, block, face, Math.max(0.05f, amount)));
        return false;
    }

    private static void pop(ServerLevel level, SlimeBlobEntity blob) {
        Vec3 at = blob.position();
        // Detonate inside the block mass, not above it: rays attenuate fast.
        net.minecraft.core.Direction face = blob.face();
        double cx = at.x - face.getStepX() * 0.55;
        double cy = at.y - face.getStepY() * 0.55;
        double cz = at.z - face.getStepZ() * 0.55;
        float power = 1.5f + 1.5f * Math.min(1.5f, blob.growth());
        blob.discard();
        level.addFreshEntity(new MuzzleFlashEntity(level, at.x, at.y, at.z,
                MuzzleFlashEntity.WHITE, 0.6f));
        FxParticles.slimeBurst(level, at, 14);
        FxParticles.embers(level, at, 10);
        FxParticles.smoke(level, at, 6);
        level.explode(null, cx, cy, cz, power, false, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public void tick() {
        super.tick();
        // Anchor check: if the block face this blob grew on is gone (mined,
        // exploded, piston-moved), fizzle out instead of hovering in mid-air.
        if (level().isClientSide() || tickCount % 10 != 0) return;
        Direction face = face();
        BlockPos anchor = BlockPos.containing(
                getX() - face.getStepX() * 0.55,
                getY() - face.getStepY() * 0.55,
                getZ() - face.getStepZ() * 0.55);
        if (!level().getBlockState(anchor).isFaceSturdy(level(), anchor, face)) {
            FxParticles.slimeBurst((ServerLevel) level(), position(), 3);
            discard();
        }
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
        builder.define(FACE, Direction.UP.ordinal());
        builder.define(GROWTH, 0.1f);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(FACE, input.getIntOr("Face", Direction.UP.ordinal()));
        entityData.set(GROWTH, input.getFloatOr("Growth", 0.1f));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Face", entityData.get(FACE));
        output.putFloat("Growth", growth());
    }
}
