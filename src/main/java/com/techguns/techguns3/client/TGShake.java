package com.techguns.techguns3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Modern cinematic juice, all client-side and visual-only:
 * <ul>
 *   <li>Trauma camera shake (roll + pitch/yaw jitter ∝ trauma², decays fast).</li>
 *   <li>Explosion flash: white fullscreen fade when a big blast goes off nearby.</li>
 * </ul>
 * Trauma sources: firing heavy guns (local kick path) and first-seen
 * {@code BlastEntity} detonations scaled by radius/distance. Nothing here
 * touches gameplay or networking.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class TGShake {
    private TGShake() {}

    private static float trauma;
    private static float flash;
    private static final Set<UUID> SEEN_BLASTS = new HashSet<>();

    /** 0..1 trauma bump (clamped). */
    public static void addTrauma(float amount) {
        trauma = Mth.clamp(trauma + amount, 0.0f, 1.0f);
    }

    /** Fullscreen flash 0..1 (decays on its own). */
    public static void addFlash(float amount) {
        flash = Mth.clamp(flash + amount, 0.0f, 1.0f);
    }

    public static float flash() {
        return flash;
    }

    /**
     * Called once per blast entity when first submitted: distance-scaled shake
     * + flash so nukes thump the screen and firecrackers don't.
     */
    public static void onBlastSeen(UUID id, double x, double y, double z, float radius) {
        if (!SEEN_BLASTS.add(id)) return;
        if (SEEN_BLASTS.size() > 128) SEEN_BLASTS.clear();
        try {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var eye = mc.player.getEyePosition();
            double dx = eye.x - x;
            double dy = eye.y - y;
            double dz = eye.z - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double reach = 6.0 + radius * 6.0;
            if (dist > reach) return;
            float k = (float) (1.0 - dist / reach);
            addTrauma(k * k * Math.min(1.0f, 0.35f + radius * 0.08f));
            if (radius >= 4.0f) {
                addFlash(k * Math.min(0.8f, radius * 0.06f));
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // ~1.6/s decay, frame-rate independent enough at fixed client ticks.
        trauma = Math.max(0.0f, trauma - 0.032f);
        flash = Math.max(0.0f, flash - 0.06f);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (trauma <= 0.003f) return;
        float t = trauma * trauma;
        long time = System.nanoTime() / 1_000_000L;
        float n1 = noise(time * 0.043f) * t * 3.2f;
        float n2 = noise(time * 0.037f + 40.0f) * t * 3.2f;
        float n3 = noise(time * 0.031f + 90.0f) * t * 2.4f;
        event.setRoll(event.getRoll() + n3);
        event.setYaw(event.getYaw() + n1);
        event.setPitch(event.getPitch() + n2);
    }

    /** Cheap smooth pseudo-noise in [-1, 1]. */
    private static float noise(double t) {
        return (float) (Math.sin(t) * 0.55 + Math.sin(t * 2.13 + 1.7) * 0.3 + Math.sin(t * 4.7 + 4.2) * 0.15);
    }
}
