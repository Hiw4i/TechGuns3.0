package com.techguns.techguns3.client;

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
 * Scope mask for high-magnification guns (bolt-action): dark surround + thin
 * crosshair drawn code-only while RMB-aiming. Red dots and reflex sights stay
 * bare glass; only true scopes get the tube.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ScopeOverlay {
    private ScopeOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "scope");

    /** Guns at or below this zoom FOV count as scoped. */
    private static final float SCOPE_ZOOM_FOV = 0.5f;

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        if (!mc.player.isUsingItem() || mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) return;
        ItemStack using = mc.player.getUseItem();
        if (!(using.getItem() instanceof GenericGunItem gun)) return;
        if (!gun.stats().canZoom() || gun.stats().zoomFov() > SCOPE_ZOOM_FOV) return;

        int w = extractor.guiWidth();
        int h = extractor.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        int r = Math.min(w, h) / 2 - 8;
        int scope = 0xFF0A0A0A;
        // Dark surround outside the tube.
        extractor.fill(0, 0, w, cy - r, scope);
        extractor.fill(0, cy + r, w, h, scope);
        extractor.fill(0, cy - r, cx - r, cy + r, scope);
        extractor.fill(cx + r, cy - r, w, cy + r, scope);
        // Tube rim + thin crosshair.
        extractor.outline(cx - r, cy - r, r * 2, r * 2, 0xFF000000);
        extractor.horizontalLine(cx - r, cx + r, cy, 0xCCFFFFFF);
        extractor.verticalLine(cx, cy - r, cy + r, 0xCCFFFFFF);
    }
}
