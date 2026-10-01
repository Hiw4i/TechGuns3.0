package com.techguns.techguns3.fx;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.registry.TGParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side particle spawner with per-call curves. Counts scale with the
 * {@code fxDensity} common config; visuals stay data-driven, no new classes
 * per effect. All sizes are compact on purpose: muzzle smoke grows to ~0.45m
 * max so it can never read as a head-sized blob.
 */
public final class FxParticles {
    private FxParticles() {}

    public static void glow(ServerLevel level, double x, double y, double z, int count,
                            int color, int endColor, float size0, float size1, int life,
                            float gravity, float drag, double spread, double speed) {
        int n = scaled(count);
        if (n <= 0) return;
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double vx = (random.nextDouble() * 2 - 1) * spread * speed;
            double vy = (random.nextDouble() * 2 - 1) * spread * speed;
            double vz = (random.nextDouble() * 2 - 1) * spread * speed;
            level.sendParticles(new TGParticles.GlowOptions(color, endColor, size0, size1,
                            life, gravity, drag),
                    x, y, z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    public static void puff(ServerLevel level, double x, double y, double z, int count,
                            int color, int endColor, float size0, float size1, int life,
                            float gravity, float drag, double spread, double speed) {
        int n = scaled(count);
        if (n <= 0) return;
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double vx = (random.nextDouble() * 2 - 1) * spread * speed;
            double vy = random.nextDouble() * spread * speed * 0.7;
            double vz = (random.nextDouble() * 2 - 1) * spread * speed;
            level.sendParticles(new TGParticles.PuffOptions(color, endColor, size0, size1,
                            life, gravity, drag),
                    x, y, z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    /** Directional puff pushed along {@code push} (muzzle smoke drifts forward, not into the face). */
    public static void puffPushed(ServerLevel level, Vec3 at, Vec3 push, int count,
                                  int color, int endColor, float size0, float size1, int life,
                                  float gravity, float drag, double spread, double speed) {
        int n = scaled(count);
        if (n <= 0) return;
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double vx = push.x + (random.nextDouble() * 2 - 1) * spread * speed;
            double vy = push.y + random.nextDouble() * spread * speed * 0.7;
            double vz = push.z + (random.nextDouble() * 2 - 1) * spread * speed;
            level.sendParticles(new TGParticles.PuffOptions(color, endColor, size0, size1,
                            life, gravity, drag),
                    at.x, at.y, at.z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    public static void flame(ServerLevel level, double x, double y, double z, int count,
                             int color, int endColor, float size0, float size1, int life,
                             float gravity, float drag, double spread, double speed) {
        int n = scaled(count);
        if (n <= 0) return;
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double vx = (random.nextDouble() * 2 - 1) * spread * speed;
            double vy = (random.nextDouble() * 2 - 1) * spread * speed;
            double vz = (random.nextDouble() * 2 - 1) * spread * speed;
            level.sendParticles(new TGParticles.FlameOptions(color, endColor, size0, size1,
                            life, gravity, drag),
                    x, y, z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    /** Fire pushed along {@code push}: flamethrower jet tongues and rising impact flames. */
    public static void flamePushed(ServerLevel level, Vec3 at, Vec3 push, int count,
                                   int color, int endColor, float size0, float size1, int life,
                                   float gravity, float drag, double spread, double speed) {
        int n = scaled(count);
        if (n <= 0) return;
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double vx = push.x + (random.nextDouble() * 2 - 1) * spread * speed;
            double vy = push.y + (random.nextDouble() * 2 - 1) * spread * speed;
            double vz = push.z + (random.nextDouble() * 2 - 1) * spread * speed;
            level.sendParticles(new TGParticles.FlameOptions(color, endColor, size0, size1,
                            life, gravity, drag),
                    at.x, at.y, at.z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    private static int scaled(int count) {
        return Math.max(count > 0 ? 1 : 0, (int) Math.round(count * TGConfig.fxDensity()));
    }

    // --- presets ---

    /** White-hot impact sparks. */
    public static void impactSparks(ServerLevel level, Vec3 at, int count) {
        glow(level, at.x, at.y, at.z, count,
                0xFFF6D8, 0xFF9A2A, 0.10f, 0.02f, 14, 0.06f, 0.94f, 1.0, 1.6);
    }

    /** Expanding impact ring: sparks thrown outward in the hit plane (TG2 Shockwave feel, no new entity). */
    public static void impactRing(ServerLevel level, Vec3 at, Vec3 normal, int color, int count) {
        int n = scaled(count);
        if (n <= 0) return;
        Vec3 nrm = normal.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : normal.normalize();
        Vec3 up = Math.abs(nrm.y) > 0.94 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = nrm.cross(up).normalize();
        Vec3 w = nrm.cross(u).normalize();
        var random = level.getRandom();
        for (int i = 0; i < n; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double speed = 1.2 + random.nextDouble() * 1.6;
            double vx = (u.x * Math.cos(ang) + w.x * Math.sin(ang)) * speed;
            double vy = (u.y * Math.cos(ang) + w.y * Math.sin(ang)) * speed + 0.4;
            double vz = (u.z * Math.cos(ang) + w.z * Math.sin(ang)) * speed;
            level.sendParticles(new TGParticles.GlowOptions(color, color, 0.09f, 0.015f,
                            10, 0.05f, 0.92f),
                    at.x, at.y, at.z, 1, vx, vy, vz, 1.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }

    /** Baked muzzle flare: tiny hot core that pops and dies in ~6 ticks. */
    public static void muzzleFlare(ServerLevel level, Vec3 at, int color, float scale) {
        float s0 = 0.20f * scale + 0.06f;
        glow(level, at.x, at.y, at.z, 1,
                0xFFFFFF, color, s0, 0.03f, 6, 0.0f, 1.0f, 0.25, 0.4);
        glow(level, at.x, at.y, at.z, 2,
                color, 0xFF5A1A, 0.12f, 0.02f, 8, 0.02f, 0.96f, 0.8, 1.2);
    }

    /** Cyan tesla discharge sparks. */
    public static void teslaSparks(ServerLevel level, Vec3 at, int count) {
        glow(level, at.x, at.y, at.z, count,
                0xE8FBFF, 0x2AB6FF, 0.11f, 0.02f, 16, 0.02f, 0.93f, 1.0, 1.8);
    }

    /** Green slime burst. */
    public static void slimeBurst(ServerLevel level, Vec3 at, int count) {
        glow(level, at.x, at.y, at.z, count,
                0xC6FF5E, 0x2FA32F, 0.16f, 0.03f, 22, 0.10f, 0.94f, 1.0, 2.2);
    }

    /** Grey powder smoke, drifts forward/up and grows to 0.45m max. */
    public static void smoke(ServerLevel level, Vec3 at, int count) {
        puff(level, at.x, at.y, at.z, count,
                0x9A9A9A, 0x3A3A3A, 0.18f, 0.45f, 24, -0.012f, 0.97f, 0.4, 0.4);
    }

    /** Muzzle smoke pushed along the barrel so it never hangs in the shooter's head. */
    public static void muzzleSmoke(ServerLevel level, Vec3 at, Vec3 look, int count) {
        puffPushed(level, at, look.scale(0.7), count,
                0x9A9A9A, 0x3A3A3A, 0.16f, 0.42f, 22, -0.012f, 0.97f, 0.35, 0.35);
    }

    /** Hot ember sparks for explosions. */
    public static void embers(ServerLevel level, Vec3 at, int count) {
        glow(level, at.x, at.y, at.z, count,
                0xFFE9A8, 0xFF5A1A, 0.14f, 0.02f, 26, 0.12f, 0.95f, 1.0, 3.0);
    }

    /** Flamethrower jet tongues: dense soft fire blown along the barrel. */
    public static void flameJet(ServerLevel level, Vec3 at, Vec3 look, int count) {
        flamePushed(level, at, look.scale(2.4), count,
                0xFFE9A8, 0xFF5A1A, 0.36f, 0.10f, 16, -0.015f, 0.94f, 0.5, 0.9);
    }

    /** Fire blooming where the stream lands: flames rise, embers spit. */
    public static void flameBurst(ServerLevel level, Vec3 at, int count) {
        flame(level, at.x, at.y, at.z, count,
                0xFFF3C4, 0xFF5A1A, 0.32f, 0.08f, 18, -0.03f, 0.93f, 0.9, 1.2);
        glow(level, at.x, at.y, at.z, Math.max(1, count / 2),
                0xFFE9A8, 0xFF5A1A, 0.12f, 0.02f, 18, 0.08f, 0.95f, 1.0, 2.2);
    }
}
