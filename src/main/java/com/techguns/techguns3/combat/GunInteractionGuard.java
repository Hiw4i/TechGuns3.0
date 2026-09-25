package com.techguns.techguns3.combat;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Makes LMB mean "fire" while a gun is held. Cancels the vanilla consequences
 * (melee damage, block mining) on both logical sides; the actual shot is driven
 * by {@link GunServerLogic} from client intents.
 */
@EventBusSubscriber(modid = TechGuns3.MODID)
public final class GunInteractionGuard {
    private GunInteractionGuard() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof GenericGunItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof GenericGunItem) {
            event.setCanceled(true);
        }
    }
}
