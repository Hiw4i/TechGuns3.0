package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Crosshair of the held weapon.
 *
 * <p>The gap of the crosshair follows the spread of the weapon, so it opens up
 * while the weapon is inaccurate and closes while aiming. A hit marker is
 * drawn for a moment when a shot of the player hits something.</p>
 *
 * <p>While the weapon is aimed the model brings its own sights to the
 * crosshair, so the marker of the screen fades away and the iron sights of the
 * weapon take over. A scoped weapon has no iron sights and gets the reticle
 * of the scope instead.</p>
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID, value = Dist.CLIENT)
public final class GunCrosshairOverlay {
    private GunCrosshairOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "crosshair");

    /** How long the hit marker stays visible, in nanoseconds. */
    private static final long HIT_MARKER_NANOS = 320_000_000L;

    /** The crosshair is gone once the weapon is this far aimed. */
    private static final float HIDE_AT_AIM = 0.85f;

    private static long hitAt;
    private static boolean hitKill;
    /** The gap the crosshair is drawn with, smoothed. */
    private static float smoothedGap = -1.0f;

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
        float fade = 1.0f;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof GenericGunItem gun) {
            var stats = gun.stats();
            boolean scoped = stats.canZoom() && stats.zoomFov() <= TechGunsGeoItemRenderer.SCOPE_ZOOM_FOV;
            if (!scoped) {
                float spread = stats.spread();
                if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                    spread *= stats.zoomSpreadMult();
                }
                gap = 3.0f + spread * 190.0f;
                // The iron sights of the weapon come to the crosshair while
                // aiming, the marker of the screen steps out of the way.
                float aim = GunFeel.get().aimProgress();
                if (aim > 0.55f) {
                    fade = Math.max(0.0f, 1.0f - (aim - 0.55f) / (HIDE_AT_AIM - 0.55f));
                }
            }
        }
        if (gap >= 0.0f) {
            // The gap follows the spread of the weapon, a jump of the spread
            // would make the crosshair twitch. The smoothing runs over time,
            // so it is the same at any frame rate.
            float dt = tracker.getRealtimeDeltaTicks() / 20.0f;
            float factor = 1.0f - (float) Math.exp(-dt / 0.05f);
            smoothedGap = smoothedGap < 0.0f ? gap : smoothedGap + (gap - smoothedGap) * factor;
            if (fade > FxGeometry.MIN_VISIBLE_ALPHA) {
                int alpha = (int) (fade * 0.88f * 255.0f) << 24 | 0xFFFFFF;
                drawCrosshair(extractor, cx, cy, smoothedGap, alpha);
            }
        } else {
            smoothedGap = -1.0f;
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
