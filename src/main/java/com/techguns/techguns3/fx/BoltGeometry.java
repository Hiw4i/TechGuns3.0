package com.techguns.techguns3.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fractal lightning bolt, pure math, no Minecraft dependencies.
 *
 * <p>Midpoint-displacement main channel (no straight core segment survives),
 * recursive side branches, fully determined by the seed — server and every
 * client rebuild the identical bolt from {@code (seed, endpoints)}.</p>
 */
public final class BoltGeometry {
    private BoltGeometry() {}

    /** Tesla defaults: 5 subdivisions, 2 short capped branches. */
    public static final int TESLA_SUBDIVISIONS = 5;
    public static final int TESLA_BRANCHES = 2;
    /** Branches never exceed this no matter how long the main channel is. */
    public static final float MAX_BRANCH_METERS = 2.0f;

    public record Bolt(float[] main, List<float[]> branches) {}

    public static Bolt tesla(long seed,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1) {
        return generate(seed, x0, y0, z0, x1, y1, z1,
                TESLA_SUBDIVISIONS, 0.16f, TESLA_BRANCHES, 0.12f);
    }

    public static Bolt generate(long seed,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                int subdivisions, float roughness,
                                int branchCount, float branchLenFrac) {
        Random rand = new Random(seed);
        float[] main = displace(rand,
                new float[]{x0, y0, z0}, new float[]{x1, y1, z1}, subdivisions, roughness);
        float totalLen = dist(x0, y0, z0, x1, y1, z1);
        List<float[]> branches = new ArrayList<>(branchCount);
        for (int b = 0; b < branchCount; b++) {
            float t = 0.15f + 0.70f * rand.nextFloat();
            float[] base = pointAt(main, t);
            float[] tangent = tangentAt(main, t);
            // Fork 20-60° off the main direction, random azimuth via basis.
            float[] u = perpA(tangent, rand);
            float[] w = cross(tangent, u);
            float ang = (float) Math.toRadians(20 + 40 * rand.nextFloat());
            float[] dir = norm(new float[]{
                    tangent[0] + (float) (u[0] * Math.tan(ang) * (rand.nextBoolean() ? 1 : -1)
                            + w[0] * (rand.nextFloat() * 2 - 1) * 0.8f),
                    tangent[1] + (float) (u[1] * Math.tan(ang) * (rand.nextBoolean() ? 1 : -1)
                            + w[1] * (rand.nextFloat() * 2 - 1) * 0.8f) + 0.15f,
                    tangent[2] + (float) (u[2] * Math.tan(ang) * (rand.nextBoolean() ? 1 : -1)
                            + w[2] * (rand.nextFloat() * 2 - 1) * 0.8f)});
            float len = Math.min(totalLen * branchLenFrac * (0.7f + 0.6f * rand.nextFloat()),
                    MAX_BRANCH_METERS);
            float[] tip = new float[]{base[0] + dir[0] * len, base[1] + dir[1] * len, base[2] + dir[2] * len};
            branches.add(displace(new Random(rand.nextLong()), base, tip, 3, 0.35f));
        }
        return new Bolt(main, branches);
    }

    private static float[] displace(Random rand, float[] a, float[] b, int subdivisions, float roughness) {
        List<float[]> pts = new ArrayList<>();
        pts.add(a);
        pts.add(b);
        for (int s = 0; s < subdivisions; s++) {
            List<float[]> next = new ArrayList<>(pts.size() * 2 - 1);
            next.add(pts.get(0));
            for (int i = 0; i < pts.size() - 1; i++) {
                float[] p = pts.get(i);
                float[] q = pts.get(i + 1);
                float segLen = dist(p[0], p[1], p[2], q[0], q[1], q[2]);
                float amp = segLen * roughness;
                float[] d = norm(new float[]{q[0] - p[0], q[1] - p[1], q[2] - p[2]});
                float[] u = perpA(d, rand);
                float[] w = cross(d, u);
                float ou = (rand.nextFloat() * 2 - 1) * amp;
                float ow = (rand.nextFloat() * 2 - 1) * amp;
                next.add(new float[]{(p[0] + q[0]) / 2 + u[0] * ou + w[0] * ow,
                        (p[1] + q[1]) / 2 + u[1] * ou + w[1] * ow,
                        (p[2] + q[2]) / 2 + u[2] * ou + w[2] * ow});
                next.add(q);
            }
            pts = next;
        }
        float[] flat = new float[pts.size() * 3];
        for (int i = 0; i < pts.size(); i++) {
            flat[i * 3] = pts.get(i)[0];
            flat[i * 3 + 1] = pts.get(i)[1];
            flat[i * 3 + 2] = pts.get(i)[2];
        }
        return flat;
    }

    private static float[] pointAt(float[] poly, float t) {
        int segs = poly.length / 3 - 1;
        float f = Math.min(segs - 1e-4f, Math.max(0, t * segs));
        int i = (int) f;
        float k = f - i;
        return new float[]{lerp(poly[i * 3], poly[i * 3 + 3], k),
                lerp(poly[i * 3 + 1], poly[i * 3 + 4], k),
                lerp(poly[i * 3 + 2], poly[i * 3 + 5], k)};
    }

    private static float[] tangentAt(float[] poly, float t) {
        int segs = poly.length / 3 - 1;
        int i = Math.min(segs - 1, Math.max(0, (int) (t * segs)));
        return norm(new float[]{poly[i * 3 + 3] - poly[i * 3], poly[i * 3 + 4] - poly[i * 3 + 1],
                poly[i * 3 + 5] - poly[i * 3 + 2]});
    }

    private static float[] perpA(float[] d, Random rand) {
        float[] up = Math.abs(d[1]) > 0.94f ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        float[] u = cross(d, up);
        float len = (float) Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        if (len < 1e-6f) return new float[]{1, 0, 0};
        return new float[]{u[0] / len, u[1] / len, u[2] / len};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] norm(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1e-9f) return new float[]{0, 0, 1};
        return new float[]{v[0] / len, v[1] / len, v[2] / len};
    }

    private static float dist(float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
