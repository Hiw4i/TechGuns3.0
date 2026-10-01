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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Flying body chunk for GORE kills. Port of the 1.12 {@code FlyingGibs} as a
 * real synced entity (not client-only like the original): every viewer sees
 * the same chunks, no extra packets needed beyond vanilla tracking.
 *
 * <p>Physics mirror {@link ShellCasingEntity} on purpose — manual integration
 * with {@code noPhysics} + {@code setPos}, because {@code Entity.move()} is
 * forbidden on the client. Six biped-style parts (head, torso, arms, legs)
 * like {@code ModelGibsBiped}; unlisted mobs reuse the same generic chunks.</p>
 */
public class FlyingGibEntity extends Entity {
    /** Visible lifetime, ticked symmetrically on both sides so the end fade matches. */
    public static final int MAX_LIFE = 110;
    /** Last ticks spent squashing into the ground before the discard. */
    public static final int FADE_TICKS = 25;

    private static final EntityDataAccessor<Integer> PART =
            SynchedEntityData.defineId(FlyingGibEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(FlyingGibEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(FlyingGibEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(FlyingGibEntity.class, EntityDataSerializers.INT);

    private int age;

    public FlyingGibEntity(EntityType<FlyingGibEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public FlyingGibEntity(Level level, Vec3 pos, Vec3 velocity, int part, int color, float scale) {
        this(TGEntities.FLYING_GIB.get(), level);
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(velocity);
        entityData.set(PART, Math.max(0, Math.min(5, part)));
        entityData.set(COLOR, color);
        entityData.set(SCALE, scale);
        entityData.set(SEED, level.getRandom().nextInt());
    }

    public int part() {
        return entityData.get(PART);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public float gibScale() {
        return entityData.get(SCALE);
    }

    public int seed() {
        return entityData.get(SEED);
    }

    /** 1 while flying, 0 when fully squashed. The renderer derives the end squash from this. */
    public float endFade() {
        return Math.max(0.0f, Math.min(1.0f, (MAX_LIFE - age) / (float) FADE_TICKS));
    }

    /** Base dimensions per part, before victim scale. Big readable chunks: head cube, torso slab, limb bars. */
    public static Vec3 dimsFor(int part) {
        return switch (part) {
            case 0 -> new Vec3(0.42, 0.42, 0.42);
            case 1 -> new Vec3(0.55, 0.75, 0.38);
            case 2, 3 -> new Vec3(0.30, 0.70, 0.30);
            default -> new Vec3(0.32, 0.74, 0.32);
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (++age >= (level().isClientSide() ? MAX_LIFE + 10 : MAX_LIFE)) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement().scale(0.985).add(0, -0.07, 0);
        // Ground chunks barely roll: heavy friction once slow and low.
        if (motion.lengthSqr() < 0.01 && onGroundGuess()) {
            motion = Vec3.ZERO;
        }
        double nx = getX() + motion.x;
        double ny = getY() + motion.y;
        double nz = getZ() + motion.z;
        Vec3 dims = dimsFor(part()).scale(gibScale() * 0.5);
        if (!level().noCollision(this, gibBox(nx, ny, nz, dims))) {
            if (motion.y < -0.12) {
                motion = new Vec3(motion.x * 0.55, -motion.y * 0.42, motion.z * 0.55);
            } else {
                motion = new Vec3(motion.x * 0.6, 0, motion.z * 0.6);
                if (motion.lengthSqr() < 0.0006) motion = Vec3.ZERO;
            }
            nx = getX() + motion.x;
            ny = getY() + motion.y;
            nz = getZ() + motion.z;
            if (!level().noCollision(this, gibBox(nx, ny, nz, dims))) {
                setDeltaMovement(motion);
                return;
            }
        }
        setDeltaMovement(motion);
        setPos(nx, ny, nz);
    }

    private boolean onGroundGuess() {
        Vec3 dims = dimsFor(part()).scale(gibScale() * 0.5);
        return !level().noCollision(this,
                gibBox(getX(), getY() - 0.05, getZ(), dims));
    }

    private static AABB gibBox(double x, double y, double z, Vec3 half) {
        return new AABB(x - half.x, y, z - half.z, x + half.x, y + half.y * 2, z + half.z);
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
        builder.define(PART, 0);
        builder.define(COLOR, 0xC81818);
        builder.define(SCALE, 1.0f);
        builder.define(SEED, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getIntOr("Age", 0);
        entityData.set(PART, input.getIntOr("Part", 0));
        entityData.set(COLOR, input.getIntOr("Color", 0xC81818));
        entityData.set(SCALE, input.getFloatOr("Scale", 1.0f));
        entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", age);
        output.putInt("Part", part());
        output.putInt("Color", color());
        output.putFloat("Scale", gibScale());
        output.putInt("Seed", seed());
    }
}
