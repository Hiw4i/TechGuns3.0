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

    /** The measured sight, in camera space. */
    private static final Map<Item, Map<ItemDisplayContext, float[]>> HIPS = new ConcurrentHashMap<>();

    /**
     * Records the screen position of the rear sight of a weapon at the hip.
     *
     * <p>The value is smoothed: the measured position also contains the sway
     * and the walk bob of the weapon, and the average of those is the real
     * position of the sight.</p>
     *
     * @param x camera space X of the rear sight
     * @param y camera space Y of the rear sight
     */
    public static void recordAim(Item item, ItemDisplayContext perspective, float x, float y) {
        float[] slot = HIPS.computeIfAbsent(item, i -> new ConcurrentHashMap<>())
                .computeIfAbsent(perspective, p -> new float[2]);
        slot[0] += (x - slot[0]) * 0.08f;
        slot[1] += (y - slot[1]) * 0.08f;
    }

    /** Screen position of the sight at the hip, or {@code null} if unknown. */
    public static float[] hip(Item item, ItemDisplayContext perspective) {
        Map<ItemDisplayContext, float[]> byPerspective = HIPS.get(item);
        return byPerspective == null ? null : byPerspective.get(perspective);
    }
}
