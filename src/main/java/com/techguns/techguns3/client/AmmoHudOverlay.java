package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Bottom-right ammo readout while a gun is held. Vanilla HUD stays untouched;
 * this layer only adds text and respects the {@code ammoHud} client toggle.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class AmmoHudOverlay {
    private AmmoHudOverlay() {}

    public static final net.minecraft.resources.Identifier LAYER_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "ammo");

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        if (!TGConfig.ammoHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) return;
        int loaded = gun.loadedRounds(held);
        int mag = gun.stats().magazineSize();
        int color = loaded == 0 ? 0xFF5555 : loaded * 2 <= mag ? 0xFFAA00 : 0xFFFFFF;
        String text = loaded + " / " + mag;
        var font = mc.font;
        int x = extractor.guiWidth() - font.width(text) - 10;
        int y = extractor.guiHeight() - 30;
        if (loaded == 0) {
            extractor.text(font, Component.translatable("hud.techguns3.reload_hint"), x - 70, y - 12, 0xFF5555);
        }
        extractor.text(font, text, x, y, color);
        // TFG-style charge bar while LMB is held (purely local: the client sent
        // FireStart itself, so progress = held time / full charge time).
        if (gun.extras().hasCharge() && !gun.extras().guided()
                && mc.options.keyAttack.isDown() && GunClientHandler.chargeProgress(held) >= 0.0f) {
            float charge = GunClientHandler.chargeProgress(held);
            int bw = 90;
            int bx = extractor.guiWidth() - bw - 10;
            int by = y - 14;
            int fill = (int) (bw * Math.max(0.0f, Math.min(1.0f, charge)));
            int barColor = charge >= 1.0f ? 0xFF39FF5E : 0xFF2A9A3A;
            extractor.fill(bx - 1, by - 1, bx + bw + 1, by + 5, 0xFF101010);
            extractor.fill(bx, by, bx + fill, by + 4, barColor);
            if (charge >= 1.0f) {
                extractor.text(font, Component.literal("MAX"), bx + bw + 4, by - 2, 0xFF39FF5E);
            }
        }
    }
}
