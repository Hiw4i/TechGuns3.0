package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.BulletProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TG2-style tracer: short white-hot head stretched along velocity plus a
 * compact fading ribbon. History is keyed by UUID (never reused like entity
 * ids), bounded in both points and meters, and reset on any teleport jump so
 * a recycled slot can never smear a giant band across the player.
 */
public final class BulletRenderer extends EntityRenderer<BulletProjectile, BulletRenderer.State> {
    private record Trail(Deque<float[]> points, long lastGameTime) {}

    /** Render-thread-only trail history by stable UUID. */
    private static final Map<UUID, Trail> TRAILS = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Trail> eldest) {
            return size() > 64;
        }
    };
    private static final int TRAIL_POINTS = 4;
    /** Ribbon never longer than this, no matter the bullet speed. */
    private static final float MAX_TRAIL_METERS = 3.0f;
    /** Jump bigger than this resets the trail (teleport / id reuse guard). */
    private static final float RESET_DIST_SQR = 64.0f;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public final Vector3f motion = new Vector3f(0, 0, -1);
        public float px;
        public float py;
        public float pz;
        public UUID uuid;
        public int color = 0xFFD9A8;
        public float bulk = 1.0f;
        public int age;
        public boolean main;
    }

    private static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, path);
    }

    /** CE projectile art (RenderTextureProjectile/RenderSonicShotgunProjectile 1:1). */
    private static final net.minecraft.resources.Identifier TEX_FIREBALL =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/fx/fireball.png");
    private static final net.minecraft.resources.Identifier TEX_BIOBLOB =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/entity/bioblob.png");
    private static final net.minecraft.resources.Identifier TEX_BULLET_BLUE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/entity/bullet_blue.png");
    private static final net.minecraft.resources.Identifier TEX_TFG =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/fx/tfg_flare.png");
    private static final net.minecraft.resources.Identifier TEX_ROCKET =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/item/ammo/rocket.png");
    private static final net.minecraft.resources.Identifier TEX_ROCKET_NUKE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/item/ammo/rocket_nuke.png");
    private static final net.minecraft.resources.Identifier TEX_ROCKET_HV =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/item/ammo/rocket_hv.png");
    private static final net.minecraft.resources.Identifier TEX_SHOCKWAVE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/fx/shockwave.png");

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletProjectile entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var motion = entity.getDeltaMovement();
        state.motion.set((float) motion.x, (float) motion.y, (float) motion.z);
        if (state.motion.lengthSquared() < 1e-6f) state.motion.set(0, 0, -1);
        var pos = entity.position();
        state.px = (float) pos.x;
        state.py = (float) pos.y;
        state.pz = (float) pos.z;
        state.uuid = entity.getUUID();
        state.color = entity.tracerColor();
        state.bulk = entity.bulk();
        state.age = entity.tickCount;
        state.main = entity.sonicMain();

        long gameTime = entity.level().getGameTime();
        Trail trail = TRAILS.get(state.uuid);
        if (trail == null) {
            Deque<float[]> points = new ArrayDeque<>();
            points.addLast(new float[]{state.px, state.py, state.pz});
            TRAILS.put(state.uuid, new Trail(points, gameTime));
        } else {
            float[] last = trail.points.peekLast();
            if (last != null) {
                float dx = state.px - last[0];
                float dy = state.py - last[1];
                float dz = state.pz - last[2];
                // Teleport / stale slot: drop history instead of drawing a
                // map-long streak through the shooter.
                if (dx * dx + dy * dy + dz * dz > RESET_DIST_SQR) {
                    trail.points.clear();
                }
            }
            trail.points.addLast(new float[]{state.px, state.py, state.pz});
            while (trail.points.size() > TRAIL_POINTS) trail.points.pollFirst();
            // Time-based eviction so discarded bullets never linger.
            if (TRAILS.size() > 48) {
                TRAILS.entrySet().removeIf(e -> gameTime - e.getValue().lastGameTime > 200);
            }
            TRAILS.put(state.uuid, new Trail(trail.points, gameTime));
        }
    }

    /** Called from entity removal paths to drop history eagerly. */
    public static void dropTrail(UUID uuid) {
        TRAILS.remove(uuid);
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        Vector3f dir = new Vector3f(state.motion).normalize();
        float speed = state.motion.length();

        Vector3f up = Math.abs(dir.y) > 0.94f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        Trail trail = state.uuid == null ? null : TRAILS.get(state.uuid);
        float[] trailPts = trail == null ? null : clampTrail(trail.points);

        float cr = ((state.color >> 16) & 0xFF) / 255.0f;
        float cg = ((state.color >> 8) & 0xFF) / 255.0f;
        float cb = (state.color & 0xFF) / 255.0f;
        float coreR = Math.min(1.0f, cr + 0.35f);
        float coreG = Math.min(1.0f, cg + 0.35f);
        float coreB = Math.min(1.0f, cb + 0.35f);

        boolean fire = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_FIRE;
        boolean bio = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_BIO;
        boolean adv = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_ADVANCED
                || state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_GAUSS;
        boolean plasma = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_TFG;
        boolean rocket = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_ROCKET;
        boolean sonic = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_SONIC;

        // Own-tracer guard: a bullet born at the barrel tip (~1m from the eyes)
        // must not draw its body inside the shooter's head.
        float closeness = cameraCloseness(state);
        float bodyScale = closeness < 1.2f ? 0.5f : 1.0f;
        float trailAlpha = closeness < 1.2f ? 0.0f : 1.0f;

        // CE textured bodies (billboard quads). Sonic non-main blades stay
        // invisible like the original; the main blade draws the wave rings.
        if (sonic) {
            if (state.main && trailAlpha > 0.0f) {
                submitSonicRings(state, poseStack, collector, dir, u, w);
            }
            submitTrail(state, poseStack, collector, trailPts, trailAlpha * 0.4f, u, w, cr, cg, cb);
            return;
        }
        net.minecraft.resources.Identifier bodyTex = null;
        float bodySize = 0.2f;
        float bodyU0 = 0, bodyV0 = 0, bodyU1 = 1, bodyV1 = 1;
        if (fire) {
            bodyTex = TEX_FIREBALL;
            bodySize = 0.34f;
        } else if (bio) {
            bodyTex = TEX_BIOBLOB;
            bodySize = 0.26f;
        } else if (adv) {
            bodyTex = TEX_BULLET_BLUE;
            bodySize = 0.20f;
        } else if (plasma) {
            bodyTex = TEX_TFG;
            int frame = Math.abs(state.age) % 16;
            bodyU0 = (frame % 4) / 4.0f;
            bodyV0 = (frame / 4) / 4.0f;
            bodyU1 = bodyU0 + 0.25f;
            bodyV1 = bodyV0 + 0.25f;
            bodySize = 0.62f + 0.08f * (float) Math.sin(state.age * 0.6);
        } else if (rocket) {
            // Nuke = max bulk flag, HV = fast flight (mirror of combat logic).
            bodyTex = state.bulk >= 2.45f ? TEX_ROCKET_NUKE : (speed > 1.5f ? TEX_ROCKET_HV : TEX_ROCKET);
            bodySize = 0.55f;
        }

        if (bodyTex != null) {
            final net.minecraft.resources.Identifier tex = bodyTex;
            final float bs = bodySize * bodyScale;
            final float fau0 = bodyU0, fav0 = bodyV0, fau1 = bodyU1, fav1 = bodyV1;
            final float[] axes = FxGeometry.cameraAxes();
            collector.submitCustomGeometry(poseStack, RenderTypes.eyes(tex), (pose, consumer) -> {
                var matrix = pose.pose();
                FxGeometry.cameraBillboard(matrix, consumer, 0, 0, 0,
                        axes[0], axes[1], axes[2], axes[3], axes[4], axes[5],
                        bs, fau0, fav0, fau1, fav1, 1, 1, 1, 1, FxGeometry.FULLBRIGHT);
            });
            submitTrail(state, poseStack, collector, trailPts, trailAlpha, u, w, cr, cg, cb);
            return;
        }

        // Plain ballistic body: volumetric box + hot heart (unchanged).
        // (Textured kinds returned above; only plain tracers reach here.)
        float bulk = Math.max(0.5f, Math.min(2.5f, state.bulk));
        float len = Math.min(0.95f, Math.max(0.4f, 0.35f + speed * 0.12f))
                / (float) Math.pow(bulk, 0.7);
        float hw = 0.055f * bulk;
        float outerAlpha = 0.9f;
        float blen = len * bodyScale;
        float bhw = hw * (0.6f + 0.4f * bodyScale);

        // Nose slightly ahead, body stretching back along the flight path.
        float nose = 0.06f * bodyScale;
        float fx = dir.x * nose, fy = dir.y * nose, fz = dir.z * nose;
        float tx = -dir.x * (blen - nose), ty = -dir.y * (blen - nose), tz = -dir.z * (blen - nose);
        // Inner hot core: 55% of the shell.
        float ch = 0.55f;
        float cfx = dir.x * nose * ch, cfy = dir.y * nose * ch, cfz = dir.z * nose * ch;
        float ctx = -dir.x * (blen - nose) * ch, cty = -dir.y * (blen - nose) * ch, ctz = -dir.z * (blen - nose) * ch;

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            if (trailPts != null && trailAlpha > 0.0f) {
                int n = trailPts.length / 3;
                for (int i = 0; i < n - 1; i++) {
                    float k = (i + 1) / (float) n;
                    float a = (0.06f + 0.22f * k) * trailAlpha;
                    float thw = 0.014f + 0.022f * k;
                    FxGeometry.ribbon(matrix, consumer,
                            trailPts[i * 3] - state.px, trailPts[i * 3 + 1] - state.py, trailPts[i * 3 + 2] - state.pz,
                            trailPts[i * 3 + 3] - state.px, trailPts[i * 3 + 4] - state.py, trailPts[i * 3 + 5] - state.pz,
                            u.x, u.y, u.z, thw, cr, cg, cb, a);
                    FxGeometry.ribbon(matrix, consumer,
                            trailPts[i * 3] - state.px, trailPts[i * 3 + 1] - state.py, trailPts[i * 3 + 2] - state.pz,
                            trailPts[i * 3 + 3] - state.px, trailPts[i * 3 + 4] - state.py, trailPts[i * 3 + 5] - state.pz,
                            w.x, w.y, w.z, thw, cr, cg, cb, a);
                }
            }
            // Tinted shell volume.
            FxGeometry.orientedBox(matrix, consumer, fx, fy, fz, tx, ty, tz,
                    u.x, u.y, u.z, bhw, w.x, w.y, w.z, bhw, cr, cg, cb, outerAlpha);
            // Near-white hot heart volume.
            FxGeometry.orientedBox(matrix, consumer, cfx, cfy, cfz, ctx, cty, ctz,
                    u.x, u.y, u.z, bhw * ch, w.x, w.y, w.z, bhw * ch,
                    coreR, coreG, coreB, 1.0f);
        });
    }

    /** Shared fading ribbon trail (lightning pipeline, untextured). */
    private static void submitTrail(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    SubmitNodeCollector collector, float[] trailPts, float alpha,
                                    Vector3f u, Vector3f w, float cr, float cg, float cb) {
        if (trailPts == null || alpha <= 0.0f) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            int n = trailPts.length / 3;
            for (int i = 0; i < n - 1; i++) {
                float k = (i + 1) / (float) n;
                float a = (0.06f + 0.22f * k) * alpha;
                float thw = 0.014f + 0.022f * k;
                FxGeometry.ribbon(matrix, consumer,
                        trailPts[i * 3] - state.px, trailPts[i * 3 + 1] - state.py, trailPts[i * 3 + 2] - state.pz,
                        trailPts[i * 3 + 3] - state.px, trailPts[i * 3 + 4] - state.py, trailPts[i * 3 + 5] - state.pz,
                        u.x, u.y, u.z, thw, cr, cg, cb, a);
                FxGeometry.ribbon(matrix, consumer,
                        trailPts[i * 3] - state.px, trailPts[i * 3 + 1] - state.py, trailPts[i * 3 + 2] - state.pz,
                        trailPts[i * 3 + 3] - state.px, trailPts[i * 3 + 4] - state.py, trailPts[i * 3 + 5] - state.pz,
                        w.x, w.y, w.z, thw, cr, cg, cb, a);
            }
        });
    }

    /**
     * CE sonic wave (RenderSonicShotgunProjectile 1:1): expanding shockwave
     * rings riding behind the main blade, additive, fading with age.
     */
    private static void submitSonicRings(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         SubmitNodeCollector collector,
                                         Vector3f dir, Vector3f u, Vector3f w) {
        float age = Math.max(0, state.age);
        if (age < 3) return;
        float prog = Math.min(1.0f, age / 20.0f);
        float opacity = 1.0f - prog * prog;
        if (opacity <= 0.02f) return;
        double d = age * 0.3;
        java.util.Random rand = new java.util.Random(0x50C1);
        final float fa = opacity;
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(TEX_SHOCKWAVE), (pose, consumer) -> {
            var matrix = pose.pose();
            for (int i = 0; i <= 5; i++) {
                float r = fa * rand.nextFloat();
                float g = fa * rand.nextFloat();
                float b = fa * (0.5f + rand.nextFloat() * 0.5f);
                double offset = 2.5 - i;
                float size = (float) (d * (0.5 + rand.nextDouble()));
                if (size < 0.05f) continue;
                float cx = (float) (-dir.x * offset);
                float cy = (float) (-dir.y * offset);
                float cz = (float) (-dir.z * offset);
                // Ring quad in the plane perpendicular to flight.
                FxGeometry.uvQuad(matrix, consumer,
                        cx - u.x * size - w.x * size, cy - u.y * size - w.y * size, cz - u.z * size - w.z * size,
                        cx + u.x * size - w.x * size, cy + u.y * size - w.y * size, cz + u.z * size - w.z * size,
                        cx + u.x * size + w.x * size, cy + u.y * size + w.y * size, cz + u.z * size + w.z * size,
                        cx - u.x * size + w.x * size, cy - u.y * size + w.y * size, cz - u.z * size + w.z * size,
                        0, 0, 1, 0, 1, 1, 0, 1, r, g, b, fa, FxGeometry.FULLBRIGHT);
            }
        });
    }

    /** Newest-first walk capped at MAX_TRAIL_METERS so speed can't stretch it. */
    private static float[] clampTrail(Deque<float[]> trail) {
        if (trail.size() < 2) return null;
        float[][] pts = trail.toArray(new float[0][]);
        // Walk from the head backwards while within budget, then re-order.
        java.util.ArrayList<float[]> kept = new java.util.ArrayList<>();
        kept.add(pts[pts.length - 1]);
        float acc = 0.0f;
        for (int i = pts.length - 1; i > 0; i--) {
            float dx = pts[i][0] - pts[i - 1][0];
            float dy = pts[i][1] - pts[i - 1][1];
            float dz = pts[i][2] - pts[i - 1][2];
            acc += Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (acc > MAX_TRAIL_METERS) break;
            kept.add(pts[i - 1]);
        }
        if (kept.size() < 2) return null;
        float[] out = new float[kept.size() * 3];
        for (int i = 0; i < kept.size(); i++) {
            float[] p = kept.get(kept.size() - 1 - i);
            out[i * 3] = p[0];
            out[i * 3 + 1] = p[1];
            out[i * 3 + 2] = p[2];
        }
        return out;
    }

    private static float cameraCloseness(State state) {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return 99.0f;
            var cam = mc.player.getEyePosition();
            double dx = cam.x - state.px;
            double dy = cam.y - state.py;
            double dz = cam.z - state.pz;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        } catch (Exception e) {
            return 99.0f;
        }
    }
}
