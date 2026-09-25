package com.techguns.techguns3.entity;

import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.registry.TGItems;
import com.techguns.techguns3.registry.TGSounds;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Wave-1 bullet. Collapses every plain 1.12 {@code GenericProjectile} shooter
 * (pistol/rifle/shotgun rounds) into one entity; exotic types (rockets, lasers,
 * tesla, bio, grenades) return as subclasses in later waves.
 *
 * <p>1.12 mechanics kept: speed in blocks/tick, TTL, linear damage drop,
 * 0.99 air friction, 0.85 in water + splash sound, optional gravity,
 * shooter exclusion, block-material impact sounds. Dropped for now:
 * ricochet, penetration vs armor (kept as data for later), headshots,
 * NPC factions/revenge AI, muzzle-light pulses, impact FX packets.</p>
 */
public class BulletProjectile extends Projectile implements ItemSupplier {

    /** Tunables per shot, built from {@code GunStats} by the firing gun. */
    public record Spec(float damage, float speed, int ttlTicks,
                       float dropStart, float dropEnd, float dropMin,
                       double gravity, float penetration) {}

    private static final Set<SoundType> WOOD = Set.of(
            SoundType.WOOD, SoundType.LADDER, SoundType.BAMBOO_WOOD,
            SoundType.NETHER_WOOD, SoundType.CHERRY_WOOD, SoundType.SCAFFOLDING);
    private static final Set<SoundType> GLASS = Set.of(
            SoundType.GLASS, SoundType.AMETHYST, SoundType.AMETHYST_CLUSTER,
            SoundType.SMALL_AMETHYST_BUD, SoundType.MEDIUM_AMETHYST_BUD, SoundType.LARGE_AMETHYST_BUD);
    private static final Set<SoundType> METAL = Set.of(
            SoundType.METAL, SoundType.ANVIL, SoundType.COPPER, SoundType.CHAIN,
            SoundType.NETHERITE_BLOCK, SoundType.LODESTONE);
    private static final Set<SoundType> DIRT = Set.of(
            SoundType.SAND, SoundType.GRAVEL, SoundType.SNOW, SoundType.SOUL_SAND,
            SoundType.SOUL_SOIL, SoundType.MUD, SoundType.GRASS, SoundType.ROOTED_DIRT);

    private Spec spec = new Spec(5.0f, 2.0f, 40, 20.0f, 40.0f, 5.0f, 0.0, 0.0f);
    private int ttlLeft;
    private double startX;
    private double startY;
    private double startZ;
    private boolean startInit;
    private boolean splashed;

    public BulletProjectile(EntityType<BulletProjectile> type, Level level) {
        super(type, level);
    }

    public BulletProjectile(Level level, LivingEntity shooter, Vec3 spawn,
            float yaw, float pitch, float spread, Spec spec) {
        this(TGEntities.BULLET.get(), level);
        this.spec = spec;
        this.ttlLeft = spec.ttlTicks();
        setOwner(shooter);
        // 1.12 spread: (spread - 2*rand*spread) * 40 degrees on yaw and pitch.
        float yawOff = (spread - random.nextFloat() * 2.0f * spread) * 40.0f;
        float pitchOff = (spread - random.nextFloat() * 2.0f * spread) * 40.0f;
        float y = yaw + yawOff;
        float p = pitch + pitchOff;
        setPos(spawn.x, spawn.y, spawn.z);
        setRot(y, p);
        double f = Math.cos(p * Math.PI / 180.0);
        Vec3 motion = new Vec3(-Math.sin(y * Math.PI / 180.0) * f, -Math.sin(p * Math.PI / 180.0),
                Math.cos(y * Math.PI / 180.0) * f).scale(spec.speed());
        setDeltaMovement(motion);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != getOwner()
                && entity.canBeHitByProjectile()
                && (!(getOwner() instanceof LivingEntity living) || !living.isAlliedTo(entity))
                && (!(getOwner() instanceof TurretHeadEntity turret)
                    || !(entity instanceof LivingEntity target) || turret.canAttackTarget(target));
    }

    @Override
    public void tick() {
        if (!startInit && !level().isClientSide()) {
            startX = getX();
            startY = getY();
            startZ = getZ();
            startInit = true;
        }
        super.tick();

        // The server owns lifetime and collision. Client spawn packets do not carry
        // the per-shot Spec, so a client must not discard its visual tracer at tick 1.
        if (!level().isClientSide()) {
            ttlLeft--;
            if (ttlLeft <= 0) {
                discard();
                return;
            }
        }

        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onBulletHit(hit);
                return;
            }
        }

        setPos(to.x, to.y, to.z);

        // Visuals come from the custom tracer renderer; no stock particles here.

        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setRot((float) (Math.atan2(motion.x, motion.z) * 180.0 / Math.PI),
                (float) (Math.atan2(motion.y, horizontal) * 180.0 / Math.PI));

        float friction = 0.99f;
        if (isInWater()) {
            friction = 0.85f;
            if (!splashed) {
                splashed = true;
                if (!level().isClientSide()) {
                    level().playSound(null, getX(), getY(), getZ(),
                            TGSounds.IMPACT_WATER.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
                }
            }
        }
        setDeltaMovement(motion.scale(friction));
        if (spec.gravity() != 0.0) {
            setDeltaMovement(getDeltaMovement().add(0.0, -spec.gravity(), 0.0));
        }
    }

    private void onBulletHit(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            if (level() instanceof ServerLevel serverLevel) {
                Entity target = entityHit.getEntity();
                DamageSource source;
                Entity owner = getOwner();
                if (owner instanceof LivingEntity livingOwner) {
                    source = damageSources().mobProjectile(this, livingOwner);
                } else {
                    source = damageSources().thrown(this, owner != null ? owner : this);
                }
                Vec3 impact = entityHit.getLocation();
                target.hurtServer(serverLevel, source, damageAt(impact.x, impact.y, impact.z));
                level().addFreshEntity(new MuzzleFlashEntity(level(),
                        impact.x, impact.y, impact.z, MuzzleFlashEntity.WHITE, 0.3f));
            }
            discard();
        } else if (hit instanceof BlockHitResult blockHit) {
            if (!level().isClientSide()) {
                SoundType sound = level().getBlockState(blockHit.getBlockPos()).getSoundType();
                SoundEvent impact = impactFor(sound).get();
                level().playSound(null, blockHit.getLocation().x, blockHit.getLocation().y,
                        blockHit.getLocation().z, impact, SoundSource.BLOCKS, 1.0f, 1.0f);
                Vec3 at = blockHit.getLocation();
                level().addFreshEntity(new MuzzleFlashEntity(level(),
                        at.x, at.y, at.z, MuzzleFlashEntity.WHITE, 0.25f));
            }
            discard();
        }
    }

    private static net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> impactFor(
            SoundType sound) {
        if (WOOD.contains(sound)) return TGSounds.IMPACT_WOOD;
        if (GLASS.contains(sound)) return TGSounds.IMPACT_GLASS;
        if (METAL.contains(sound)) return TGSounds.IMPACT_METAL;
        if (DIRT.contains(sound)) return TGSounds.IMPACT_DIRT;
        return TGSounds.IMPACT_ROCK;
    }

    private float damageAt(double x, double y, double z) {
        if (spec.dropEnd() <= 0.0f) {
            return spec.damage();
        }
        double distance = Math.sqrt((x - startX) * (x - startX)
                + (y - startY) * (y - startY) + (z - startZ) * (z - startZ));
        if (distance <= spec.dropStart()) {
            return spec.damage();
        }
        if (distance >= spec.dropEnd()) {
            return spec.dropMin();
        }
        float factor = 1.0f - (float) ((distance - spec.dropStart()) / (spec.dropEnd() - spec.dropStart()));
        return spec.dropMin() + (spec.damage() - spec.dropMin()) * factor;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    /** Inventory representation only; in flight the tracer is rendered with particles. */
    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(TGItems.RIFLE_ROUNDS.get());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        spec = new Spec(
                input.getFloatOr("Damage", 5.0f),
                input.getFloatOr("Speed", 2.0f),
                input.getIntOr("TTL", 40),
                input.getFloatOr("DropStart", 20.0f),
                input.getFloatOr("DropEnd", 40.0f),
                input.getFloatOr("DropMin", 5.0f),
                input.getDoubleOr("Gravity", 0.0),
                input.getFloatOr("Penetration", 0.0f));
        ttlLeft = input.getIntOr("TTLLeft", spec.ttlTicks());
        startX = input.getDoubleOr("StartX", getX());
        startY = input.getDoubleOr("StartY", getY());
        startZ = input.getDoubleOr("StartZ", getZ());
        startInit = input.getBooleanOr("StartInit", true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Damage", spec.damage());
        output.putFloat("Speed", spec.speed());
        output.putInt("TTL", spec.ttlTicks());
        output.putFloat("DropStart", spec.dropStart());
        output.putFloat("DropEnd", spec.dropEnd());
        output.putFloat("DropMin", spec.dropMin());
        output.putDouble("Gravity", spec.gravity());
        output.putFloat("Penetration", spec.penetration());
        output.putInt("TTLLeft", ttlLeft);
        output.putDouble("StartX", startX);
        output.putDouble("StartY", startY);
        output.putDouble("StartZ", startZ);
        output.putBoolean("StartInit", startInit);
    }
}
