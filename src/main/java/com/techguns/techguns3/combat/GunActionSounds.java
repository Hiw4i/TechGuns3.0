package com.techguns.techguns3.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Delayed gun action sounds (shotgun pump, bolt rechamber). TG2 plays these
 * after the shot, not layered onto it; a tiny server-tick queue reproduces
 * that without per-gun tick state. Bounded and self-cleaning: entries for
 * unloaded dimensions expire instead of leaking.
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID)
public final class GunActionSounds {
    private GunActionSounds() {}

    private record Entry(ServerLevel level, Vec3 pos, Supplier<SoundEvent> sound,
                         SoundSource source, long dueTick) {}

    private static final int MAX_PENDING = 128;
    private static final Deque<Entry> PENDING = new ArrayDeque<>();

    public static void schedule(ServerLevel level, Vec3 pos, Supplier<SoundEvent> sound,
                                SoundSource source, int delayTicks) {
        if (sound == null || delayTicks < 0) return;
        if (PENDING.size() >= MAX_PENDING) PENDING.pollFirst();
        PENDING.addLast(new Entry(level, pos, sound, source, level.getGameTime() + delayTicks));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        var server = event.getServer();
        while (!PENDING.isEmpty()) {
            Entry head = PENDING.peekFirst();
            ServerLevel level = head.level();
            // Drop entries whose dimension went away or that rotted (server lag safety).
            if (server.getLevel(level.dimension()) != level || level.getGameTime() > head.dueTick() + 200) {
                PENDING.pollFirst();
                continue;
            }
            if (level.getGameTime() < head.dueTick()) break;
            PENDING.pollFirst();
            try {
                level.playSound(null, head.pos().x, head.pos().y, head.pos().z,
                        head.sound().get(), head.source(), 1.0f, 1.0f);
            } catch (Exception e) {
                com.techguns.techguns3.TechGuns3.LOGGER.debug(
                        "[TechGuns3] delayed action sound failed", e);
            }
        }
    }
}
