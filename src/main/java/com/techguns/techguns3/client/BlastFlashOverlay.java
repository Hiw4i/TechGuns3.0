package com.techguns.techguns3.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Fullscreen white fade punched by nearby detonations (driven by
 * {@link TGShake}). Subtle by design: a blink, never a whiteout.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class BlastFlashOverlay {
    private BlastFlashOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "blast_flash");

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        float flash = TGShake.flash();
        if (flash <= 0.01f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        int w = extractor.guiWidth();
        int h = extractor.guiHeight();
        int alpha = Math.min(110, (int) (flash * 110.0f));
        extractor.fill(0, 0, w, h, (alpha << 24) | 0xFFF6E8);
    }
}
