package com.techguns.techguns3.item;

/**
 * Wave-2 exotic mechanics, ported from Techguns2-CE gun classes.
 *
 * <p>Kept out of {@link GunStats} so the 11 wave-1 guns never change:
 * {@code GenericGunItem} carries an optional {@code extras} (default = plain
 * ballistic). Mappings:
 * <ul>
 *   <li>TFG / rockets / guided / AS50-explosive → {@code explosive} (TGExplosion params).</li>
 *   <li>NDR / lasergun / laserpistol → {@code beam} hitscan (AbstractBeamProjectile.trace).</li>
 *   <li>TFG → {@code chargeTicks} (GenericGunCharge.fullChargeTime) + ammo 1..5.</li>
 *   <li>Sonic → knockback + ignoreIframes + 16 pellets (in stats).</li>
 *   <li>Guided / Grim → {@code guided} lock-on (GuidedMissileLauncher 20/80/150).</li>
 *   <li>NDR / nuke → {@code radiationTicks} (TGRadiationSystem).</li>
 *   <li>Rockets / AS50 / gauss → {@code extraAmmoIds} variant priority.</li>
 * </ul>
 */
public record GunExtras(
        ExplosiveSpec explosive,
        boolean beam,
        int chargeTicks,
        int ammoBase,
        int ammoFull,
        float knockback,
        boolean ignoreIframes,
        float homingTurn,
        boolean guided,
        int lockTime,
        int lockPersist,
        double lockRange,
        int radiationTicks,
        String[] extraAmmoIds,
        // --- wave-2 visuals (1:1 from ClientProxy/ScreenEffect) ---
        int muzzleFlash,
        int loopTicks,
        // --- NDR: true continuous beam (damage every tick while held, like
        // NDRProjectile which teleports to the shooter and traces each tick) ---
        boolean continuousBeam
) {
    /** TG2 explosion tunables: max/min damage, inner/outer radius, block damage 0..1, nuke flag. */
    public record ExplosiveSpec(float max, float min, float r1, float r2, float blockDamage, boolean nuke) {}

    /** Muzzle-flash texture ids (ScreenEffect↔renderer map). 0 = kind default. */
    public static final int FLASH_AUTO = 0;
    public static final int FLASH_RIFLE = 1;
    public static final int FLASH_GUN = 2;
    public static final int FLASH_MINIGUN = 3;
    public static final int FLASH_BLUE = 4;
    public static final int FLASH_LASER = 5;
    public static final int FLASH_LIGHTNING = 6;
    public static final int FLASH_SONIC = 7;
    public static final int FLASH_NUKEBEAM = 8;
    public static final int FLASH_TFG = 9;
    public static final int FLASH_FLAME = 10;
    public static final int FLASH_GREENFLARE = 11;

    public static final GunExtras DEFAULT = new GunExtras(
            null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0, null,
            FLASH_AUTO, 0, false);

    public static GunExtras explosive(float max, float min, float r1, float r2, float blockDamage) {
        return new GunExtras(new ExplosiveSpec(max, min, r1, r2, blockDamage, false),
                false, 0, 1, 1, 1.0f, false, 0.0f, false, 20, 80, 150.0, 0, null,
                FLASH_AUTO, 0, false);
    }

    public static GunExtras nuke(float max, float min, float r1, float r2) {
        return new GunExtras(new ExplosiveSpec(max, min, r1, r2, 0.5f, true),
                false, 0, 1, 1, 3.0f, false, 0.0f, false, 20, 80, 150.0, 1200, null,
                FLASH_AUTO, 0, false);
    }

    /** Charged gun (TFG): full charge after {@code chargeTicks}, ammo use scales base..full. */
    public GunExtras withCharge(int ticks, int base, int full) {
        return new GunExtras(explosive, beam, ticks, base, full, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withBeam(int radiation) {
        return new GunExtras(explosive, true, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiation, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withKnockback(float kb, boolean ignoreIframes) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, kb, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withGuided(float turn, int lock, int persist, double range) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                turn, true, lock, persist, range, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withHoming(float turn) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                turn, false, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withRadiation(int ticks) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, ticks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withExtraAmmo(String... ids) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, ids,
                muzzleFlash, loopTicks, continuousBeam);
    }

    public GunExtras withExplosive(ExplosiveSpec spec) {
        return new GunExtras(spec, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, continuousBeam);
    }

    /** CE muzzle texture override (ScreenEffect id above). */
    public GunExtras withMuzzle(int flash) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                flash, loopTicks, continuousBeam);
    }

    /** Looping fire sound while held (flamethrower/NDR MaxLoopDelay): replay every N ticks. */
    public GunExtras withLoop(int ticks) {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, ticks, continuousBeam);
    }

    public boolean hasExplosive() { return explosive != null; }
    public boolean hasCharge() { return chargeTicks > 0; }
    public boolean hasExtraAmmo() { return extraAmmoIds != null && extraAmmoIds.length > 0; }
    public boolean hasLoop() { return loopTicks > 0; }

    /** NDR-style sustained beam: damage every tick while held. */
    public GunExtras withContinuous() {
        return new GunExtras(explosive, beam, chargeTicks, ammoBase, ammoFull, knockback, ignoreIframes,
                homingTurn, guided, lockTime, lockPersist, lockRange, radiationTicks, extraAmmoIds,
                muzzleFlash, loopTicks, true);
    }
}
