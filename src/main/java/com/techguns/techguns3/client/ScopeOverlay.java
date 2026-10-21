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
 * Scope mask for high-magnification guns (bolt-action): the world is hidden
 * outside of the lens and the reticle of the scope is drawn over it. Red dots
 * and reflex sights stay bare glass; only true scopes get the tube.
 *
 * <p>The lens opens up while the weapon is raised, so the scope looks like the
 * eye moves behind the eyepiece instead of a black ring appearing on the
 * screen.</p>
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID, value = Dist.CLIENT)
public final class ScopeOverlay {
    private ScopeOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "scope");

    private static final Identifier SCOPE_TEXTURE = Identifier.fromNamespaceAndPath(
            com.techguns.techguns3.TechGuns3.MODID, "textures/gui/scope.png");

    /** Guns at or below this zoom FOV count as scoped. */
    private static final float SCOPE_ZOOM_FOV = 0.5f;

    /** The lens covers this part of the smaller screen dimension. */
    private static final float LENS_SHARE = 0.46f;

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        if (!mc.player.isUsingItem() || mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) return;
        ItemStack using = mc.player.getUseItem();
        if (!(using.getItem() instanceof GenericGunItem gun)) return;
        var stats = gun.stats();
        if (!stats.canZoom() || stats.zoomFov() > SCOPE_ZOOM_FOV) return;

        // The lens opens up while the weapon comes to the eye.
        float aim = GunFeel.get().aimProgress();
        if (aim <= 0.01f) return;

        int w = extractor.guiWidth();
        int h = extractor.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        int r = (int) (Math.min(w, h) * LENS_SHARE * aim);
        if (r < 2) return;

        // The wall of the tube, the same colour the mask ends with.
        int scope = 0xFF050608;
        // Dark surround outside the tube.
        extractor.fill(0, 0, w, cy - r, scope);
        extractor.fill(0, cy + r, w, h, scope);
        extractor.fill(0, cy - r, cx - r, cy + r, scope);
        extractor.fill(cx + r, cy - r, w, cy + r, scope);
        // The lens: the vignette hides everything outside of it and the
        // reticle of the scope is drawn over the view.
        extractor.blit(SCOPE_TEXTURE, cx - r, cy - r, cx + r, cy + r,
                0.0f, 1.0f, 0.0f, 1.0f);
    }
}
