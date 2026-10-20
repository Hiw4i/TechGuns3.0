package com.techguns.techguns3.client;

import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the sight of a weapon actually ends up on screen.
 *
 * <p>The renderer reads the real position of the {@code aim} anchor bone while
 * the weapon is at the hip and caches it here. Aiming then only has to undo
 * that position, which is exact no matter how the model, the item display
 * transform or the hand placement of the game look - a hand written offset
 * would have to be recomputed for every model change.</p>
 */
public final class GunSightAlignment {
    private GunSightAlignment() {}

    private static final Map<Item, float[]> RIGHT = new ConcurrentHashMap<>();
    private static final Map<Item, float[]> LEFT = new ConcurrentHashMap<>();

    /**
     * Records the screen position of the sight of a weapon at the hip.
     *
     * <p>The value is smoothed: the measured position also contains the sway
     * and the walk bob of the weapon, and the average of those is the real
     * position of the sight.</p>
     */
    public static void record(Item item, boolean leftHand, float x, float y) {
        Map<Item, float[]> map = leftHand ? LEFT : RIGHT;
        float[] slot = map.get(item);
        if (slot == null) {
            map.put(item, new float[]{x, y});
            return;
        }
        slot[0] += (x - slot[0]) * 0.08f;
        slot[1] += (y - slot[1]) * 0.08f;
    }

    /** Screen position of the sight at the hip, or {@code null} if unknown. */
    public static float[] hip(Item item, boolean leftHand) {
        return (leftHand ? LEFT : RIGHT).get(item);
    }
}
