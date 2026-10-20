package com.techguns.techguns3.client;

import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Crosshair of the held weapon.
 *
 * <p>The gap of the crosshair follows the spread of the weapon, so it opens up
 * while the weapon is inaccurate and closes while aiming. A hit marker is
 * drawn for a moment when a shot of the player hits something.</p>
 */
public final class GunCrosshairOverlay {
    private GunCrosshairOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "crosshair");

    /** Guns at or below this zoom FOV count as scoped and draw their own crosshair. */
    private static final float SCOPE_ZOOM_FOV = 0.5f;

    /** How long the hit marker stays visible, in nanoseconds. */
    private static final long HIT_MARKER_NANOS = 320_000_000L;

    private static long hitAt;
    private static boolean hitKill;

    /** Called when the server confirms that a shot hit something. */
    public static void onHit(boolean killed) {
        hitAt = System.nanoTime();
        hitKill = killed;
    }

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;

        int w = extractor.guiWidth();
        int h = extractor.guiHeight();
        int cx = w / 2;
        int cy = h / 2;

        float gap = -1.0f;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof GenericGunItem gun) {
            var stats = gun.stats();
            boolean scoped = stats.canZoom() && stats.zoomFov() <= SCOPE_ZOOM_FOV;
            if (!scoped) {
                float spread = stats.spread();
                if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                    spread *= stats.zoomSpreadMult();
                }
                gap = 3.0f + spread * 190.0f;
            }
        }
        if (gap >= 0.0f) {
            drawCrosshair(extractor, cx, cy, gap, 0xE0FFFFFF);
        }

        long age = System.nanoTime() - hitAt;
        if (hitAt != 0L && age < HIT_MARKER_NANOS) {
            float k = 1.0f - (float) age / HIT_MARKER_NANOS;
            int color = hitKill ? 0xFFFF5A4A : 0xFFFFFFFF;
            int alpha = (int) (k * 255) << 24 | (color & 0xFFFFFF);
            drawHitMarker(extractor, cx, cy, 3 + (int) ((1.0f - k) * 3.0f), alpha);
        }
    }

    private static void drawCrosshair(GuiGraphicsExtractor extractor, int cx, int cy, float gap, int color) {
        int len = 5;
        int g = (int) Math.round(gap);
        extractor.fill(cx - g - len, cy - 1, cx - g, cy, color);
        extractor.fill(cx + g, cy - 1, cx + g + len, cy, color);
        extractor.fill(cx - 1, cy - g - len, cx, cy - g, color);
        extractor.fill(cx - 1, cy + g, cx, cy + g + len, color);
    }

    private static void drawHitMarker(GuiGraphicsExtractor extractor, int cx, int cy, int offset, int color) {
        for (int i = 0; i < 3; i++) {
            extractor.fill(cx - offset - i, cy - offset - i, cx - offset - i + 1, cy - offset - i + 2, color);
            extractor.fill(cx + offset + i, cy - offset - i, cx + offset + i + 1, cy - offset - i + 2, color);
            extractor.fill(cx - offset - i, cy + offset + i, cx - offset - i + 1, cy + offset + i + 2, color);
            extractor.fill(cx + offset + i, cy + offset + i, cx + offset + i + 1, cy + offset + i + 2, color);
        }
    }
}
