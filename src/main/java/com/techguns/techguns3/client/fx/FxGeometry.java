package com.techguns.techguns3.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4fc;

/**
 * Quad emission for vanilla pipelines. {@link #quad} matches the lightning
 * pipeline (position + color only); {@link #uvQuad} matches textured ENTITY
 * pipelines like {@code eyes} (position + color + uv + overlay + light +
 * normal). Every quad is emitted double-sided so flashes read from any angle.
 */
public final class FxGeometry {
    private FxGeometry() {}

    /** Fullbright light coords for emissive FX (muzzle stars, flames). */
    public static final int FULLBRIGHT = 0xF000F0;

    public static void quad(Matrix4fc pose, VertexConsumer consumer,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float r, float g, float b, float a) {
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
    }

    /**
     * Axis-aligned ribbon quad from {@code from} to {@code to}, widened along
     * {@code side} by {@code halfWidth}. Emits one double-sided quad.
     */
    public static void ribbon(Matrix4fc pose, VertexConsumer consumer,
                             float fx, float fy, float fz,
                             float tx, float ty, float tz,
                             float sx, float sy, float sz, float halfWidth,
                             float r, float g, float b, float a) {        quad(pose, consumer,
                fx + sx * halfWidth, fy + sy * halfWidth, fz + sz * halfWidth,
                fx - sx * halfWidth, fy - sy * halfWidth, fz - sz * halfWidth,
                tx - sx * halfWidth, ty - sy * halfWidth, tz - sz * halfWidth,
                tx + sx * halfWidth, ty + sy * halfWidth, tz + sz * halfWidth,
                r, g, b, a);
    }

    /**
     * Polyline ({@code xyz xyz ...}) as crossed ribbon pairs, one quad per
     * segment per side. Used for lightning channels and branches.
     */
    public static void polyline(Matrix4fc pose, VertexConsumer consumer, float[] pts,
                               float ux, float uy, float uz,
                               float wx, float wy, float wz,
                               float halfWidth, float r, float g, float b, float a) {
        int n = pts.length / 3;
        for (int i = 0; i < n - 1; i++) {
            float fx = pts[i * 3];
            float fy = pts[i * 3 + 1];
            float fz = pts[i * 3 + 2];
            float tx = pts[i * 3 + 3];
            float ty = pts[i * 3 + 4];
            float tz = pts[i * 3 + 5];
            ribbon(pose, consumer, fx, fy, fz, tx, ty, tz, ux, uy, uz, halfWidth, r, g, b, a);
            ribbon(pose, consumer, fx, fy, fz, tx, ty, tz, wx, wy, wz, halfWidth, r, g, b, a);
        }
    }

    /**
     * Textured quad for ENTITY-format pipelines (eyes / entityTranslucent).
     * UVs map a sub-rect of the texture; light should be {@link #FULLBRIGHT}
     * for emissive FX. Double-sided.
     */
    public static void uvQuad(Matrix4fc pose, VertexConsumer consumer,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float u1, float v1, float u2, float v2,
                             float u3, float v3, float u4, float v4,
                             float r, float g, float b, float a, int light) {
        uvVertex(pose, consumer, x1, y1, z1, u1, v1, r, g, b, a, light);
        uvVertex(pose, consumer, x2, y2, z2, u2, v2, r, g, b, a, light);
        uvVertex(pose, consumer, x3, y3, z3, u3, v3, r, g, b, a, light);
        uvVertex(pose, consumer, x4, y4, z4, u4, v4, r, g, b, a, light);
        uvVertex(pose, consumer, x4, y4, z4, u4, v4, r, g, b, a, light);
        uvVertex(pose, consumer, x3, y3, z3, u3, v3, r, g, b, a, light);
        uvVertex(pose, consumer, x2, y2, z2, u2, v2, r, g, b, a, light);
        uvVertex(pose, consumer, x1, y1, z1, u1, v1, r, g, b, a, light);
    }

    private static void uvVertex(Matrix4fc pose, VertexConsumer consumer,
                                 float x, float y, float z, float u, float v,
                                 float r, float g, float b, float a, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    /**
     * Volumetric projectile body: a box stretched from {@code from} to {@code to}
     * with half-widths {@code hw} (along {@code side}) and {@code hd} (along the
     * second axis). All six faces, double-sided — reads as a solid round, pellet
     * or blob from every angle, never as a flat cross.
     */
    public static void orientedBox(Matrix4fc pose, VertexConsumer consumer,
                                  float fx, float fy, float fz,
                                  float tx, float ty, float tz,
                                  float sx, float sy, float sz, float hw,
                                  float wx, float wy, float wz, float hd,
                                  float r, float g, float b, float a) {
        // 8 corners: front/back x side x second-axis.
        float f1x = fx + sx * hw + wx * hd, f1y = fy + sy * hw + wy * hd, f1z = fz + sz * hw + wz * hd;
        float f2x = fx - sx * hw + wx * hd, f2y = fy - sy * hw + wy * hd, f2z = fz - sz * hw + wz * hd;
        float f3x = fx - sx * hw - wx * hd, f3y = fy - sy * hw - wy * hd, f3z = fz - sz * hw - wz * hd;
        float f4x = fx + sx * hw - wx * hd, f4y = fy + sy * hw - wy * hd, f4z = fz + sz * hw - wz * hd;
        float t1x = tx + sx * hw + wx * hd, t1y = ty + sy * hw + wy * hd, t1z = tz + sz * hw + wz * hd;
        float t2x = tx - sx * hw + wx * hd, t2y = ty - sy * hw + wy * hd, t2z = tz - sz * hw + wz * hd;
        float t3x = tx - sx * hw - wx * hd, t3y = ty - sy * hw - wy * hd, t3z = tz - sz * hw - wz * hd;
        float t4x = tx + sx * hw - wx * hd, t4y = ty + sy * hw - wy * hd, t4z = tz + sz * hw - wz * hd;
        // Front / back caps.
        quad(pose, consumer, f1x, f1y, f1z, f2x, f2y, f2z, f3x, f3y, f3z, f4x, f4y, f4z, r, g, b, a);
        quad(pose, consumer, t1x, t1y, t1z, t2x, t2y, t2z, t3x, t3y, t3z, t4x, t4y, t4z, r, g, b, a);
        // Four side walls.
        quad(pose, consumer, f1x, f1y, f1z, f4x, f4y, f4z, t4x, t4y, t4z, t1x, t1y, t1z, r, g, b, a);
        quad(pose, consumer, f2x, f2y, f2z, f1x, f1y, f1z, t1x, t1y, t1z, t2x, t2y, t2z, r, g, b, a);
        quad(pose, consumer, f3x, f3y, f3z, f2x, f2y, f2z, t2x, t2y, t2z, t3x, t3y, t3z, r, g, b, a);
        quad(pose, consumer, f4x, f4y, f4z, f3x, f3y, f3z, t3x, t3y, t3z, t4x, t4y, t4z, r, g, b, a);
    }

    /** Translate a flat polyline by a constant offset (parallel arc channels). */
    public static float[] translated(float[] pts, float ox, float oy, float oz) {
        float[] out = pts.clone();
        for (int i = 0; i < out.length; i += 3) {
            out[i] += ox;
            out[i + 1] += oy;
            out[i + 2] += oz;
        }
        return out;
    }

    /**
     * True camera-facing billboard (like CE RenderTextureProjectile): one quad
     * that always faces the viewer, so round sprites (bioblob, fireball) read
     * as balls from every angle instead of showing crossed-plane edges.
     * Call only with an unrotated pose (entity renderers); right/up are world
     * camera axes resolved by {@link #cameraAxes()}.
     */
    public static void cameraBillboard(Matrix4fc pose, VertexConsumer consumer,
                                       float cx, float cy, float cz,
                                       float rx, float ry, float rz,
                                       float ux, float uy, float uz,
                                       float half,
                                       float u0, float v0, float u1, float v1,
                                       float r, float g, float b, float a, int light) {
        uvQuad(pose, consumer,
                cx - rx * half - ux * half, cy - ry * half - uy * half, cz - rz * half - uz * half,
                cx + rx * half - ux * half, cy + ry * half - uy * half, cz + rz * half - uz * half,
                cx + rx * half + ux * half, cy + ry * half + uy * half, cz + rz * half + uz * half,
                cx - rx * half + ux * half, cy - ry * half + uy * half, cz - rz * half + uz * half,
                u0, v1, u1, v1, u1, v0, u0, v0, r, g, b, a, light);
    }

    /** World-space camera right + up vectors (6 floats). Falls back to identity. */
    public static float[] cameraAxes() {
        try {
            var rot = net.minecraft.client.Minecraft.getInstance()
                    .gameRenderer.mainCamera().rotation();
            org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0).rotate(rot);
            org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0).rotate(rot);
            return new float[]{right.x, right.y, right.z, up.x, up.y, up.z};
        } catch (Exception e) {
            return new float[]{1, 0, 0, 0, 1, 0};
        }
    }

    /**
     * Axis-aligned box centered at origin, double-sided. Squash factors stretch
     * each axis independently (slime blobs squash along the block normal).
     */
    public static void box(Matrix4fc pose, VertexConsumer consumer,
                           float sx, float sy, float sz,
                           float r, float g, float b, float a) {        float x = sx / 2;
        float y = sy / 2;
        float z = sz / 2;
        // Top / bottom.
        quad(pose, consumer, -x, y, -z, x, y, -z, x, y, z, -x, y, z, r, g, b, a);
        quad(pose, consumer, -x, -y, -z, -x, -y, z, x, -y, z, x, -y, -z, r, g, b, a);
        // North / south.
        quad(pose, consumer, -x, -y, -z, x, -y, -z, x, y, -z, -x, y, -z, r, g, b, a);
        quad(pose, consumer, -x, -y, z, -x, y, z, x, y, z, x, -y, z, r, g, b, a);
        // West / east.
        quad(pose, consumer, -x, -y, -z, -x, y, -z, -x, y, z, -x, -y, z, r, g, b, a);
        quad(pose, consumer, x, -y, -z, x, -y, z, x, y, z, x, y, -z, r, g, b, a);
    }

    /**
     * Textured box for opaque ENTITY pipelines (cutout/solid). Every face maps
     * the same UV rect — used for gib chunks sampling the meat/bone regions of
     * {@code textures/entity/gore.png}. Offset shifts the whole box so a bone
     * core can protrude from the meat (snapped-bone read).
     */
    public static void texturedBox(Matrix4fc pose, VertexConsumer consumer,
                                   float sx, float sy, float sz,
                                   float ox, float oy, float oz,
                                   float u1, float v1, float u2, float v2,
                                   float r, float g, float b, float a, int light) {
        float x = sx / 2;
        float y = sy / 2;
        float z = sz / 2;
        // Top / bottom.
        uvQuad(pose, consumer, ox - x, oy + y, oz - z, ox + x, oy + y, oz - z,
                ox + x, oy + y, oz + z, ox - x, oy + y, oz + z,
                u1, v1, u2, v1, u2, v2, u1, v2, r, g, b, a, light);
        uvQuad(pose, consumer, ox - x, oy - y, oz - z, ox - x, oy - y, oz + z,
                ox + x, oy - y, oz + z, ox + x, oy - y, oz - z,
                u1, v1, u1, v2, u2, v2, u2, v1, r, g, b, a, light);
        // North / south.
        uvQuad(pose, consumer, ox - x, oy - y, oz - z, ox + x, oy - y, oz - z,
                ox + x, oy + y, oz - z, ox - x, oy + y, oz - z,
                u1, v1, u2, v1, u2, v2, u1, v2, r, g, b, a, light);
        uvQuad(pose, consumer, ox - x, oy - y, oz + z, ox - x, oy + y, oz + z,
                ox + x, oy + y, oz + z, ox + x, oy - y, oz + z,
                u1, v1, u1, v2, u2, v2, u2, v1, r, g, b, a, light);
        // West / east.
        uvQuad(pose, consumer, ox - x, oy - y, oz - z, ox - x, oy + y, oz - z,
                ox - x, oy + y, oz + z, ox - x, oy - y, oz + z,
                u1, v1, u2, v1, u2, v2, u1, v2, r, g, b, a, light);
        uvQuad(pose, consumer, ox + x, oy - y, oz - z, ox + x, oy - y, oz + z,
                ox + x, oy + y, oz + z, ox + x, oy + y, oz - z,
                u1, v1, u1, v2, u2, v2, u2, v1, r, g, b, a, light);
    }
}
