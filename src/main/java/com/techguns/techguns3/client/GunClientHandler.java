package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.network.GunPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.techguns.techguns3.TechGuns3;

/**
 * Client-side gun input. Converts vanilla buttons into gun intents:
 * <ul>
 *   <li>LMB (attack key) with a gun held = fire. Semi-auto fires on the rising edge,
 *       automatics send start/stop so the server can hold the trigger.</li>
 *   <li>RMB (use key) with a gun held = aim/zoom, mirrored to the server for the
 *       spread bonus.</li>
 *   <li>R = reload request.</li>
 * </ul>
 * Also applies instant local feedback (recoil kick + muzzle particles) so firing
 * feels responsive despite server authority. Damage and ammo stay server-side.
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class GunClientHandler {
    private GunClientHandler() {}

    private static boolean lmbDown;
    private static boolean zoomSent;
    private static long lastAutoFeedbackTick;
    private static ItemStack lastGun = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onAttackMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.player.getMainHandItem().getItem() instanceof GenericGunItem) {
            // Suppress the vanilla swing/melee pipeline; ClientTick polls keyAttack for firing.
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) {
            if (player != null) {
                lmbDown = false;
                syncZoom(player, false);
            } else {
                lmbDown = false;
                zoomSent = false;
            }
            return;
        }
        if (player.isSpectator() || !player.isAlive()) {
            lmbDown = false;
            syncZoom(player, false);
            return;
        }
        ItemStack held = player.getMainHandItem();
        boolean holding = held.getItem() instanceof GenericGunItem gun;

        // Weapon switch invalidates hold state on both sides. Compare the item only:
        // every shot mutates the AMMO component, which must NOT look like a switch.
        if (held.getItem() != lastGun.getItem()) {
            lastGun = held.copy();
            if (lmbDown) {
                lmbDown = false;
                ClientPacketDistributor.sendToServer(new GunPackets.FireStop(true));
            }
            syncZoom(player, false);
        }
        if (!holding) {
            lmbDown = false;
            syncZoom(player, false);
            return;
        }

        // Reload key (edge-triggered by vanilla).
        if (TGKeyMappings.reload().consumeClick()) {
            ClientPacketDistributor.sendToServer(new GunPackets.ReloadRequest(true));
        }

        boolean zooming = player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem().getItem() instanceof GenericGunItem;
        syncZoom(player, zooming);

        boolean down = mc.options.keyAttack.isDown();
        if (down != lmbDown) {
            lmbDown = down;
            if (down) {
                onLmbPressed(player, held, (GenericGunItem) held.getItem(), zooming);
            } else {
                ClientPacketDistributor.sendToServer(new GunPackets.FireStop(true));
            }
            return;
        }
        // Held trigger on an automatic: continuous local kick/flash at the gun's own rate.
        // Damage still comes from the server; this is visuals only.
        if (down && !((GenericGunItem) held.getItem()).stats().semiAuto()) {
            long now = player.level().getGameTime();
            long period = Math.max(1, ((GenericGunItem) held.getItem()).stats().fireCooldownTicks());
            if (now - lastAutoFeedbackTick >= period) {
                lastAutoFeedbackTick = now;
                predictShotFeedback(player, (GenericGunItem) held.getItem());
            }
        }
    }

    private static void onLmbPressed(LocalPlayer player, ItemStack held, GenericGunItem gun, boolean zooming) {
        if (gun.stats().semiAuto()) {
            // Rate-limit + reload-lock client-side so dead clicks give no phantom kick.
            if (player.getCooldowns().isOnCooldown(held)) return;
            ClientPacketDistributor.sendToServer(new GunPackets.FirePressed(true, zooming));
            predictShotFeedback(player, gun);
        } else {
            ClientPacketDistributor.sendToServer(new GunPackets.FireStart(true, zooming));
            lastAutoFeedbackTick = player.level().getGameTime();
            predictShotFeedback(player, gun);
        }
    }

    private static void syncZoom(LocalPlayer player, boolean zooming) {
        if (zooming == zoomSent) return;
        zoomSent = zooming;
        ClientPacketDistributor.sendToServer(new GunPackets.ZoomState(zooming));
    }

    /** Instant local kick so the shot lands physically before the server echo.
     * Flash/tracer visuals arrive with the server's FX entities (~1 tick). */
    private static void predictShotFeedback(LocalPlayer player, GenericGunItem gun) {
        if (TGConfig.cameraRecoil()) {
            float yawJitter = (player.getRandom().nextFloat() * 2.0f - 1.0f) * gun.stats().recoilYawJitter();
            // Negative pitch kicks the muzzle up (XRot decreases looking up).
            player.turn(yawJitter, -gun.stats().recoilPitchDeg());
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!(event.getPlayer() instanceof LocalPlayer player)) return;
        if (!player.isUsingItem()) return;
        ItemStack using = player.getUseItem();
        if (using.getItem() instanceof GenericGunItem gun
                && gun.stats().canZoom()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            event.setNewFovModifier(event.getFovModifier() * gun.stats().zoomFov());
        }
    }
}
