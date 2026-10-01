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
    /** Client game-time when the current charge hold started (-1 = not charging). */
    private static long chargeStartTick = -1;
    /** Item id being charged (charge belongs to one gun; switching cancels). */
    private static net.minecraft.resources.Identifier chargeGunId;

    /**
     * Local charge progress 0..1 for the charge HUD (-1 when not charging).
     * Mirrors the server's charge math from our own FireStart timestamp.
     */
    public static float chargeProgress(ItemStack held) {
        if (chargeStartTick < 0 || held.isEmpty()) return -1.0f;
        if (!(held.getItem() instanceof GenericGunItem gun)) return -1.0f;
        var extras = gun.extras();
        if (!extras.hasCharge()) return -1.0f;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
        if (!id.equals(chargeGunId)) return -1.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return -1.0f;
        long now = mc.level.getGameTime();
        return Math.min(1.0f, (now - chargeStartTick) / (float) Math.max(1, extras.chargeTicks()));
    }

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
            chargeStartTick = -1;
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
                chargeStartTick = -1;
                ClientPacketDistributor.sendToServer(new GunPackets.FireStop(true));
            }
            return;
        }
        // Held trigger on an automatic: continuous local kick/flash at the gun's own rate.
        // Damage still comes from the server; this is visuals only.
        // Sustained beams (NDR) get a gentle continuous tremor instead of kicks.
        if (down && !((GenericGunItem) held.getItem()).stats().semiAuto()) {
            GenericGunItem gun = (GenericGunItem) held.getItem();
            var extras = gun.extras();
            if (extras.continuousBeam()) {
                if (TGConfig.cameraRecoil()) {
                    float jx = (player.getRandom().nextFloat() * 2.0f - 1.0f) * 0.12f;
                    float jy = (player.getRandom().nextFloat() * 2.0f - 1.0f) * 0.12f;
                    player.turn(jx, jy);
                }
            } else {
                long now = player.level().getGameTime();
                // Hoisted: stats() builds a fresh record per call.
                var stats = gun.stats();
                long period = Math.max(1, stats.fireCooldownTicks());
                if (now - lastAutoFeedbackTick >= period) {
                    lastAutoFeedbackTick = now;
                    predictShotFeedback(player, gun, stats, extras);
                }
            }
        }
    }

    private static void onLmbPressed(LocalPlayer player, ItemStack held, GenericGunItem gun, boolean zooming) {
        var extras = gun.extras();
        if (extras.hasCharge() && !extras.guided()) {
            chargeStartTick = player.level().getGameTime();
            chargeGunId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
        }
        var stats = gun.stats();
        if (stats.semiAuto()) {
            // Rate-limit + reload-lock client-side so dead clicks give no phantom kick.
            if (player.getCooldowns().isOnCooldown(held)) return;
            ClientPacketDistributor.sendToServer(new GunPackets.FirePressed(true, zooming));
            predictShotFeedback(player, gun, stats, extras);
        } else {
            ClientPacketDistributor.sendToServer(new GunPackets.FireStart(true, zooming));
            lastAutoFeedbackTick = player.level().getGameTime();
            // Sustained beams start silently; the hold tick owns the tremor.
            if (!extras.continuousBeam()) {
                predictShotFeedback(player, gun, stats, extras);
            }
        }
    }

    private static void syncZoom(LocalPlayer player, boolean zooming) {
        if (zooming == zoomSent) return;
        zoomSent = zooming;
        ClientPacketDistributor.sendToServer(new GunPackets.ZoomState(zooming));
    }

    private static long lastShotNanos;

    /**
     * Instant local kick + baked muzzle flare so the shot lands physically
     * before the server echo. Server entities (~1 tick later) confirm it for
     * third person and other viewers; this predict is deliberately compact so
     * it can never become the head-sized blob.
     *
     * <p>Takes pre-resolved stats/extras: both build fresh records per call.</p>
     */
    private static void predictShotFeedback(LocalPlayer player, GenericGunItem gun,
                                            com.techguns.techguns3.item.GunStats stats,
                                            com.techguns.techguns3.item.GunExtras extras) {
        if (TGConfig.cameraRecoil()) {
            float yawJitter = (player.getRandom().nextFloat() * 2.0f - 1.0f) * stats.recoilYawJitter();
            // Negative pitch kicks the muzzle up (XRot decreases looking up).
            player.turn(yawJitter, -stats.recoilPitchDeg());
            // Heavy guns thump the camera (decays in TGShake).
            TGShake.addTrauma(Math.min(0.45f, stats.recoilPitchDeg() * 0.12f));
        }
        lastShotNanos = System.nanoTime();
        if (!TGConfig.cinematicFx()) return;
        try {
            // Same barrel tip the server fires from (single source of truth).
            var tip = com.techguns.techguns3.combat.GunServerLogic.muzzlePos(player);
            var look = player.getLookAngle();
            var level = player.level();
            var preset = com.techguns.techguns3.fx.GunFxPresets.forKind(
                    stats.projectileKind(), stats.muzzleFlashScale());
            if (stats.projectileKind()
                    == com.techguns.techguns3.item.GunStats.ProjectileKind.FIRE) {
                // Flamethrower: tongues of textured flame blown forward, no smoke puff.
                var rnd = player.getRandom();
                for (int i = 0; i < 5; i++) {
                    level.addParticle(new com.techguns.techguns3.registry.TGParticles.FlameOptions(
                                    0xFFE9A8, 0xFF5A1A, 0.30f, 0.08f, 14, -0.015f, 0.94f),
                            tip.x + look.x * 0.5, tip.y + look.y * 0.5, tip.z + look.z * 0.5,
                            look.x * 2.2 + (rnd.nextDouble() - 0.5) * 0.7,
                            look.y * 2.2 + (rnd.nextDouble() - 0.5) * 0.7,
                            look.z * 2.2 + (rnd.nextDouble() - 0.5) * 0.7);
                }
            } else {
                // Tiny hot flare + one forward-pushed puff at the tip.
                level.addParticle(new com.techguns.techguns3.registry.TGParticles.GlowOptions(
                                0xFFFFFF, preset.flareColor(), 0.14f + 0.05f * stats.muzzleFlashScale(),
                                0.02f, 5, 0.0f, 1.0f),
                        tip.x, tip.y, tip.z, look.x * 0.3, look.y * 0.3, look.z * 0.3);
                level.addParticle(new com.techguns.techguns3.registry.TGParticles.PuffOptions(
                                0x9A9A9A, 0x3A3A3A, 0.14f, 0.34f, 18, -0.012f, 0.97f),
                        tip.x + look.x * 0.4, tip.y + look.y * 0.4, tip.z + look.z * 0.4,
                        look.x * 0.7, 0.15, look.z * 0.7);
            }
        } catch (Exception e) {
            com.techguns.techguns3.TechGuns3.LOGGER.debug("[TechGuns3] predict FX failed for {}",
                    gun, e);
        }
    }

    /** Smoothed ADS zoom (modern transition instead of an instant FOV cut). */
    private static float smoothedZoom = 1.0f;

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!(event.getPlayer() instanceof LocalPlayer player)) return;
        float fov = event.getFovModifier();
        float target = 1.0f;
        if (player.isUsingItem()) {
            ItemStack using = player.getUseItem();
            if (using.getItem() instanceof GenericGunItem gun
                    && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                var stats = gun.stats();
                if (stats.canZoom()) {
                    target = stats.zoomFov();
                }
            }
        }
        // Ease toward the target (~8 frames); instant when far off (weapon switch).
        if (Math.abs(target - smoothedZoom) > 0.4f) {
            smoothedZoom = target;
        } else {
            smoothedZoom += (target - smoothedZoom) * 0.25f;
        }
        fov *= smoothedZoom;
        // TG2 scope-recoil feel: brief punch-out on every shot, then settle.
        long dtMs = (System.nanoTime() - lastShotNanos) / 1_000_000L;
        if (dtMs >= 0 && dtMs < 90
                && player.getMainHandItem().getItem() instanceof GenericGunItem) {
            float k = 1.0f - dtMs / 90.0f;
            fov *= 1.0f + 0.025f * k;
        }
        event.setNewFovModifier(fov);
    }
}
