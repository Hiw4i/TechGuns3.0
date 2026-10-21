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

    /**
     * Draw the flash at the current pose (the muzzle anchor bone).
     *
     * <p>The sprite is turned into a billboard: the pose stack is inverted to
     * find the camera axes in the space of the weapon model, so the flash
     * always faces the player no matter how the weapon is oriented.</p>
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

        // Camera axes in the space of the weapon model.
        org.joml.Matrix4f model = new org.joml.Matrix4f(poseStack.last().pose());
        org.joml.Matrix3f inverse = new org.joml.Matrix3f(model).invert();
        org.joml.Vector3f right = inverse.transform(new org.joml.Vector3f(1, 0, 0)).normalize();
        org.joml.Vector3f up = inverse.transform(new org.joml.Vector3f(0, 1, 0)).normalize();
        // Random roll, so no two shots look the same.
        float cos = (float) Math.cos(roll);
        float sin = (float) Math.sin(roll);
        float rx = right.x * cos + up.x * sin;
        float ry = right.y * cos + up.y * sin;
        float rz = right.z * cos + up.z * sin;
        float ux = -right.x * sin + up.x * cos;
        float uy = -right.y * sin + up.y * cos;
        float uz = -right.z * sin + up.z * cos;

        final float fs = s;
        final float fa = alpha;
        final float fr = r;
        final float fg = g;
        final float fb = b;
        final float fu0 = u0;
        final float fv0 = v0;
        final float fu1 = u1;
        final float fv1 = v1;
        final float frx = rx, fry = ry, frz = rz;
        final float fux = ux, fuy = uy, fuz = uz;

        collector.submitCustomGeometry(poseStack,
                FxRenderTypes.styled(FxRenderTypes.Style.SPRITE, art.texture()),
                (pose, consumer) -> {
                    var matrix = pose.pose();
                    FxGeometry.uvQuad(matrix, consumer,
                            -fs * frx - fs * fux, -fs * fry - fs * fuy, -fs * frz - fs * fuz,
                            fs * frx - fs * fux, fs * fry - fs * fuy, fs * frz - fs * fuz,
                            fs * frx + fs * fux, fs * fry + fs * fuy, fs * frz + fs * fuz,
                            -fs * frx + fs * fux, -fs * fry + fs * fuy, -fs * frz + fs * fuz,
                            fu0, fv0, fu1, fv0, fu1, fv1, fu0, fv1,
                            fr, fg, fb, fa, FxGeometry.FULLBRIGHT);
                    // A second, smaller and hotter sprite in the middle gives
                    // the flash a white hot heart.
                    float core = fs * 0.45f;
                    FxGeometry.uvQuad(matrix, consumer,
                            -core * frx - core * fux, -core * fry - core * fuy, -core * frz - core * fuz,
                            core * frx - core * fux, core * fry - core * fuy, core * frz - core * fuz,
                            core * frx + core * fux, core * fry + core * fuy, core * frz + core * fuz,
                            -core * frx + core * fux, -core * fry + core * fuy, -core * frz + core * fuz,
                            fu0, fv0, fu1, fv0, fu1, fv1, fu0, fv1,
                            1, 1, 1, Math.min(1.0f, fa * 1.3f), FxGeometry.FULLBRIGHT);
                });
    }
}
