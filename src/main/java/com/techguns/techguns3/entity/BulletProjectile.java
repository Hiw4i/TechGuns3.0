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
                       double gravity, float penetration,
                       float weaveAmp, float weaveFreq, int homingTargetId, float homingTurn,
                       float slimeAmount, int poisonDuration, int poisonAmp, int fireTicks, int tracerColor,
                       float bulk,
                       // --- wave-2: explosions / knockback / radiation ---
                       float explosiveMax, float explosiveMin, float explosiveR1, float explosiveR2,
                       float explosiveBlockDamage, boolean explosiveNuke,
                       float knockback, boolean ignoreIframes, int radiationTicks,
                       // --- wave-2 visuals: sonic main blade draws the wave rings ---
                       boolean sonicMain) {
        /** Straight flight, no slime (guns opt in via stats). */
        public Spec(float damage, float speed, int ttlTicks,
                    float dropStart, float dropEnd, float dropMin,
                    double gravity, float penetration) {
            this(damage, speed, ttlTicks, dropStart, dropEnd, dropMin, gravity,
                    penetration, 0.0f, 0.0f, -1, 0.2f, 0.0f, 0, 0, 0, 0xFFD9A8, 1.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, 0.0f, false, 0, false);
        }

        public boolean hasExplosive() { return explosiveMax > 0.0f && explosiveR2 > 0.0f; }

        /** Copy flagged as the sonic main blade (draws the CE wave rings). */
        public Spec withSonicMain() {
            return new Spec(damage, speed, ttlTicks, dropStart, dropEnd, dropMin,
                    gravity, penetration, weaveAmp, weaveFreq, homingTargetId, homingTurn,
                    slimeAmount, poisonDuration, poisonAmp, fireTicks, tracerColor, bulk,
                    explosiveMax, explosiveMin, explosiveR1, explosiveR2, explosiveBlockDamage,
                    explosiveNuke, knockback, ignoreIframes, radiationTicks, true);
        }
    }

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
    private int age;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_COLOR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    BulletProjectile.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_BULK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    BulletProjectile.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_MAIN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    BulletProjectile.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
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
        entityData.set(DATA_COLOR, spec.tracerColor());
        entityData.set(DATA_BULK, spec.bulk());
        entityData.set(DATA_MAIN, spec.sonicMain());
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
        // CE projectiles never collide mid-air: pellets must not eat each other
        // (shotgun/sonic fans) and stray bullets must not block fresh shots.
        if (entity instanceof BulletProjectile) return false;
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
        if (!level().isClientSide()) {
            steer();
        }
        Vec3 to = from.add(getDeltaMovement());
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onBulletHit(hit);
                return;
            }
        }

        setPos(to.x, to.y, to.z);

        // TG2 flare-trail per ammunition kind. The ribbon renderer stays
        // short; these sprites sell the stream. Client only, zero gameplay.
        if (level().isClientSide()) {
            try {
                trailSprite(to);
            } catch (Exception ignored) {
            }
        }

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

    /**
     * Per-kind trail sprite. The kind is read from the synced tracer color
     * (the single source of truth in {@code GunFxPresets}), so no extra sync
     * is needed: fire leaves a continuous tongue of flame, bio a fat spore,
     * plain rounds a tiny spark.
     */
    private void trailSprite(Vec3 at) {
        int c = tracerColor();
        if (c == com.techguns.techguns3.fx.GunFxPresets.TRACER_FIRE) {
            float wob = (float) tickCount * 12.9898f;
            float ox = (float) Math.sin(wob) * 0.09f;
            float oy = (float) Math.sin(wob * 1.7f) * 0.09f;
            float oz = (float) Math.cos(wob * 0.9f) * 0.09f;
            level().addParticle(new com.techguns.techguns3.registry.TGParticles.FlameOptions(
                            0xFFE9A8, 0xFF5A1A, 0.30f, 0.07f, 14, -0.02f, 0.94f),
                    at.x, at.y, at.z, 0, 0.02, 0);
            level().addParticle(new com.techguns.techguns3.registry.TGParticles.FlameOptions(
                            0xFFF3C4, 0xFF7A1A, 0.22f, 0.05f, 12, -0.02f, 0.94f),
                    at.x + ox, at.y + oy, at.z + oz, 0, 0.03, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                                0xFFE9A8, 0xFF5A1A, 0.10f, 0.02f, 14, 0.06f, 0.95f),
                        at.x, at.y, at.z, 0, 0.05, 0);
            }
        } else if (c == com.techguns.techguns3.fx.GunFxPresets.TRACER_BIO) {
            if (tickCount % 2 == 0) {
                level().addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                                c, 0x2FA32F, 0.13f, 0.03f, 9, 0.02f, 0.95f),
                        at.x, at.y, at.z, 0, 0, 0);
            }
        } else if (c == com.techguns.techguns3.fx.GunFxPresets.TRACER_TFG
                || c == com.techguns.techguns3.fx.GunFxPresets.TRACER_LASER
                || c == com.techguns.techguns3.fx.GunFxPresets.TRACER_GAUSS
                || c == com.techguns.techguns3.fx.GunFxPresets.TRACER_ADVANCED
                || c == com.techguns.techguns3.fx.GunFxPresets.TRACER_SONIC) {
            // Wave-2 energy trails: fat colored glow every tick (CE Albedo light feel).
            level().addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                            c, c, 0.13f, 0.03f, 9, 0.0f, 0.96f),
                    at.x, at.y, at.z, 0, 0, 0);
        } else if (c == com.techguns.techguns3.fx.GunFxPresets.TRACER_ROCKET) {
            // Rocket exhaust: orange glow + grey smoke puff.
            level().addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                            0xFFE9A8, 0xFF5A1A, 0.16f, 0.03f, 10, 0.02f, 0.95f),
                    at.x, at.y, at.z, 0, 0, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(new com.techguns.techguns3.registry.TGParticles.PuffOptions(
                                0xB0B0B0, 0x404040, 0.16f, 0.40f, 20, -0.01f, 0.97f),
                        at.x, at.y, at.z, 0, 0.05, 0);
            }
        } else if ((tickCount % 2) == 0) {
            level().addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                            c, c, 0.075f, 0.015f, 6, 0.0f, 1.0f),
                    at.x, at.y, at.z, 0, 0, 0);
        }
    }

    /** Curved flight: sine weave plus gentle homing toward a synced target id. */
    private void steer() {
        age++;
        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        if (speed < 1e-6) return;
        if (spec.weaveAmp() > 0.0f) {
            Vec3 dir = motion.normalize();
            Vec3 up = Math.abs(dir.y) > 0.94 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 side = dir.cross(up).normalize();
            Vec3 pushed = motion.add(side.scale(Math.sin(age * spec.weaveFreq()) * spec.weaveAmp()));
            motion = pushed.normalize().scale(speed);
        }
        if (spec.homingTargetId() >= 0) {
            Entity target = level().getEntity(spec.homingTargetId());
            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3 want = living.getEyePosition().subtract(position()).normalize().scale(speed);
                motion = motion.lerp(want, spec.homingTurn()).normalize().scale(speed);
            }
        }
        setDeltaMovement(motion);
    }

    private void onBulletHit(HitResult hit) {
        if (!level().isClientSide()) {
            // Per-impact trace: debug only, this fires on every landed bullet.
            com.techguns.techguns3.TechGuns3.LOGGER.debug(
                    "[TGTEST] bullet hit {} at {} target={}",
                    hit.getType(), hit.getLocation(),
                    hit instanceof EntityHitResult e ? e.getEntity().toString() : "-");
        }
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
                float dmg = damageAt(impact.x, impact.y, impact.z);
                boolean bio = spec.slimeAmount() > 0.0f || spec.poisonDuration() > 0;
                if (spec.ignoreIframes() && target instanceof LivingEntity livingTarget) {
                    // Sonic: pierces the post-hit damage cooldown like the original
                    // (ignoreHurtresistTime). Modern iframes live in
                    // LivingEntity.damageCooldownTime (public), not invulnerableTime.
                    livingTarget.damageCooldownTime = 0;
                }
                target.hurtServer(serverLevel, source, dmg);
                if (spec.knockback() > 0.0f && target instanceof LivingEntity livingKb) {
                    Vec3 push = getDeltaMovement().normalize().scale(spec.knockback() * 0.6);
                    livingKb.push(push.x, push.y + spec.knockback() * 0.15, push.z);
                }
                if (spec.radiationTicks() > 0 && target instanceof LivingEntity livingRad) {
                    livingRad.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.techguns.techguns3.registry.TGEffects.RADIATION,
                            spec.radiationTicks(), 1));
                }
                if (spec.hasExplosive()) {
                    com.techguns.techguns3.combat.ExplosiveCombat.explode(serverLevel,
                            getOwner() instanceof LivingEntity livingOwner ? livingOwner : null,
                            impact, spec);
                    discard();
                    return;
                }
                if (target instanceof LivingEntity livingVictim && !livingVictim.isAlive()) {
                    // Lethal Techguns hit: dense blood explosion, chunks on the gib roll.
                    com.techguns.techguns3.damagesystem.GoreHandler.onKill(serverLevel, livingVictim,
                            getDeltaMovement().normalize(),
                            com.techguns.techguns3.damagesystem.GoreHandler.BULLET_GORE_CHANCE,
                            bio ? com.techguns.techguns3.damagesystem.GoreRegistry.ICHOR_GREEN
                                    : com.techguns.techguns3.damagesystem.GoreHandler.USE_VICTIM_COLOR);
                }
                if (spec.poisonDuration() > 0 && target instanceof LivingEntity living) {
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.POISON,
                            spec.poisonDuration(), spec.poisonAmp()));
                }
                if (spec.fireTicks() > 0) {
                    target.igniteForSeconds(spec.fireTicks() / 20.0f);
                }
                // Compact impact: small baked star + sparks + outward ring.
                // Never head-sized; the ring sells power, not quad size.
                level().addFreshEntity(new MuzzleFlashEntity(level(),
                        impact.x, impact.y, impact.z, MuzzleFlashEntity.WHITE, 0.20f));
                com.techguns.techguns3.fx.FxParticles.impactSparks(serverLevel, impact, 5);
                com.techguns.techguns3.fx.FxParticles.impactRing(serverLevel, impact,
                        getDeltaMovement().normalize().scale(-1), 0xFFE9C8, 6);
                if (target instanceof LivingEntity livingVictim) {
                    // Flesh hit, not stone: directional blood along the shot + wet thud.
                    // Bio rounds bleed acid ichor instead of arterial red.
                    int blood = bio ? com.techguns.techguns3.damagesystem.GoreRegistry.ICHOR_GREEN
                            : com.techguns.techguns3.damagesystem.GoreRegistry.bloodColor(livingVictim);
                    com.techguns.techguns3.fx.BloodFx.hitBurst(serverLevel, impact,
                            getDeltaMovement().normalize(), dmg, blood);
                    level().playSound(null, impact.x, impact.y, impact.z,
                            com.techguns.techguns3.registry.TGSounds.FLESH_IMPACT.get(),
                            net.minecraft.sounds.SoundSource.HOSTILE, 0.9f,
                            0.85f + serverLevel.getRandom().nextFloat() * 0.3f);
                }
                if (spec.fireTicks() > 0) {
                    com.techguns.techguns3.fx.FxParticles.flameBurst(serverLevel, impact, 5);
                }
                if (spec.slimeAmount() > 0.0f) {
                    com.techguns.techguns3.fx.FxParticles.slimeBurst(serverLevel, impact, 6);
                }
            }
            discard();
        } else if (hit instanceof BlockHitResult blockHit) {
            if (!level().isClientSide()) {
                // Position-sensitive variant (Neo extension): respects blocks that
                // change their sound per position/entity instead of the static type.
                SoundType sound = level().getBlockState(blockHit.getBlockPos())
                        .getSoundType(level(), blockHit.getBlockPos(), this);
                SoundEvent impact = impactFor(sound).get();
                level().playSound(null, blockHit.getLocation().x, blockHit.getLocation().y,
                        blockHit.getLocation().z, impact, SoundSource.BLOCKS, 1.0f, 1.0f);
                Vec3 at = blockHit.getLocation();
                var face = blockHit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                level().addFreshEntity(new MuzzleFlashEntity(level(),
                        at.x, at.y, at.z, MuzzleFlashEntity.WHITE, 0.17f));
                com.techguns.techguns3.fx.FxParticles.impactSparks(
                        (ServerLevel) level(), at, 4);
                com.techguns.techguns3.fx.FxParticles.impactRing(
                        (ServerLevel) level(), at, normal, 0xFFE9C8, 5);
                com.techguns.techguns3.fx.FxParticles.smoke(
                        (ServerLevel) level(), at, 1);
                if (spec.fireTicks() > 0) {
                    // The stream sets the world alight where it lands, like the
                    // original flamethrower: bloom of flame + a real fire block.
                    com.techguns.techguns3.fx.FxParticles.flameBurst(
                            (ServerLevel) level(), at, 9);
                    igniteAt((ServerLevel) level(), at, face);
                }
                if (spec.slimeAmount() > 0.0f) {
                    com.techguns.techguns3.entity.SlimeBlobEntity.growAt(
                            (ServerLevel) level(), blockHit.getBlockPos(),
                            blockHit.getDirection(), spec.slimeAmount());
                }
                if (spec.hasExplosive()) {
                    com.techguns.techguns3.combat.ExplosiveCombat.explode((ServerLevel) level(),
                            getOwner() instanceof LivingEntity livingOwner ? livingOwner : null,
                            at, spec);
                }
            }
            discard();
        }
    }

    /**
     * Places a real fire block where the flamethrower stream lands (player
     * weapon, like flint and steel: no mob-griefing check). Tries the impact
     * cell first, then the cell above, and only onto a sturdy top face.
     */
    private static void igniteAt(ServerLevel level, Vec3 at, net.minecraft.core.Direction face) {
        try {
            net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(at.x, at.y, at.z);
            net.minecraft.core.BlockPos[] candidates = {
                    base, base.relative(face), base.above()
            };
            for (net.minecraft.core.BlockPos pos : candidates) {
                if (!level.getBlockState(pos).isAir()) continue;
                net.minecraft.core.BlockPos below = pos.below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) continue;
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                return;
            }
        } catch (Exception ignored) {
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
        builder.define(DATA_COLOR, 0xFFD9A8);
        builder.define(DATA_BULK, 1.0f);
        builder.define(DATA_MAIN, false);
    }

    public int tracerColor() {
        return entityData.get(DATA_COLOR);
    }

    /** Body volume multiplier (1 = rifle round, >1 = fat pellet/blob). Synced. */
    public float bulk() {
        return Math.max(0.5f, Math.min(2.5f, entityData.get(DATA_BULK)));
    }

    /** Sonic main blade (draws CE wave rings). Synced. */
    public boolean sonicMain() {
        return entityData.get(DATA_MAIN);
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
                input.getFloatOr("Penetration", 0.0f),
                input.getFloatOr("WeaveAmp", 0.0f),
                input.getFloatOr("WeaveFreq", 0.0f),
                input.getIntOr("HomingId", -1),
                input.getFloatOr("HomingTurn", 0.2f),
                input.getFloatOr("Slime", 0.0f),
                input.getIntOr("PoisonTicks", 0),
                input.getIntOr("PoisonAmp", 0),
                input.getIntOr("FireTicks", 0),
                input.getIntOr("TracerColor", 0xFFD9A8),
                input.getFloatOr("Bulk", 1.0f),
                input.getFloatOr("ExpMax", 0.0f),
                input.getFloatOr("ExpMin", 0.0f),
                input.getFloatOr("ExpR1", 0.0f),
                input.getFloatOr("ExpR2", 0.0f),
                input.getFloatOr("ExpBlock", 0.0f),
                input.getBooleanOr("ExpNuke", false),
                input.getFloatOr("Knockback", 0.0f),
                input.getBooleanOr("NoIframes", false),
                input.getIntOr("Radiation", 0),
                input.getBooleanOr("SonicMain", false));
        ttlLeft = input.getIntOr("TTLLeft", spec.ttlTicks());
        age = input.getIntOr("Age", 0);
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
        output.putFloat("WeaveAmp", spec.weaveAmp());
        output.putFloat("WeaveFreq", spec.weaveFreq());
        output.putInt("HomingId", spec.homingTargetId());
        output.putFloat("HomingTurn", spec.homingTurn());
        output.putFloat("Slime", spec.slimeAmount());
        output.putInt("PoisonTicks", spec.poisonDuration());
        output.putInt("PoisonAmp", spec.poisonAmp());
        output.putInt("FireTicks", spec.fireTicks());
        output.putInt("TracerColor", spec.tracerColor());
        output.putFloat("Bulk", spec.bulk());
        output.putFloat("ExpMax", spec.explosiveMax());
        output.putFloat("ExpMin", spec.explosiveMin());
        output.putFloat("ExpR1", spec.explosiveR1());
        output.putFloat("ExpR2", spec.explosiveR2());
        output.putFloat("ExpBlock", spec.explosiveBlockDamage());
        output.putBoolean("ExpNuke", spec.explosiveNuke());
        output.putFloat("Knockback", spec.knockback());
        output.putBoolean("NoIframes", spec.ignoreIframes());
        output.putInt("Radiation", spec.radiationTicks());
        output.putBoolean("SonicMain", spec.sonicMain());
        output.putInt("TTLLeft", ttlLeft);
        output.putInt("Age", age);
        output.putDouble("StartX", startX);
        output.putDouble("StartY", startY);
        output.putDouble("StartZ", startZ);
        output.putBoolean("StartInit", startInit);
    }
}
