package com.techguns.techguns3.client.fx;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Muzzle-flash artwork table (CE {@code ScreenEffect} 1:1).
 *
 * <p>Every ammunition kind has its own baked sprite sheet. The sheet is a
 * {@code grid x grid} atlas and only the first {@code frames} cells are
 * animated - the rest of the sheet is padding.</p>
 */
public final class MuzzleFlashArt {
    private MuzzleFlashArt() {}

    /** Texture atlas, cells per row and how many cells are animated. */
    public record Art(Identifier texture, int grid, int frames) {}

    private static final Map<Integer, Art> ART = Map.ofEntries(
            Map.entry(1, new Art(id("fx/muzzleflashnew_add.png"), 4, 12)),
            Map.entry(2, new Art(id("fx/muzzleflashnew_2_add_2.png"), 4, 11)),
            Map.entry(3, new Art(id("fx/muzzleflash_minigun.png"), 2, 4)),
            Map.entry(4, new Art(id("fx/bluemuzzleflash.png"), 4, 8)),
            Map.entry(5, new Art(id("fx/laserflare02.png"), 4, 7)),
            Map.entry(6, new Art(id("fx/teslaflare01.png"), 4, 14)),
            Map.entry(7, new Art(id("fx/sonicwave4x4.png"), 4, 16)),
            Map.entry(8, new Art(id("fx/nukebeamflare.png"), 4, 16)),
            Map.entry(9, new Art(id("fx/tfg_flare.png"), 4, 16)),
            Map.entry(10, new Art(id("fx/flamethrower.png"), 4, 16)),
            Map.entry(11, new Art(id("fx/lensflare1.png"), 1, 1)));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TechGuns3.MODID, "textures/" + path);
    }

    /** Art for a flash id, or {@code null} when the id has no sheet. */
    public static Art art(int flashId) {
        return ART.get(flashId);
    }
}
