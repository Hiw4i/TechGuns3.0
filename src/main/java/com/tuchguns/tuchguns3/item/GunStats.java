package com.tuchguns.tuchguns3.item;

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
 *   <li>{@code maxRange} = {@code TTL} (ticks-to-live; actual range ≈ TTL blocks at {@code bulletSpeed} b/t),</li>
 *   <li>{@code spread} = {@code accuracy}, {@code bulletSpeed} = {@code speed},</li>
 *   <li>{@code pellets} = 1 + shotgun {@code bulletcount}, {@code pelletSpread} = shotgun {@code spread},</li>
 *   <li>damage drop = {@code setDamageDrop(start, end, min)}, {@code penetration} as-is,</li>
 *   <li>{@code semiAuto} as-is, {@code magazineFed} = true when the 1.12 ammo was a magazine item.</li>
 * </ul>
 * Zoom, recoil spring, muzzle flash, AI stats and camo variants are phase-3+ work.</p>
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
        Supplier<SoundEvent> extraSound
) {}
