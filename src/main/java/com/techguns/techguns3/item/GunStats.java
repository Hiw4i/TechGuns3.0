package com.techguns.techguns3.item;

import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * Ballistic tunables of a Techguns firearm, ported from the 1.12
 * {@code techguns.items.guns.GenericGun(...)} constructor args so balance survives the port.
 *
 * <p>1.12 mapping:
 * <ul>
 *   <li>{@code baseDamage} = {@code damage}, {@code fireCooldownTicks} = {@code minFiretime},</li>
 *   <li>{@code magazineSize} = {@code clipsize}, {@code reloadTimeTicks} = {@code reloadtime},</li>
 *   <li>{@code maxRange} = {@code TTL} (ticks-to-live; travel distance ≈ TTL × {@code bulletSpeed}),</li>
 *   <li>{@code spread} = {@code accuracy}, {@code bulletSpeed} = {@code speed},</li>
 *   <li>{@code pellets} = 1 + shotgun {@code bulletcount}, {@code pelletSpread} = shotgun {@code spread},</li>
 *   <li>damage drop = {@code setDamageDrop(start, end, min)}, {@code penetration} as-is,</li>
 *   <li>{@code semiAuto} as-is, {@code magazineFed} = true when the 1.12 ammo was a magazine item.</li>
 * </ul>
 * Modern additions (data-driven, replace 1.12 hardcoded renderer values):
 * zoom FOV + spread bonus, per-shot camera recoil and muzzle-flash scale,
 * minigun-style spinning barrels flag.</p>
 */
public record GunStats(
        float baseDamage,
        int fireCooldownTicks,
        int magazineSize,
        int reloadTimeTicks,
        int maxRange,
        float spread,
        float bulletSpeed,
        double gravity,
        int pellets,
        float pelletSpread,
        float dropStart,
        float dropEnd,
        float dropMin,
        float penetration,
        boolean semiAuto,
        boolean magazineFed,
        Supplier<SoundEvent> fireSound,
        Supplier<SoundEvent> reloadSound,
        Supplier<SoundEvent> extraSound,
        ProjectileKind projectileKind,
        // --- modern presentation / feel (data-driven, was hardcoded in 1.12 renderers) ---
        boolean canZoom,
        float zoomFov,
        float zoomSpreadMult,
        float recoilPitchDeg,
        float recoilYawJitter,
        float muzzleFlashScale,
        boolean spinsBarrels
) {
    public enum ProjectileKind { BALLISTIC, ELECTRIC }

    public GunStats(float baseDamage, int fireCooldownTicks, int magazineSize, int reloadTimeTicks,
                    int maxRange, float spread, float bulletSpeed, double gravity, int pellets,
                    float pelletSpread, float dropStart, float dropEnd, float dropMin,
                    float penetration, boolean semiAuto, boolean magazineFed,
                    Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound,
                    Supplier<SoundEvent> extraSound) {
        this(baseDamage, fireCooldownTicks, magazineSize, reloadTimeTicks, maxRange, spread,
                bulletSpeed, gravity, pellets, pelletSpread, dropStart, dropEnd, dropMin,
                penetration, semiAuto, magazineFed, fireSound, reloadSound, extraSound,
                ProjectileKind.BALLISTIC, true, 0.8f, 0.5f, 0.8f, 0.4f, 0.7f, false);
    }
}
