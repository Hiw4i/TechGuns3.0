package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Status effects. Wave-2 adds radiation (TGRadiationSystem): green harmful
 * effect applied by the Nuclear Death Ray and nuke explosions. Damage itself
 * is dealt by {@link com.techguns.techguns3.damagesystem.RadiationHandler}
 * so the effect stays a plain marker (no risky MobEffect overrides).
 */
public final class TGEffects {
    private TGEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, TechGuns3.MODID);

    /** Sickly green harmful effect (color 0x39FF5E, like the TFG glow). */
    public static final DeferredHolder<MobEffect, MobEffect> RADIATION =
            EFFECTS.register("radiation", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x39FF5E) {});
}
