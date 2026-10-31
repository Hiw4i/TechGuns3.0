package com.techguns.techguns3.combat;

import com.geckolib.animatable.GeoItem;
import com.geckolib.cache.SyncedSingletonAnimatableCache;
import com.geckolib.network.packet.singleton.SingletonAnimTriggerPacket;
import com.techguns.techguns3.item.AnimatedGunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * View-model animation sync.
 *
 * <p>GeckoLib's own {@code triggerAnim} broadcasts to the shooter as well, so
 * the weapon animation always arrived one round trip after the shot. Here the
 * shooter is excluded from the broadcast and predicts the animation locally
 * instead, which is what makes a rapid fire weapon feel connected to the
 * trigger. Everyone else still gets the authoritative server trigger.</p>
 */
public final class GunAnimSync {
    private GunAnimSync() {}

    /** Plays an animation on every client that watches the shooter. */
    public static void triggerForOthers(ServerPlayer player, ItemStack stack,
                                        AnimatedGunItem gun, String controller, String anim) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        long instanceId = GeoItem.getOrAssignId(stack, server);
        String syncableId = SyncedSingletonAnimatableCache.getOrCreateId(gun);
        PacketDistributor.sendToPlayersTrackingEntity(player, new SingletonAnimTriggerPacket(
                syncableId, instanceId, Optional.of(controller), anim));
    }

    /**
     * Plays an animation on the local view model right away.
     *
     * <p>Uses the same instance id the renderer resolves, so the animation
     * lands on the manager that is actually being drawn.</p>
     */
    public static void triggerLocal(ItemStack stack, String controller, String anim) {
        if (!(stack.getItem() instanceof AnimatedGunItem gun)) {
            return;
        }
        // TEMP DIAGNOSTIC: every local animation trigger, so a stray replay of
        // the draw track cannot hide behind the weapon-switch guard.
        com.techguns.techguns3.TechGuns3.LOGGER.debug(
                "[TechGuns3] anim trigger local: {} / {} (id {})", controller, anim, GeoItem.getId(stack));
        long instanceId = GeoItem.getId(stack);
        gun.getAnimatableInstanceCache().getManagerForId(instanceId)
                .tryTriggerAnimation(controller, anim);
    }
}
