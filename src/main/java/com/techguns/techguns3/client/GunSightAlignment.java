package com.techguns.techguns3.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the sight of a weapon actually ends up on screen.
 *
 * <p>The renderer reads the real position of the {@code aim} anchor bone while
 * the weapon is at the hip and caches it here. Aiming then only has to undo
 * the position of the sight, which is exact no matter how the model, the item
 * display transform or the hand placement of the game look - a hand written
 * offset would have to be recomputed for every model change.</p>
 *
 * <p>The cache is keyed by the display context, because the main hand is drawn
 * with the left context when the player uses the left arm.</p>
 */
public final class GunSightAlignment {
    private GunSightAlignment() {}

    /** The measured sight of one weapon in one hand. */
    private static final class Measurement {
        float x;
        float y;
        long stamp;
    }

    private static final Map<Item, Map<ItemDisplayContext, Measurement>> MEASUREMENTS =
            new ConcurrentHashMap<>();

    /** How fast the measurement follows the weapon, in seconds. */
    private static final float SMOOTHING = 0.15f;

    /**
     * Records the screen position of the rear sight of a weapon at the hip.
     *
     * <p>The value is smoothed: the measured position also contains the sway
     * and the walk bob of the weapon, and the average of those is the real
     * position of the sight. The smoothing runs over time and not over frames,
     * so the measurement settles the same way at any frame rate.</p>
     *
     * @param x camera space X of the rear sight
     * @param y camera space Y of the rear sight
     */
    public static void recordAim(Item item, ItemDisplayContext perspective, float x, float y) {
        Measurement measurement = MEASUREMENTS
                .computeIfAbsent(item, i -> new ConcurrentHashMap<>())
                .computeIfAbsent(perspective, p -> new Measurement());
        long now = System.nanoTime();
        float dt = measurement.stamp == 0L ? 0.0f : (now - measurement.stamp) / 1_000_000_000.0f;
        measurement.stamp = now;
        float factor = dt <= 0.0f ? 0.08f : 1.0f - (float) Math.exp(-dt / SMOOTHING);
        measurement.x += (x - measurement.x) * factor;
        measurement.y += (y - measurement.y) * factor;
    }

    /** Screen position of the sight at the hip, or {@code null} if unknown. */
    public static float[] hip(Item item, ItemDisplayContext perspective) {
        Measurement measurement = MEASUREMENTS.get(item) == null
                ? null : MEASUREMENTS.get(item).get(perspective);
        return measurement == null ? null : new float[]{measurement.x, measurement.y};
    }
}
