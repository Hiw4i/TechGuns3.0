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

/**
 * Ammo readout of the held weapon.
 *
 * <p>A gun metal plate in the bottom right with the name of the weapon, the
 * rounds in the magazine, the rounds in the inventory, the icon of the
 * ammunition and the progress of a running reload. The charge bar of the
 * plasma gun is drawn on top of it.</p>
 */
public final class GunHudOverlay {
    private GunHudOverlay() {}

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "ammo");

    private static final Identifier PANEL =
            Identifier.fromNamespaceAndPath(com.techguns.techguns3.TechGuns3.MODID, "textures/gui/hud_panel.png");

    private static final int PANEL_W = 148;
    private static final int PANEL_H = 46;

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
        // Weapon name, dimmed.
        extractor.text(font, held.getHoverName().getString(), x + 12, y + 6, 0xFFB9BEC7, true);

        // Ammunition icon of the weapon.
        Item ammoItem = gun.ammoItem();
        if (ammoItem != null && ammoItem != net.minecraft.world.item.Items.AIR) {
            extractor.item(new ItemStack(ammoItem), x + PANEL_W - 24, y + 5);
        }

        // Rounds in the magazine, big, and the size of the magazine next to it.
        int ammoColor = loaded == 0 ? 0xFFE05A5A : loaded * 2 <= magazine ? 0xFFE8A22A : 0xFFF2F4F7;
        extractor.text(font, String.valueOf(loaded), x + 12, y + 16, ammoColor, true);
        int loadedWidth = font.width(String.valueOf(loaded));
        extractor.text(font, "/ " + magazine, x + 12 + loadedWidth + 3, y + 21, 0xFF8A9099, true);

        // Rounds in the inventory.
        if (reserve > 0) {
            extractor.text(font, String.valueOf(reserve), x + PANEL_W - 40, y + 22, 0xFFB9BEC7, true);
        }

        // Reload progress.
        float reload = GunClientHandler.reloadProgress(held);
        if (reload > 0.0f) {
            int barX = x + 12;
            int barY = y + PANEL_H - 9;
            int barW = PANEL_W - 30;
            extractor.fill(barX, barY, barX + barW, barY + 3, 0xFF16171A);
            extractor.fill(barX, barY, barX + (int) (barW * reload), barY + 3, 0xFFE8A22A);
        }

        // Charge bar of the plasma gun.
        var extras = gun.extras();
        if (extras.hasCharge() && !extras.guided()) {
            float charge = GunClientHandler.chargeProgress(held);
            if (charge >= 0.0f) {
                int barX = x + 12;
                int barY = y + PANEL_H - 9;
                int barW = PANEL_W - 30;
                boolean full = charge >= 1.0f;
                extractor.fill(barX, barY, barX + barW, barY + 3, 0xFF16171A);
                extractor.fill(barX, barY, barX + (int) (barW * charge), barY + 3,
                        full ? 0xFF39FF5E : 0xFF2A9A3A);
                if (full) {
                    extractor.text(font, "MAX", barX + barW + 3, barY - 4, 0xFF39FF5E, true);
                }
            }
        }
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
