package com.techguns.techguns3.combat;

import com.techguns.techguns3.network.GunPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells the shooter that its shot hit something, so the client can show the
 * hit marker. Only the player that fired is told, it is a personal feedback
 * and not part of the game state.
 */
public final class HitFeedback {
    private HitFeedback() {}

    public static void hit(ServerLevel level, LivingEntity shooter, LivingEntity victim) {
        if (shooter instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new GunPackets.HitConfirm(!victim.isAlive()));
        }
    }
}
