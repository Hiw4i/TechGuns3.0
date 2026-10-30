package com.techguns.techguns3.mob;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.ai.TGCombatMemory;
import com.techguns.techguns3.ai.TGDynamiteGoal;
import com.techguns.techguns3.ai.TGFleeGoal;
import com.techguns.techguns3.ai.TGGunCombatGoal;
import com.techguns.techguns3.ai.TGHearingGoal;
import com.techguns.techguns3.ai.TGBuildGoal;
import com.techguns.techguns3.ai.TGMineGoal;
import com.techguns.techguns3.ai.TGPickupWeaponGoal;
import com.techguns.techguns3.combat.BallisticCombat;
import com.techguns.techguns3.combat.GunServerLogic;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.item.GunStats;
import com.techguns.techguns3.registry.TGDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Profile-driven armed NPC: the entity half of the Techguns mob system.
 *
 * <p>Every stat, weapon and ability comes from the datapack profile
 * ({@code data/techguns3/mobs/<id>.json} via {@link MobRegistry}); this class
 * only owns the equipment/resource state and the shared gun helpers the
 * {@link com.techguns.techguns3.ai} goals call. All goals are registered
 * unconditionally — each {@code canUse()} re-checks the live profile, so a
 * {@code /reload} that flips an ability applies to already-spawned mobs.</p>
 *
 * <p>Legacy hook: subclasses may provide {@link #defaultGun()} as the weapon
 * used when the profile is missing or lists no weapons.</p>
 */
public class TGArmedMob extends Monster {
    /** Equipment/resource caps (CE NPC behaviour: scrounging has a ceiling). */
    public static final int MAX_SPARE_MAGAZINES = 64;
    public static final int MAX_BUILD_MATERIALS = 256;
    /** Equipment drop chance for NPCs that do not keep their guns (vanilla default). */
    private static final float NPC_GUN_DROP_CHANCE = 0.085f;
    /** Weapon behaviour defaults for profiles missing the combat block. */
    private static final float DEFAULT_ACCURACY = 0.4f;
    private static final double DEFAULT_DAMAGE_MULTIPLIER = 0.5;
    private static final int DEFAULT_REACTION_TICKS = 10;
    private static final int MIN_REACTION_TICKS = 2;
    /** Aim error at accuracy 0 (degrees); sharpshooter accuracy 1 has none. */
    private static final float MAX_AIM_JITTER_DEG = 6.0f;
    private static final float SPREAD_WILD = 2.5f;
    private static final float SPREAD_TIGHT = 0.4f;
    /** Velocity lead is deliberately under full so dodging stays viable. */
    private static final double LEAD_FACTOR = 0.85;
    private static final float MIN_LEAD_SPEED = 0.1f;
    /** Floors for the difficulty-scaled stats. */
    private static final float MIN_ACCURACY = 0.05f;
    private static final double MIN_DIFFICULTY_DIVISOR = 0.25;

    private final TGCombatMemory memory = new TGCombatMemory();

    /** Spare magazines; -1 = unlimited (the CE NPC behaviour). */
    private int spareMagazines = -1;
    /** Blocks the builder may place; consumed by TGBuildGoal. */
    private int buildMaterials;
    /** TNT charges left; consumed by TGDynamiteGoal. */
    private int dynamiteLeft;
    /** True once finalizeSpawn (or NBT load) applied a profile's attributes. */
    private boolean profileApplied;

    public TGArmedMob(EntityType<? extends TGArmedMob> type, Level level) {
        super(type, level);
    }

    /** Fallback weapon when the live profile has none (legacy subclasses). */
    @Nullable
    protected GenericGunItem defaultGun() {
        return null;
    }

    @Nullable
    public MobProfile profile() {
        return MobRegistry.getFor(getType());
    }

    public TGCombatMemory memory() {
        return memory;
    }

    // --- goals -------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TGFleeGoal(this));
        goalSelector.addGoal(2, new TGGunCombatGoal(this));
        goalSelector.addGoal(3, new TGDynamiteGoal(this));
        goalSelector.addGoal(4, new TGPickupWeaponGoal(this));
        goalSelector.addGoal(5, new TGMineGoal(this));
        goalSelector.addGoal(6, new TGBuildGoal(this));
        // Melee is the weaponless fallback: it must sit BELOW the siege goals,
        // otherwise it locks MOVE against a wall and starves digging/building.
        goalSelector.addGoal(7, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(8, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new TGHearingGoal(this));
    }

    // --- spawn -------------------------------------------------------------

    // Override-only API by design (vanilla mobs do the same); the deprecation
    // targets external callers, which must go through EventHooks#finalizeMobSpawn.
    @SuppressWarnings("deprecation")
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        MobProfile profile = profile();
        if (profile != null) {
            applyProfile(profile);
        } else {
            equipFallbackWeapon();
        }
        return result;
    }

    /** Apply profile attributes + spawn equipment. Safe to call on /reload-agnostic spawn only. */
    public void applyProfile(MobProfile profile) {
        MobProfile.Stats stats = profile.stats();
        setBaseAttribute(Attributes.MAX_HEALTH, stats.maxHealth());
        setBaseAttribute(Attributes.MOVEMENT_SPEED, stats.movementSpeed());
        setBaseAttribute(Attributes.ATTACK_DAMAGE, stats.attackDamage());
        setBaseAttribute(Attributes.FOLLOW_RANGE, stats.followRange());
        setBaseAttribute(Attributes.ARMOR, stats.armor());
        setBaseAttribute(Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResistance());
        setBaseAttribute(Attributes.SCALE, stats.scale());
        setBaseAttribute(Attributes.STEP_HEIGHT, stats.stepHeight());
        setHealth(getMaxHealth());
        profileApplied = true;

        Identifier weaponId = profile.armed() ? profile.pickWeapon(random) : null;
        GenericGunItem gun = resolveGun(weaponId);
        if (gun != null) {
            equipGun(gun);
        } else {
            equipFallbackWeapon();
        }
        spareMagazines = profile.combat().ammoMagazines();
        buildMaterials = profile.abilities().build() ? profile.abilities().buildBlocks() : 0;
        dynamiteLeft = profile.abilities().dynamite();
        // The drop chance itself is decided in equipGun (the one place that
        // knows whether a gun is actually carried).
    }

    @Nullable
    private GenericGunItem resolveGun(@Nullable Identifier weaponId) {
        if (weaponId != null) {
            Item item = BuiltInRegistries.ITEM.getValue(weaponId);
            if (item instanceof GenericGunItem gun) return gun;
        }
        return null;
    }

    /** Full magazine into main hand. */
    public void equipGun(GenericGunItem gun) {
        setItemSlot(EquipmentSlot.MAINHAND, loadedGunStack(gun, gun.def().magazine()));
        setDropChance(EquipmentSlot.MAINHAND, TGConfig.mobsKeepGunsOnDeath() ? 1.0f : NPC_GUN_DROP_CHANCE);
    }

    /** A fresh gun stack loaded with {@code rounds} (spawn/equip path only). */
    private static ItemStack loadedGunStack(GenericGunItem gun, int rounds) {
        ItemStack stack = new ItemStack(gun);
        stack.set(TGDataComponents.AMMO.get(), rounds);
        return stack;
    }

    private void equipFallbackWeapon() {
        GenericGunItem gun = defaultGun();
        if (gun != null) {
            equipGun(gun);
            spareMagazines = -1;
        }
    }

    private void setBaseAttribute(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** @return whether attributes were ever applied (self-test assert). */
    public boolean profileApplied() {
        return profileApplied;
    }

    // --- weapon helpers ----------------------------------------------------

    @Nullable
    public GenericGunItem heldGun() {
        return getMainHandItem().getItem() instanceof GenericGunItem gun ? gun : null;
    }

    public int loadedRounds() {
        GenericGunItem gun = heldGun();
        if (gun == null) return 0;
        return gun.loadedRounds(getMainHandItem());
    }

    public boolean isReloading() {
        return memory.reloadTicks > 0;
    }

    /** Magazine empty and no spare magazines left. */
    public boolean weaponDry() {
        if (isReloading()) return false;
        if (spareMagazines == -1) return false;
        return loadedRounds() == 0 && spareMagazines == 0;
    }

    /** Start a reload if a magazine is available; plays the gun's reload sound. */
    public boolean tryStartReload() {
        if (isReloading() || loadedRounds() > 0) return false;
        if (spareMagazines == 0) return false;
        GenericGunItem gun = heldGun();
        if (gun == null) return false;
        GunStats stats = gun.stats();
        memory.reloadTicks = stats.reloadTimeTicks();
        SoundEvent reloadSound = stats.reloadSound().get();
        if (reloadSound != null && level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    reloadSound, getSoundSource(), 1.0f, 1.0f);
        }
        return true;
    }

    /** Called from aiStep when memory.reloadTicks hits zero. */
    private void finishReload() {
        GenericGunItem gun = heldGun();
        if (gun == null) return;
        ItemStack stack = getMainHandItem();
        stack.set(TGDataComponents.AMMO.get(), gun.def().magazine());
        if (spareMagazines > 0) spareMagazines--;
        memory.burstShots = 0;
    }

    /** One shot at the target with lead prediction, accuracy spread and difficulty scaling. */
    public void fireGunAt(LivingEntity target) {
        GenericGunItem gun = heldGun();
        if (!(level() instanceof ServerLevel serverLevel) || gun == null) return;
        GunStats stats = gun.stats();
        MobProfile profile = profile();

        Vec3 muzzle = GunServerLogic.muzzlePos(this);
        Vec3 aim = leadPoint(target, muzzle, stats.bulletSpeed());
        Vec3 direction = aim.subtract(muzzle);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(direction.y, horizontal));

        float accuracy = effectiveAccuracy(profile);
        // Angular aim error: full jitter at accuracy 0, sharpshooter at 1.
        float jitterDeg = (1.0f - accuracy) * MAX_AIM_JITTER_DEG;
        yaw += (random.nextFloat() - 0.5f) * 2.0f * jitterDeg;
        pitch += (random.nextFloat() - 0.5f) * 2.0f * jitterDeg;
        // Projectile spread multiplier: wild hip fire vs tight aimed shots.
        float spreadMult = Mth.lerp(accuracy, SPREAD_WILD, SPREAD_TIGHT);

        double damageMult = profile != null ? profile.combat().damageMultiplier() : DEFAULT_DAMAGE_MULTIPLIER;
        GunStats scaled = stats.withDamage((float) (stats.baseDamage() * damageMult));
        BallisticCombat.fire(serverLevel, this, scaled, gun.extras(), null,
                0.0f, 1.0f, -1, muzzle, yaw, pitch, getSoundSource(), spreadMult);

        ItemStack stack = getMainHandItem();
        stack.set(TGDataComponents.AMMO.get(), Math.max(0, loadedRounds() - 1));
    }

    /** Point ahead of a moving target: distance / bullet speed ticks of velocity. */
    private Vec3 leadPoint(LivingEntity target, Vec3 muzzle, float bulletSpeed) {
        Vec3 targetPos = target.getEyePosition();
        if (bulletSpeed <= MIN_LEAD_SPEED) return targetPos;
        double ticksToHit = targetPos.distanceTo(muzzle) / bulletSpeed;
        // Lead slightly under full: feels smart, keeps dodging viable.
        return targetPos.add(target.getDeltaMovement().scale(ticksToHit * LEAD_FACTOR));
    }

    // --- targeting -----------------------------------------------------------

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Master switch: with AI off the mob stays a passive walker.
        if (target != null && !TGConfig.mobsAiEnabled()) return;
        LivingEntity previous = getTarget();
        if (target != null && target != previous) {
            // Fresh contact: reset the engagement so reaction delay re-arms.
            TGCombatMemory memory = memory();
            memory.reactionTicks = reactionTicks(profile());
            memory.aimTicks = 0;
            memory.burstShots = 0;
            memory.noLosTicks = 0;
        }
        super.setTarget(target);
    }

    /** Profile accuracy clamped through the global difficulty slider. */
    public float effectiveAccuracy(@Nullable MobProfile profile) {
        double base = profile != null ? profile.combat().accuracy() : DEFAULT_ACCURACY;
        return (float) Mth.clamp(base * TGConfig.mobsDifficulty(), MIN_ACCURACY, 1.0);
    }

    /** Reaction delay in ticks, scaled by difficulty (higher difficulty = faster). */
    public int reactionTicks(@Nullable MobProfile profile) {
        int base = profile != null ? profile.combat().reactionTicks() : DEFAULT_REACTION_TICKS;
        // The divisor floor keeps a mis-set config from zeroing the delay.
        return Math.max(MIN_REACTION_TICKS,
                (int) Math.round(base / Math.max(MIN_DIFFICULTY_DIVISOR, TGConfig.mobsDifficulty())));
    }

    // --- resources ---------------------------------------------------------

    public int spareMagazines() { return spareMagazines; }
    public void addSpareMagazines(int count) {
        if (spareMagazines != -1) spareMagazines = Math.min(MAX_SPARE_MAGAZINES, spareMagazines + count);
    }

    public int buildMaterials() { return buildMaterials; }
    public void addBuildMaterials(int count) { buildMaterials = Math.min(MAX_BUILD_MATERIALS, buildMaterials + count); }
    public void consumeBuildMaterial() { if (buildMaterials > 0) buildMaterials--; }

    public int dynamiteLeft() { return dynamiteLeft; }
    public void consumeDynamite() { if (dynamiteLeft > 0) dynamiteLeft--; }

    // --- tick ---------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (!TGConfig.mobsAiEnabled()) return;

        // Reload finishes the tick the timer hits zero (memory ticks below).
        if (memory.reloadTicks == 1) {
            finishReload();
        }
        LivingEntity target = getTarget();
        boolean hasLos = target != null && hasLineOfSight(target);
        memory.onTickWithTarget(target, hasLos);
    }

    // --- persistence ---------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TechgunsSpareMags", spareMagazines);
        output.putInt("TechgunsBuildMaterials", buildMaterials);
        output.putInt("TechgunsDynamite", dynamiteLeft);
        output.putBoolean("TechgunsProfileApplied", profileApplied);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        spareMagazines = input.getIntOr("TechgunsSpareMags", -1);
        buildMaterials = input.getIntOr("TechgunsBuildMaterials", 0);
        dynamiteLeft = input.getIntOr("TechgunsDynamite", 0);
        profileApplied = input.getBooleanOr("TechgunsProfileApplied", false);
    }
}
