package com.techguns.techguns3.combat;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.registry.TGSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TG2 explosion behavior (TGExplosion): distance-falloff entity damage in
 * r1..r2 plus an optional vanilla block explosion scaled by blockDamage.
 * Ported from RocketProjectile / TFGProjectile / Grenade40mmProjectile.
 */
public final class ExplosiveCombat {
    private ExplosiveCombat() {}

    public static void explode(ServerLevel level, LivingEntity owner, Vec3 at, BulletProjectile.Spec spec) {
        float max = (float) (spec.explosiveMax() * TGConfig.gunDamageMultiplier());
        float min = (float) (spec.explosiveMin() * TGConfig.gunDamageMultiplier());
        float r1 = spec.explosiveR1();
        float r2 = Math.max(r1 + 0.5f, spec.explosiveR2());
        boolean nuke = spec.explosiveNuke();

        // Entity damage with linear falloff r1(full) -> r2(min), like TGExplosion.
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new AABB(at, at).inflate(r2),
                e -> e.isAlive() && e.canBeHitByProjectile())) {
            if (entity == owner && at.distanceTo(entity.position()) < 1.0) continue;
            double dist = entity.position().add(0, entity.getBbHeight() * 0.5, 0).distanceTo(at);
            if (dist > r2) continue;
            float dmg = dist <= r1 ? max : min + (max - min) * (1.0f - (float) ((dist - r1) / (r2 - r1)));
            DamageSource source = owner != null
                    ? level.damageSources().mobProjectile(null, owner)
                    : level.damageSources().explosion(null, null);
            if (entity instanceof LivingEntity living) {
                entity.hurtServer(level, source, dmg);
                if (spec.knockback() > 0.0f) {
                    Vec3 push = entity.position().subtract(at).normalize()
                            .scale(spec.knockback() * 0.8);
                    living.push(push.x, push.y + 0.4 * spec.knockback() * 0.3, push.z);
                }
                if (spec.radiationTicks() > 0) {
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.techguns.techguns3.registry.TGEffects.RADIATION,
                            spec.radiationTicks(), nuke ? 2 : 1));
                }
                if (!living.isAlive()) {
                    com.techguns.techguns3.damagesystem.GoreHandler.onKill(level, living,
                            entity.position().subtract(at).normalize(),
                            1.0f, com.techguns.techguns3.damagesystem.GoreHandler.USE_VICTIM_COLOR);
                }
            } else {
                entity.hurtServer(level, source, dmg);
            }
        }

        // Presentation: CE blast billboard (blooms to r2) + flash + sparks + ring.
        boolean tfg = spec.tracerColor() == com.techguns.techguns3.fx.GunFxPresets.TRACER_TFG;
        int tint = nuke ? 0xFFFF9A : tfg ? 0x7CFF6E : 0xFFB43A;
        level.addFreshEntity(new com.techguns.techguns3.entity.BlastEntity(level,
                at.x, at.y + r2 * 0.25, at.z, r2, tint));
        level.addFreshEntity(new MuzzleFlashEntity(level, at.x, at.y, at.z,
                nuke ? 0xFFFF9A : MuzzleFlashEntity.WHITE, Math.min(2.5f, 0.6f + r2 * 0.12f)));
        FxParticles.impactRing(level, at, new Vec3(0, 1, 0), tint, (int) Math.min(24, 6 + r2 * 2));
        FxParticles.glow(level, at.x, at.y, at.z, (int) Math.min(30, 8 + r2 * 3),
                0xFFF6D8, nuke ? 0x7CFC00 : 0xFF5A1A, 0.16f, 0.02f, 22, 0.08f, 0.94f, 1.0, 3.0);
        FxParticles.smoke(level, at, (int) Math.min(16, 3 + r2));
        FxParticles.embers(level, at, (int) Math.min(12, 2 + r2));
        if (nuke) {
            FxParticles.flameBurst(level, at, 16);
        }

        // Sound: layered explosion + nuke/TFG variants.
        level.playSound(null, at.x, at.y, at.z, TGSounds.EXPLOSION.get(),
                SoundSource.BLOCKS, Math.min(4.0f, 1.0f + r2 * 0.15f), 0.9f);
        if (nuke) {
            level.playSound(null, at.x, at.y, at.z, TGSounds.NUKE_EXPLOSION.get(),
                    SoundSource.BLOCKS, 4.0f, 1.0f);
        } else if (spec.tracerColor() == com.techguns.techguns3.fx.GunFxPresets.TRACER_TFG) {
            level.playSound(null, at.x, at.y, at.z, TGSounds.TFG_EXPLOSION.get(),
                    SoundSource.BLOCKS, 2.0f, 1.0f);
            if (r2 > 6.0f) {
                level.playSound(null, at.x, at.y, at.z, TGSounds.TFG_EXPLOSION_ECHO.get(),
                        SoundSource.BLOCKS, 1.5f, 1.0f);
            }
        }

        // Blocks: vanilla explosion with yield scaled by CE blockdamage (0 = no grief).
        if (spec.explosiveBlockDamage() > 0.0f && TGConfig.explosionsDamageBlocks()) {
            float yield = r2 * spec.explosiveBlockDamage() * (nuke ? 1.0f : 0.6f);
            if (yield >= 1.0f) {
                level.explode(null, at.x, at.y, at.z, Math.min(nuke ? 12.0f : 8.0f, yield), true,
                        Level.ExplosionInteraction.MOB);
            }
        }
    }
}
