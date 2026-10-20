package com.techguns.techguns3.combat;

import com.techguns.techguns3.network.GunPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells the shooter that its shot hit something, so the client can show the
 * hit marker. Only the player that fired is told, it is a personal feedback
 * and not part of the game state.
 *
 * <p>A shotgun sends a dozen pellets, and every pellet that connects would
 * send a packet. The marker is a single flash on screen, so the notices are
 * rate limited per shooter: a volley produces one packet, an automatic still
 * produces one per shot.</p>
 */
public final class HitFeedback {
    private HitFeedback() {}

    /** Minimum time between two hit notices of the same shooter. */
    private static final long MIN_INTERVAL_NANOS = 45_000_000L;

    private static final Map<UUID, Long> LAST_NOTICE = new ConcurrentHashMap<>();

    public static void hit(ServerLevel level, LivingEntity shooter, LivingEntity victim) {
        if (!(shooter instanceof ServerPlayer player)) {
            return;
        }
        long now = System.nanoTime();
        UUID id = player.getUUID();
        Long last = LAST_NOTICE.get(id);
        if (last != null && now - last < MIN_INTERVAL_NANOS) {
            return;
        }
        if (LAST_NOTICE.size() > 256) {
            LAST_NOTICE.clear();
        }
        LAST_NOTICE.put(id, now);
        PacketDistributor.sendToPlayer(player, new GunPackets.HitConfirm(!victim.isAlive()));
    }
}
