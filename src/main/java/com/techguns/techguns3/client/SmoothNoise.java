package com.techguns.techguns3.client;

/**
 * One dimensional value noise with a smooth interpolation.
 *
 * <p>Used for the small irregularities of the weapon motion: a recoil that
 * follows the same curve every time reads as an animation, a recoil with a
 * little noise in it reads as a piece of metal.</p>
 */
public final class SmoothNoise {
    private SmoothNoise() {}

    /**
     * Noise value in [-1, 1].
     *
     * @param x     position on the noise axis
     * @param seed  seed of the noise, a different seed gives a different curve
     */
    public static float value(float x, int seed) {
        int i = (int) Math.floor(x);
        float f = x - i;
        // Smoothstep between the two neighbours, so the curve has no kinks.
        float u = f * f * (3.0f - 2.0f * f);
        float a = hash(i, seed);
        float b = hash(i + 1, seed);
        return (a + (b - a) * u) * 2.0f - 1.0f;
    }

    private static float hash(int i, int seed) {
        int h = i * 374761393 + seed * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
