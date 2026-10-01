package com.techguns.techguns3.damagesystem;

/**
 * How a Techguns kill may end the victim. Port of the 1.12
 * {@code techguns.deatheffects.EntityDeathUtils.DeathType}.
 *
 * <p>Wave 1 implements {@link #GORE} only (meat + blood). {@link #BIO} and
 * {@link #LASER} are reserved for the biogun desolve and the tesla/laser
 * disintegration passes: same handoff, different client show
 * (bubbling slime pile / ash + glow instead of chunks).</p>
 */
public enum DeathType {
    /** Vanilla death, no gore. */
    DEFAULT,
    /** Body bursts into flying chunks plus a blood fountain. */
    GORE,
    /** Reserved: acid desolve (biogun). */
    BIO,
    /** Reserved: energy disintegration (tesla/laser). */
    LASER
}
