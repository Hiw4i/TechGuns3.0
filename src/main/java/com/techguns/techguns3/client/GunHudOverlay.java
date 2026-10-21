package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Ammo readout of the held weapon.
 *
 * <p>A gun metal plate in the bottom right with the name of the weapon, the
 * rounds in the magazine, the rounds in the inventory, the icon of the
 * ammunition, the fire mode and the progress of a running reload. The charge
 * bar of the plasma gun is drawn on top of it.</p>
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID, value = Dist.CLIENT)
public final class GunHudOverlay {
    private GunHudOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "ammo");

    private static final Identifier PANEL =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "textures/gui/hud_panel.png");

    private static final int PANEL_W = 148;
    private static final int PANEL_H = 46;

    /** Below this part of the magazine the counter starts to pulse. */
    private static final float LOW_AMMO_SHARE = 0.25f;

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tracker) {
        if (!TGConfig.ammoHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) return;

        var stats = gun.stats();
        int loaded = gun.loadedRounds(held);
        int magazine = stats.magazineSize();
        int reserve = countReserve(mc.player.getInventory(), gun);

        int x = extractor.guiWidth() - PANEL_W - 6;
        int y = extractor.guiHeight() - PANEL_H - 6;

        extractor.blit(PANEL, x, y, x + PANEL_W, y + PANEL_H, 0.0f, 1.0f, 0.0f, 1.0f);

        var font = mc.font;
        // Weapon name, dimmed. Long names are cut so they never run into the
        // icon of the ammunition.
        String label = held.getHoverName().getString();
        int room = PANEL_W - 12 - 26;
        if (font.width(label) > room) {
            label = font.plainSubstrByWidth(label, Math.max(0, room - font.width("..."))) + "...";
        }
        extractor.text(font, label, x + 12, y + 6, 0xFFB9BEC7, true);

        // Ammunition icon of the weapon.
        Item ammoItem = gun.ammoItem();
        if (ammoItem != null && ammoItem != net.minecraft.world.item.Items.AIR) {
            extractor.item(new ItemStack(ammoItem), x + PANEL_W - 22, y + 4);
        }

        // Rounds in the magazine, big, and the size of the magazine next to it.
        // A nearly empty magazine pulses, so the player sees it without
        // having to read the number.
        int ammoColor = 0xFFF2F4F7;
        if (loaded == 0) {
            ammoColor = 0xFFE05A5A;
        } else if (magazine > 0 && loaded <= magazine * LOW_AMMO_SHARE) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.nanoTime() / 120_000_000.0);
            ammoColor = lerpColor(0xFFE05A5A, 0xFFE8A22A, pulse);
        }
        extractor.text(font, String.valueOf(loaded), x + 12, y + 16, ammoColor, true);
        int loadedWidth = font.width(String.valueOf(loaded));
        extractor.text(font, "/ " + magazine, x + 12 + loadedWidth + 3, y + 21, 0xFF8A9099, true);

        // The fire mode of the weapon, behind the rounds.
        extractor.text(font, stats.semiAuto() ? "SEMI" : "AUTO", x + 78, y + 21, 0xFF6E747E, true);

        // Rounds in the inventory.
        if (reserve > 0) {
            extractor.text(font, String.valueOf(reserve), x + PANEL_W - 40, y + 22, 0xFFB9BEC7, true);
        }

        // Reload progress.
        float reload = GunClientHandler.reloadProgress(held);
        if (reload > 0.0f) {
            drawBar(extractor, x + 12, y + PANEL_H - 9, PANEL_W - 30, reload, 0xFFE8A22A);
        }

        // Charge bar of the plasma gun.
        var extras = gun.extras();
        if (extras.hasCharge() && !extras.guided()) {
            float charge = GunClientHandler.chargeProgress(held);
            if (charge >= 0.0f) {
                boolean full = charge >= 1.0f;
                drawBar(extractor, x + 12, y + PANEL_H - 9, PANEL_W - 30, charge,
                        full ? 0xFF39FF5E : 0xFF2A9A3A);
                if (full) {
                    extractor.text(font, "MAX", x + PANEL_W - 30, y + PANEL_H - 13, 0xFF39FF5E, true);
                }
            }
        }
    }

    /** A bar with a dark groove and a coloured fill. */
    private static void drawBar(GuiGraphicsExtractor extractor, int x, int y, int width,
                                 float progress, int color) {
        extractor.fill(x, y, x + width, y + 3, 0xFF16171A);
        int filled = (int) (width * Math.max(0.0f, Math.min(1.0f, progress)));
        if (filled > 0) {
            extractor.fill(x, y, x + filled, y + 3, color);
        }
    }

    /** Blends two colours, the alpha of the result is taken from the first. */
    private static int lerpColor(int from, int to, float t) {
        float k = Math.max(0.0f, Math.min(1.0f, t));
        int a = (from >> 24) & 0xFF;
        int r = (int) (((from >> 16) & 0xFF) * (1 - k) + ((to >> 16) & 0xFF) * k);
        int g = (int) (((from >> 8) & 0xFF) * (1 - k) + ((to >> 8) & 0xFF) * k);
        int b = (int) ((from & 0xFF) * (1 - k) + (to & 0xFF) * k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rounds of the matching ammunition in the inventory, without the gun itself. */
    private static int countReserve(Inventory inventory, GenericGunItem gun) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(gun.ammoItem())) {
                total += stack.getCount();
            } else if (gun.acceptedAmmoItems().contains(stack.getItem())) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
