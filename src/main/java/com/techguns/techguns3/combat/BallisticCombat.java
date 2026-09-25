package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.item.GunStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-owned ballistic attack shared by players, NPCs and mounted guns. */
public final class BallisticCombat {
    private BallisticCombat() {}

    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch, SoundSource source) {
        fire(level, shooter, stats, muzzle, yaw, pitch, source, 1.0f);
    }

    /** Entry point with an aim-state spread multiplier (zoomed shots group tighter). */
    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats,
                            Vec3 muzzle, float yaw, float pitch, SoundSource source, float spreadMult) {
        if (stats.projectileKind() == GunStats.ProjectileKind.ELECTRIC) {
            ElectricCombat.fire(level, shooter, stats, muzzle, yaw, pitch);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    stats.fireSound().get(), source, 1.0f, 1.0f);
            return;
        }
        BulletProjectile.Spec spec = new BulletProjectile.Spec(
                (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier()),
                stats.bulletSpeed(), stats.maxRange(),
                stats.dropStart(), stats.dropEnd(), stats.dropMin(),
                stats.gravity(), stats.penetration());
        for (int i = 0; i < stats.pellets(); i++) {
            float spread = (i == 0 ? stats.spread() : stats.pelletSpread()) * spreadMult;
            level.addFreshEntity(new BulletProjectile(level, shooter, muzzle, yaw, pitch, spread, spec));
        }
        boolean electric = stats.projectileKind() == GunStats.ProjectileKind.ELECTRIC;
        level.addFreshEntity(new MuzzleFlashEntity(level, muzzle.x, muzzle.y, muzzle.z,
                electric ? MuzzleFlashEntity.CYAN : MuzzleFlashEntity.WARM,
                electric ? 0.55f : 0.4f + 0.12f * stats.muzzleFlashScale()));
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                stats.fireSound().get(), source, 1.0f, 1.0f);
        if (stats.extraSound() != null) {
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    stats.extraSound().get(), source, 1.0f, 1.0f);
        }
    }
}
