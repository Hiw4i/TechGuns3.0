package com.techguns.techguns3.fx;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.damagesystem.GoreRegistry;
import com.techguns.techguns3.registry.TGParticles;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side blood: directional arterial spray on every flesh hit plus the
 * big radial fountain on a gibbed kill. Same vanilla packet pipeline as
 * {@link FxParticles} (no custom packets, multiplayer-consistent), but every
 * droplet carries its own velocity so the spray keeps the bullet's direction:
 * forward spatter along the shot, backmist at the wound, chunks on heavy hits.
 */
public final class BloodFx {
    private BloodFx() {}

    /**
     * Blood on hit. {@code dir} is the projectile travel direction, {@code damage}
     * the landed damage, {@code bloodColor} from
     * {@link GoreRegistry#bloodColor} (or the ichor override for bio rounds).
     */
    public static void hitBurst(ServerLevel level, Vec3 at, Vec3 dir, float damage, int bloodColor) {
        if (!TGConfig.goreEnabled() || !TGConfig.bloodOnHit()) return;
        if (bloodColor == GoreRegistry.NO_BLOOD) {
            FxParticles.impactSparks(level, at, 4);
            return;
        }
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0.3, 0) : dir.normalize();
        Vec3 up = Math.abs(d.y) > 0.94 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(up).normalize();
        Vec3 w = d.cross(u).normalize();
        RandomSource random = level.getRandom();

        int spray = scaled(Math.min(26, 7 + (int) (damage * 1.3f)));
        for (int i = 0; i < spray; i++) {
            // Forward cone along the shot: fast core, slower wide spatter.
            double along = 1.6 + random.nextDouble() * 3.2;
            double side = (random.nextDouble() * 2 - 1) * 0.9;
            double side2 = (random.nextDouble() * 2 - 1) * 0.9;
            Vec3 v = new Vec3(
                    d.x * along + u.x * side + w.x * side2,
                    d.y * along + u.y * side + w.y * side2 + random.nextDouble() * 1.2,
                    d.z * along + u.z * side + w.z * side2);
            int life = 16 + random.nextInt(14);
            float size = 0.10f + random.nextFloat() * 0.08f;
            level.sendParticles(new TGParticles.BloodOptions(bloodColor, GoreRegistry.BLOOD_DARK,
                            size, 0.03f, life, 0.30f, 0.96f),
                    at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Backmist hanging at the wound: dark, slow, grows like breath on air.
        int mist = scaled(4);
        for (int i = 0; i < mist; i++) {
            Vec3 v = new Vec3(
                    -d.x * 0.5 + (random.nextDouble() * 2 - 1) * 0.5,
                    0.4 + random.nextDouble() * 0.6,
                    -d.z * 0.5 + (random.nextDouble() * 2 - 1) * 0.5);
            level.sendParticles(new TGParticles.PuffOptions(darken(bloodColor), GoreRegistry.BLOOD_DARK,
                            0.22f, 0.5f, 22, -0.01f, 0.96f),
                    at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Meat: every flesh hit tears off something solid that arcs and falls.
        // Heavier rounds tear more; light rounds still leave one chunk behind.
        int chunks = Math.max(1, scaled(damage >= 14.0f ? 3 : damage >= 7.0f ? 2 : 1));
        for (int i = 0; i < chunks; i++) {
            Vec3 v = new Vec3(
                    d.x * (1.0 + random.nextDouble() * 1.6) + (random.nextDouble() * 2 - 1) * 0.8,
                    1.2 + random.nextDouble() * 1.6,
                    d.z * (1.0 + random.nextDouble() * 1.6) + (random.nextDouble() * 2 - 1) * 0.8);
            level.sendParticles(new TGParticles.PuffOptions(darken(bloodColor), GoreRegistry.BLOOD_DARK,
                            0.20f, 0.07f, 26, 0.35f, 0.97f),
                    at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    /**
     * Full dense fountain for a gibbed kill: a tall spray, a thick close core
     * that reads as solid meat-water, low ground speckle, hanging mist, a wet
     * ground ring and solid flesh bits. One call per kill, ~150 droplets max.
     */
    public static void deathBurst(ServerLevel level, Vec3 at, int bloodColor) {
        if (!TGConfig.goreEnabled()) return;
        if (bloodColor == GoreRegistry.NO_BLOOD) {
            FxParticles.impactSparks(level, at, 12);
            return;
        }
        RandomSource random = level.getRandom();
        // Tall fountain: every direction, high arc.
        int spray = scaled(70);
        for (int i = 0; i < spray; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double outward = 0.6 + random.nextDouble() * 2.8;
            Vec3 v = new Vec3(Math.cos(ang) * outward,
                    1.2 + random.nextDouble() * 3.4, Math.sin(ang) * outward);
            int life = 22 + random.nextInt(15);
            float size = 0.12f + random.nextFloat() * 0.10f;
            sendBlood(level, random, at, v, bloodColor, size, life);
        }
        // Dense close core: slow, thick, short-range — the "solid" middle of the burst.
        int core = scaled(34);
        for (int i = 0; i < core; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double outward = 0.2 + random.nextDouble() * 1.1;
            Vec3 v = new Vec3(Math.cos(ang) * outward,
                    0.6 + random.nextDouble() * 2.0, Math.sin(ang) * outward);
            int life = 18 + random.nextInt(11);
            float size = 0.14f + random.nextFloat() * 0.10f;
            sendBlood(level, random, at, v, bloodColor, size, life);
        }
        // Low ground speckle: flat fast droplets that arc down around the body.
        int speckle = scaled(26);
        for (int i = 0; i < speckle; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double outward = 1.2 + random.nextDouble() * 2.6;
            Vec3 v = new Vec3(Math.cos(ang) * outward,
                    0.3 + random.nextDouble() * 0.9, Math.sin(ang) * outward);
            int life = 20 + random.nextInt(12);
            sendBlood(level, new Vec3(at.x, at.y - 0.3, at.z), v, bloodColor,
                    0.10f + random.nextFloat() * 0.07f, life);
        }
        int mist = scaled(14);
        for (int i = 0; i < mist; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            Vec3 v = new Vec3(Math.cos(ang) * 0.7, 0.5 + random.nextDouble() * 0.8, Math.sin(ang) * 0.7);
            level.sendParticles(new TGParticles.PuffOptions(darken(bloodColor), GoreRegistry.BLOOD_DARK,
                            0.3f, 0.65f, 26, -0.01f, 0.96f),
                    at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        // Solid flesh bits, always: even a failed gib roll leaves meat behind.
        int bits = scaled(5);
        for (int i = 0; i < bits; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double outward = 0.5 + random.nextDouble() * 2.0;
            Vec3 v = new Vec3(Math.cos(ang) * outward,
                    1.4 + random.nextDouble() * 2.0, Math.sin(ang) * outward);
            level.sendParticles(new TGParticles.PuffOptions(darken(bloodColor), GoreRegistry.BLOOD_DARK,
                            0.20f, 0.07f, 28, 0.35f, 0.97f),
                    at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        // Low flat ring hugging the ground, like the TG2 shockwave but wet.
        FxParticles.impactRing(level, new Vec3(at.x, at.y - 0.4, at.z), new Vec3(0, 1, 0), bloodColor, 16);
    }

    private static void sendBlood(ServerLevel level, RandomSource random, Vec3 at, Vec3 v,
                                 int bloodColor, float size, int life) {
        level.sendParticles(new TGParticles.BloodOptions(bloodColor, GoreRegistry.BLOOD_DARK,
                        size, 0.03f, life, 0.32f, 0.96f),
                at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
    }

    private static void sendBlood(ServerLevel level, Vec3 at, Vec3 v,
                                 int bloodColor, float size, int life) {
        level.sendParticles(new TGParticles.BloodOptions(bloodColor, GoreRegistry.BLOOD_DARK,
                        size, 0.03f, life, 0.32f, 0.96f),
                at.x, at.y, at.z, 1, v.x, v.y, v.z, 1.0,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
    }

    private static int scaled(int count) {
        return Math.max(count > 0 ? 1 : 0, (int) Math.round(count * TGConfig.fxDensity()));
    }

    private static int darken(int color) {
        int r = (int) (((color >> 16) & 0xFF) * 0.55f);
        int g = (int) (((color >> 8) & 0xFF) * 0.55f);
        int b = (int) ((color & 0xFF) * 0.55f);
        return (r << 16) | (g << 8) | b;
    }
}
