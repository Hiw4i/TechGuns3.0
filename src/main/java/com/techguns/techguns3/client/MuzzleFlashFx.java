package com.techguns.techguns3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.client.fx.FxRenderTypes;
import com.techguns.techguns3.client.fx.MuzzleFlashArt;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.Random;

/**
 * First-person muzzle flash.
 *
 * <p>The world-entity flash is right for everybody watching the shooter, but
 * in first person the weapon is a view model: a flash sitting at a fixed
 * world offset never lines up with the barrel and never moves with the
 * recoil. This effect is drawn inside the view model instead, on the
 * {@code muzzle} anchor bone, so it is glued to the barrel the whole time.</p>
 *
 * <p>Triggered locally on the shot the client predicted, so it appears on the
 * same frame as the trigger pull instead of a round trip later.</p>
 */
public final class MuzzleFlashFx {
    private MuzzleFlashFx() {}

    /** How long the flash lives. CE shows it for 50 ms. */
    private static final long DURATION_NANOS = 70_000_000L;

    private static int flashId;
    private static int color = 0xFFFFFF;
    private static float scale = 0.5f;
    private static long startedAt;
    private static float roll;

    /** Fire the flash for the local view model. */
    public static void trigger(int flashId, int color, float scale) {
        MuzzleFlashFx.flashId = flashId;
        MuzzleFlashFx.color = color;
        MuzzleFlashFx.scale = scale;
        MuzzleFlashFx.startedAt = System.nanoTime();
        MuzzleFlashFx.roll = new Random().nextFloat() * (float) (Math.PI * 2.0);
    }

    /** How big the flash is, in model units. */
    public static float size() {
        return scale;
    }

    public static boolean isActive() {
        return flashId != 0 && System.nanoTime() - startedAt < DURATION_NANOS;
    }

    /** 0 at spawn, 1 when the flash is gone. */
    public static float age() {
        return Math.min(1.0f, (System.nanoTime() - startedAt) / (float) DURATION_NANOS);
    }

    public static void clear() {
        flashId = 0;
        startedAt = 0L;
    }

    /**
     * Draw the flash at the current pose (the muzzle anchor bone).
     *
     * @param size how big the flash is in model units
     */
    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float size) {
        if (!isActive()) return;
        MuzzleFlashArt.Art art = MuzzleFlashArt.art(flashId);
        if (art == null) return;

        float t = age();
        // Pop: full size on the first frame, then shrink while fading out.
        float pop = 0.72f + 0.28f * (1.0f - t);
        float s = size * pop;
        float alpha = Math.min(1.0f, (1.0f - t) * 1.6f);
        if (alpha <= 0.02f) return;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        int frame = Math.min(art.frames() - 1, (int) (t * art.frames()));
        int grid = art.grid();
        float u0 = (frame % grid) / (float) grid;
        float v0 = (frame / grid) / (float) grid;
        float u1 = u0 + 1.0f / grid;
        float v1 = v0 + 1.0f / grid;

        float cos = (float) Math.cos(roll);
        float sin = (float) Math.sin(roll);
        final float fs = s;
        final float fa = alpha;
        final float fr = r;
        final float fg = g;
        final float fb = b;
        final float fu0 = u0;
        final float fv0 = v0;
        final float fu1 = u1;
        final float fv1 = v1;

        collector.submitCustomGeometry(poseStack, FxRenderTypes.additive(art.texture()),
                (pose, consumer) -> {
                    var matrix = pose.pose();
                    // Three crossed planes: the barrel points away from the
                    // camera, so a single quad would be invisible edge-on.
                    rolledQuad(matrix, consumer, fs, fu0, fv0, fu1, fv1, fr, fg, fb, fa, cos, sin);
                    FxGeometry.uvQuad(matrix, consumer,
                            0, -fs, -fs, 0, -fs, fs, 0, fs, fs, 0, fs, -fs,
                            fu0, fv0, fu1, fv0, fu1, fv1, fu0, fv1,
                            fr, fg, fb, fa * 0.85f, FxGeometry.FULLBRIGHT);
                    FxGeometry.uvQuad(matrix, consumer,
                            -fs, 0, -fs, fs, 0, -fs, fs, 0, fs, -fs, 0, fs,
                            fu0, fv0, fu1, fv0, fu1, fv1, fu0, fv1,
                            fr, fg, fb, fa * 0.7f, FxGeometry.FULLBRIGHT);
                });
    }

    /** Sprite rotated around Z, so no two shots look the same. */
    private static void rolledQuad(org.joml.Matrix4fc pose,
                                   com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                   float s, float u0, float v0, float u1, float v1,
                                   float r, float g, float b, float a,
                                   float cos, float sin) {
        float x1 = -s * cos + s * sin;
        float y1 = -s * sin - s * cos;
        float x2 = s * cos + s * sin;
        float y2 = s * sin - s * cos;
        float x3 = s * cos - s * sin;
        float y3 = s * sin + s * cos;
        float x4 = -s * cos - s * sin;
        float y4 = -s * sin + s * cos;
        FxGeometry.uvQuad(pose, consumer,
                x1, y1, 0, x2, y2, 0, x3, y3, 0, x4, y4, 0,
                u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, a, FxGeometry.FULLBRIGHT);
    }
}
