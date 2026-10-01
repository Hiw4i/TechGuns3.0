package com.techguns.techguns3.fx;

import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.item.GunExtras;
import com.techguns.techguns3.item.GunStats;

/**
 * Per-ammunition cinematic presets (TG2 FXList feel, data instead of hardcoded
 * renderer values). Single source of truth for muzzle color, flash size,
 * tracer color and smoke amount so server combat, client prediction and
 * impact FX always agree.
 */
public final class GunFxPresets {
    private GunFxPresets() {}

    /** Tracer hues per ammunition kind; the client branches its trail look on these. */
    public static final int TRACER_WARM = 0xFFD9A8;
    public static final int TRACER_BIO = 0x7CFC66;
    public static final int TRACER_FIRE = 0xFF8A1A;
    public static final int TRACER_TESLA = 0x7FEBFF;
    // Wave-2 (CE FXList colors, 1:1):
    public static final int TRACER_TFG = 0x39FF5E;
    public static final int TRACER_LASER = 0xFF2A1A;
    public static final int TRACER_SONIC = 0xCFE8FF;
    public static final int TRACER_GAUSS = 0x4DC6FF;
    public static final int TRACER_ADVANCED = 0x3A9BFF;
    public static final int TRACER_ROCKET = 0xFFB43A;
    public static final int TRACER_NDR = 0xB44DFF;

    public record Preset(int muzzleColor, float flashScale, int tracerColor,
                         int smokeCount, int flareColor, float lightRadius, int muzzleTexture) {}

    public static Preset forKind(GunStats.ProjectileKind kind, float muzzleFlashScale) {
        return switch (kind) {
            case ELECTRIC -> new Preset(MuzzleFlashEntity.CYAN, 0.42f, TRACER_TESLA, 0, TRACER_TESLA, 7.0f, GunExtras.FLASH_LIGHTNING);
            case BIO -> new Preset(TRACER_BIO, 0.34f + 0.10f * muzzleFlashScale, TRACER_BIO, 1, TRACER_BIO, 5.0f, GunExtras.FLASH_GREENFLARE);
            case FIRE -> new Preset(TRACER_FIRE, 0.38f + 0.12f * muzzleFlashScale, TRACER_FIRE, 1, TRACER_FIRE, 7.0f, GunExtras.FLASH_FLAME);
            case PLASMA -> new Preset(TRACER_TFG, 0.55f, TRACER_TFG, 0, TRACER_TFG, 9.0f, GunExtras.FLASH_TFG);
            case LASER, BEAM -> new Preset(TRACER_LASER, 0.45f, TRACER_LASER, 0, TRACER_LASER, 7.0f, GunExtras.FLASH_LASER);
            case SONIC -> new Preset(TRACER_SONIC, 0.50f, TRACER_SONIC, 0, TRACER_SONIC, 6.0f, GunExtras.FLASH_SONIC);
            case GAUSS, ADVANCED -> new Preset(TRACER_GAUSS, 0.45f, TRACER_GAUSS, 1, TRACER_GAUSS, 7.0f, GunExtras.FLASH_BLUE);
            case EXPLOSIVE -> new Preset(TRACER_ROCKET, 0.55f, TRACER_ROCKET, 2, TRACER_ROCKET, 8.0f, GunExtras.FLASH_GUN);
            default -> new Preset(MuzzleFlashEntity.WARM, 0.30f + 0.10f * muzzleFlashScale, TRACER_WARM, 1, TRACER_WARM, 6.0f, GunExtras.FLASH_RIFLE);
        };
    }

    /** CE muzzle texture: per-gun override wins, otherwise the kind default above. */
    public static int muzzleTexture(GunStats stats, com.techguns.techguns3.item.GunExtras extras) {
        if (extras.muzzleFlash() != com.techguns.techguns3.item.GunExtras.FLASH_AUTO) {
            return extras.muzzleFlash();
        }
        return forKind(stats.projectileKind(), stats.muzzleFlashScale()).muzzleTexture();
    }
}
