package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.BeamEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * CE hitscan beam (RenderLaserProjectile/RenderNDRProjectile 1:1):
 * additive textured beam with scrolling U, width pulsing with life progress,
 * plus a bright start cap. Laser uses {@code laser3.png}, NDR the 17-frame
 * animated {@code nukebeam.png}.
 */
public final class BeamRenderer extends EntityRenderer<BeamEntity, BeamRenderer.State> {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TechGuns3.MODID, path);
    }

    private static final Identifier TEX_LASER = id("textures/fx/laser3.png");
    private static final Identifier TEX_LASER_START = id("textures/fx/laser3_start.png");
    private static final Identifier TEX_NUKEBEAM = id("textures/fx/nukebeam.png");

    public BeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public final Vector3f end = new Vector3f();
        public int color;
        public float prog;
        public float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeamEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.end.set(entity.endOffset());
        state.color = entity.color();
        // Wall-clock pulse: sustained beams refresh life every tick, so a
        // life-based progress would freeze the shimmer. Time loops it forever.
        try {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            double t = (level != null ? level.getGameTime() : 0) + partialTick;
            state.prog = (float) ((t % 20.0) / 20.0);
            state.time = (float) t;
        } catch (Exception e) {
            state.prog = 0.5f;
            state.time = 0.0f;
        }
    }

    @Override
    protected boolean affectedByCulling(BeamEntity entity) {
        return false;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float len = state.end.length();
        if (len < 0.05f) return;
        boolean nuke = state.color == com.techguns.techguns3.fx.GunFxPresets.TRACER_NDR;
        float prog = Math.max(0.0f, Math.min(1.0f, state.prog));
        // CE width pulse: swell mid-life, vanish at both ends.
        float pulse = (float) Math.sin(Math.sqrt(Math.max(0.001f, prog)) * Math.PI);
        // CE world widths (laserWidth 3.0 in f10 space ≈ ±0.075): slim beam + hot core.
        float width = (nuke ? 0.085f : 0.07f) * (0.25f + 0.75f * pulse);
        float core = (nuke ? 0.038f : 0.03f) * (0.25f + 0.75f * pulse);
        float brightness = 0.35f + 0.65f * pulse;

        Vector3f dir = new Vector3f(state.end).normalize();
        Vector3f up = Math.abs(dir.y) > 0.94f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        Identifier bodyTex = nuke ? TEX_NUKEBEAM : TEX_LASER;
        // NDR: 17-frame vertical strip animation; laser: static frame.
        final float v0, v1;
        if (nuke) {
            int frame = Math.min(16, (int) (prog * 17.0f));
            v0 = frame / 17.0f;
            v1 = (frame + 1) / 17.0f;
        } else {
            v0 = 0.0f;
            v1 = 1.0f;
        }
        // Segmented quads (≤4m, UV 0..1 each): standalone textures clamp past
        // UV 1, so one long quad would smear edge streaks down the whole beam.
        int segs = Math.max(1, Math.min(25, (int) Math.ceil(len / 4.0f)));
        final float fw = width, fc = core, fb = brightness;
        final float fvx0 = v0, fvx1 = v1;
        final float dx = dir.x, dy = dir.y, dz = dir.z;
        final float ux = u.x, uy = u.y, uz = u.z, wx = w.x, wy = w.y, wz = w.z;
        final int nsegs = segs;
        final float flen = len;
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(bodyTex), (pose, consumer) -> {
            var matrix = pose.pose();
            for (int sgi = 0; sgi < nsegs; sgi++) {
                float s0 = flen * sgi / nsegs;
                float s1 = flen * (sgi + 1) / nsegs;
                float ax = dx * s0, ay = dy * s0, az = dz * s0;
                float bx = dx * s1, by = dy * s1, bz = dz * s1;
                beamQuad(matrix, consumer, ax, ay, az, bx, by, bz, ux, uy, uz, fw, 0, fvx0, 1, fvx1, 1, 1, 1, fb * 0.85f);
                beamQuad(matrix, consumer, ax, ay, az, bx, by, bz, wx, wy, wz, fw, 0, fvx0, 1, fvx1, 1, 1, 1, fb * 0.85f);
                beamQuad(matrix, consumer, ax, ay, az, bx, by, bz, ux, uy, uz, fc, 0, fvx0, 1, fvx1, 1, 1, 1, Math.min(1.0f, fb + 0.2f));
                beamQuad(matrix, consumer, ax, ay, az, bx, by, bz, wx, wy, wz, fc, 0, fvx0, 1, fvx1, 1, 1, 1, Math.min(1.0f, fb + 0.2f));
            }
        });
        // Bright start cap at the muzzle (laser3_start, NDR reuses beam head).
        Identifier capTex = nuke ? TEX_NUKEBEAM : TEX_LASER_START;
        final float capLen = Math.min(1.0f, len);
        final float cex = dir.x * capLen, cey = dir.y * capLen, cez = dir.z * capLen;
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(capTex), (pose, consumer) -> {
            var matrix = pose.pose();
            beamQuad(matrix, consumer, 0, 0, 0, cex, cey, cez, ux, uy, uz, fw * 1.1f, 0, 0, 1, 1, 1, 1, 1, fb);
            beamQuad(matrix, consumer, 0, 0, 0, cex, cey, cez, wx, wy, wz, fw * 1.1f, 0, 0, 1, 1, 1, 1, 1, fb);
        });
        if (nuke) {
            submitSpiral(state, poseStack, collector, dir, u, w, len, fb, v0, v1);
        }
    }

    /**
     * CE NDR spiral (RenderNDRProjectile 1:1, world units): one continuous
     * ribbon coiling around the beam — consecutive helix points joined into a
     * strip, radius blooming with sqrt distance, coil rotating with wall-clock
     * time. Reads as the original spiral, not dotted beads.
     */
    private static void submitSpiral(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     SubmitNodeCollector collector,
                                     Vector3f dir, Vector3f u, Vector3f w,
                                     float len, float brightness, float frameV0, float frameV1) {
        float spiralLen = Math.min(len, 30.0f);
        int segments = Math.max(8, Math.min(30, (int) (spiralLen / 1.0f)));
        double angle = Math.PI / 8.0;
        double angleOffset = state.time * 2.2;
        final double fAngle = angle, fOff = angleOffset;
        final int segs = segments;
        final float step = spiralLen / segments;
        final float fb = brightness * 0.9f;
        final float fv0 = frameV0, fv1 = frameV1;
        final float dx = dir.x, dy = dir.y, dz = dir.z;
        final float ux = u.x, uy = u.y, uz = u.z, wx = w.x, wy = w.y, wz = w.z;
        final float ribbonHalf = 0.045f;
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(TEX_NUKEBEAM), (pose, consumer) -> {
            var matrix = pose.pose();
            // Helix point i, then join prev->curr with a strip quad widened
            // along the beam axis (CE offsets ±w along local X = beam dir).
            double prevHx = 0, prevHy = 0, prevHz = 0, prevU = 0;
            for (int i = 0; i <= segs; i++) {
                float s = i * step;
                float iprog = Math.min(1.0f, (float) i / (float) segs);
                double radius = 0.5 * (1.0 - Math.cos(2.0 * Math.sqrt(Math.max(0.001f, iprog)) * Math.PI)) * 0.10;
                double a = fOff - fAngle * i;
                double hx = dx * s + ux * Math.cos(a) * radius + wx * Math.sin(a) * radius;
                double hy = dy * s + uy * Math.cos(a) * radius + wy * Math.sin(a) * radius;
                double hz = dz * s + uz * Math.cos(a) * radius + wz * Math.sin(a) * radius;
                double uu = (s / 8.0) % 1.0;
                if (i > 0) {
                    float u0 = (float) Math.max(0.0, Math.min(0.94, prevU));
                    float u1 = (float) Math.max(0.0, Math.min(1.0, uu));
                    FxGeometry.uvQuad(matrix, consumer,
                            (float) (prevHx - dx * ribbonHalf), (float) (prevHy - dy * ribbonHalf), (float) (prevHz - dz * ribbonHalf),
                            (float) (hx - dx * ribbonHalf), (float) (hy - dy * ribbonHalf), (float) (hz - dz * ribbonHalf),
                            (float) (hx + dx * ribbonHalf), (float) (hy + dy * ribbonHalf), (float) (hz + dz * ribbonHalf),
                            (float) (prevHx + dx * ribbonHalf), (float) (prevHy + dy * ribbonHalf), (float) (prevHz + dz * ribbonHalf),
                            u0, fv0, u1, fv0, u1, fv1, u0, fv1,
                            1, 1, 1, fb, FxGeometry.FULLBRIGHT);
                }
                prevHx = hx;
                prevHy = hy;
                prevHz = hz;
                prevU = uu;
            }
        });
    }

    private static void beamQuad(org.joml.Matrix4fc matrix,
                                 com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                 float fx, float fy, float fz, float tx, float ty, float tz,
                                 float sx, float sy, float sz, float halfWidth,
                                 float u0, float v0, float u1, float v1,
                                 float r, float g, float b, float a) {
        FxGeometry.uvQuad(matrix, consumer,
                fx + sx * halfWidth, fy + sy * halfWidth, fz + sz * halfWidth,
                fx - sx * halfWidth, fy - sy * halfWidth, fz - sz * halfWidth,
                tx - sx * halfWidth, ty - sy * halfWidth, tz - sz * halfWidth,
                tx + sx * halfWidth, ty + sy * halfWidth, tz + sz * halfWidth,
                u0, v0, u1, v0, u1, v1, u0, v1,
                r, g, b, a, FxGeometry.FULLBRIGHT);
    }
}
