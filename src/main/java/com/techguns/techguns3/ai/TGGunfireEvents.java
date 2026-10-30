package com.techguns.techguns3.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Recent-gunfire registry for the hearing AI: {@code BallisticCombat.fire}
 * reports every shot (players, NPCs, turrets), and {@link TGHearingGoal}
 * polls it. Ring buffer, capped, self-expiring by game time.
 */
public final class TGGunfireEvents {
    private static final int MAX_ENTRIES = 96;
    private static final long EXPIRY_TICKS = 20;

    /** One reported shot; holds the shooter reference for its ~1s lifetime. */
    public record Shot(ServerLevel level, Vec3 pos, LivingEntity shooter, long tick) {}

    private static final ArrayDeque<Shot> RECENT = new ArrayDeque<>();

    private TGGunfireEvents() {}

    /** Called by every server-side gun fire path. */
    public static void emit(ServerLevel level, Vec3 pos, LivingEntity shooter) {
        Shot shot = new Shot(level, pos, shooter, level.getGameTime());
        synchronized (RECENT) {
            RECENT.addLast(shot);
            while (RECENT.size() > MAX_ENTRIES) {
                RECENT.pollFirst();
            }
        }
    }

    /** Shots in {@code radius} around {@code ear} that are at most 1 second old. */
    public static List<Shot> recent(ServerLevel level, Vec3 ear, double radius) {
        long now = level.getGameTime();
        double radiusSq = radius * radius;
        List<Shot> out = new ArrayList<>();
        synchronized (RECENT) {
            for (Shot shot : RECENT) {
                if (shot.level() != level || now - shot.tick() > EXPIRY_TICKS) continue;
                if (shot.pos().distanceToSqr(ear) <= radiusSq) {
                    out.add(shot);
                }
            }
        }
        return out;
    }
}
