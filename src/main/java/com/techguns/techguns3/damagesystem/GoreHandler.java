package com.techguns.techguns3.damagesystem;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.entity.FlyingGibEntity;
import com.techguns.techguns3.fx.BloodFx;
import com.techguns.techguns3.registry.TGSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * GORE deaths for Techguns kills. Port of the 1.12 chain
 * ({@code TGEventHandler.onLivingDeathEvent} →
 * {@code PacketEntityDeathType} → {@code DeathEffect.createDeathEffect}):
 * the victim bursts into {@link FlyingGibEntity} chunks plus a dense blood
 * fountain, with the vanilla corpse left to fall underneath.
 *
 * <p>No event subscription on purpose: the guns call {@link #onKill} directly
 * right after dealing lethal damage — same tick, same side, zero chances for
 * a missed event or a stale handoff. Deaths from other causes (fall, poison
 * ticks, fire after the shot) never gib, exactly like aimed meat shots should.</p>
 */
public final class GoreHandler {
    private GoreHandler() {}

    /** Blood color resolved from the victim unless a gun overrides it (bio ichor). */
    public static final int USE_VICTIM_COLOR = Integer.MIN_VALUE;

    /** Gib roll for plain ballistic rounds: every kill bursts, no lottery. */
    public static final float BULLET_GORE_CHANCE = 1.0f;
    /** Tesla shreds every kill too. */
    public static final float TESLA_GORE_CHANCE = 1.0f;

    /**
     * A Techguns hit just killed {@code victim}. Always a dense blood explosion;
     * flying chunks on the {@code goreChance} roll (scaled by config).
     */
    public static void onKill(ServerLevel level, LivingEntity victim, Vec3 killDir,
                             float goreChance, int bloodOverride) {
        if (!TGConfig.goreEnabled()) return;
        int bloodColor = bloodOverride != USE_VICTIM_COLOR ? bloodOverride : GoreRegistry.bloodColor(victim);
        Vec3 center = victim.position().add(0, victim.getBbHeight() * 0.55, 0);
        BloodFx.deathBurst(level, center, bloodColor);
        level.playSound(null, center.x, center.y, center.z, TGSounds.FLESH_IMPACT.get(),
                SoundSource.HOSTILE, 1.2f, 0.7f + level.getRandom().nextFloat() * 0.2f);

        if (bloodColor == GoreRegistry.NO_BLOOD) return;
        if (level.getRandom().nextFloat() >= goreChance * (float) TGConfig.goreChanceMultiplier()) return;
        int count = Math.min(TGConfig.maxGibs(), GoreRegistry.gibCount(victim));
        if (count <= 0) return;
        float scale = GoreRegistry.gibScale(victim);
        // Torso and head first: the two biggest reads of "it came apart".
        int[] order = {1, 0, 2, 3, 4, 5};
        Vec3 d = killDir.lengthSqr() < 1e-6 ? new Vec3(0, 0.4, 0) : killDir.normalize();
        var random = level.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 pos = center.add((random.nextDouble() * 2 - 1) * 0.3,
                    (random.nextDouble() * 2 - 1) * 0.25, (random.nextDouble() * 2 - 1) * 0.3);
            // Tight pile around the corpse: short lob, not a long throw.
            Vec3 vel = new Vec3(
                    d.x * (0.2 + random.nextDouble() * 0.3) + (random.nextDouble() * 2 - 1) * 0.5,
                    0.4 + random.nextDouble() * 0.4,
                    d.z * (0.2 + random.nextDouble() * 0.3) + (random.nextDouble() * 2 - 1) * 0.5);
            float variance = 0.85f + random.nextFloat() * 0.3f;
            level.addFreshEntity(new FlyingGibEntity(level, pos, vel,
                    order[i % order.length], bloodColor, scale * variance));
        }
        level.playSound(null, center.x, center.y, center.z, TGSounds.GORE_SPLIT.get(),
                SoundSource.HOSTILE, 1.3f, 0.85f + random.nextFloat() * 0.3f);
    }
}
