package com.techguns.techguns3.entity;

import com.techguns.techguns3.combat.BallisticCombat;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** First shared humanoid gun AI: server-side targeting, ammunition and firing. */
public abstract class GunSoldierEntity extends Monster implements RangedAttackMob {
    private int loadedRounds;
    private int reloadTicks;

    protected GunSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    protected abstract GenericGunItem defaultGun();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 12, 32.0f));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(defaultGun()));
        loadedRounds = defaultGun().stats().magazineSize();
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && reloadTicks > 0) {
            reloadTicks--;
            if (reloadTicks == 0) loadedRounds = defaultGun().stats().magazineSize();
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(level() instanceof ServerLevel serverLevel) || reloadTicks > 0 || !hasLineOfSight(target)) {
            return;
        }
        if (loadedRounds <= 0) {
            reloadTicks = defaultGun().stats().reloadTimeTicks();
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    defaultGun().stats().reloadSound().get(), getSoundSource(), 1.0f, 1.0f);
            return;
        }
        loadedRounds--;
        Vec3 muzzle = getEyePosition().add(0, -0.1, 0);
        Vec3 targetPos = target.getEyePosition();
        Vec3 direction = targetPos.subtract(muzzle);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(direction.y, horizontal));
        BallisticCombat.fire(serverLevel, this, defaultGun().stats(), muzzle, yaw, pitch, getSoundSource());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TechgunsLoadedRounds", loadedRounds);
        output.putInt("TechgunsReloadTicks", reloadTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        loadedRounds = input.getIntOr("TechgunsLoadedRounds", defaultGun().stats().magazineSize());
        reloadTicks = input.getIntOr("TechgunsReloadTicks", 0);
    }
}
