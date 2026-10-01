package com.techguns.techguns3.damagesystem;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.fx.FxParticles;
import com.techguns.techguns3.registry.TGEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Server-side radiation damage (TGRadiationSystem, simplified): every 40 ticks
 * a radiated entity takes 1.0 (amplifier 0) or 2.0 (amplifier 1+) magic damage
 * plus a green glint. Creative/spectator entities are skipped.
 */
@EventBusSubscriber(modid = TechGuns3.MODID)
public final class RadiationHandler {
    private RadiationHandler() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel level)) return;
        var instance = living.getEffect(TGEffects.RADIATION);
        if (instance == null) return;
        if (living.isSpectator()) return;
        if (living instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return;
        if (living.tickCount % 40 != 0) return;
        float dmg = instance.getAmplifier() >= 1 ? 2.0f : 1.0f;
        living.hurtServer(level, level.damageSources().magic(), dmg);
        var at = living.position().add(0, living.getBbHeight() * 0.6, 0);
        FxParticles.glow(level, at.x, at.y, at.z, 2,
                0x39FF5E, 0x1A7A2E, 0.12f, 0.03f, 14, 0.0f, 0.96f, 0.6, 1.2);
    }
}
