package com.techguns.techguns3.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4fc;

/**
 * Quad emission matching vanilla's lightning pipeline (position + color only).
 * Every quad is emitted double-sided so tracers and arcs read from any angle.
 */
public final class FxGeometry {
    private FxGeometry() {}

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
                             float r, float g, float b, float a) {
        quad(pose, consumer,
                fx + sx * halfWidth, fy + sy * halfWidth, fz + sz * halfWidth,
                fx - sx * halfWidth, fy - sy * halfWidth, fz - sz * halfWidth,
                tx - sx * halfWidth, ty - sy * halfWidth, tz - sz * halfWidth,
                tx + sx * halfWidth, ty + sy * halfWidth, tz + sz * halfWidth,
                r, g, b, a);
    }
}
