package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.AnimatedGunItem;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
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
 *
 * <p>It also advances the weapon feel once per rendered frame: the springs of
 * the recoil, the aim blend and the scope zoom have to move with the frames
 * and not with the ticks.</p>
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
    /** Game time the last reload was asked for, -1 when none is running. */
    private static long reloadStartTick = -1;

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

    /**
     * Local reload progress 0..1 for the HUD (-1 when no reload is running).
     *
     * <p>The fire cooldown and the reload share the item cooldown of the
     * weapon, so the cooldown alone cannot tell them apart. The client
     * therefore tracks the reload itself, from the moment it asks for one.</p>
     */
    public static float reloadProgress(ItemStack held) {
        if (reloadStartTick < 0 || held.isEmpty()) return -1.0f;
        if (!(held.getItem() instanceof GenericGunItem gun)) return -1.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return -1.0f;
        long now = mc.level.getGameTime();
        int ticks = Math.max(1, gun.stats().reloadTimeTicks());
        if (now - reloadStartTick >= ticks) {
            reloadStartTick = -1;
            return -1.0f;
        }
        return (now - reloadStartTick) / (float) ticks;
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
        // The scope zoom belongs to the weapon in the hand, without one there is
        // no zoom at all.
        GunFeel.get().setZoomTarget(1.0f);
        if (player == null || mc.gui.screen() != null) {
            // The weapon feel must not keep the aim pose of a gun that is gone.
            GunFeel.get().setAiming(false);
            GunFeel.get().setSprinting(false);
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
            GunFeel.get().setAiming(false);
            GunFeel.get().setSprinting(false);
            lmbDown = false;
            syncZoom(player, false);
            return;
        }
        ItemStack held = player.getMainHandItem();
        boolean holding = held.getItem() instanceof GenericGunItem;

        // Weapon switch invalidates hold state on both sides. Compare the item only:
        // every shot mutates the AMMO component, which must NOT look like a switch.
        if (held.getItem() != lastGun.getItem()) {
            // TEMP DIAGNOSTIC: pins down the reported per-shot draw replay.
            com.techguns.techguns3.TechGuns3.LOGGER.info(
                    "[TechGuns3] weapon switch: {} -> {} (draw anim)",
                    lastGun.isEmpty() ? "<empty>" : lastGun.getItem(), held.getItem());
            lastGun = held.copy();
            chargeStartTick = -1;
            if (lmbDown) {
                lmbDown = false;
                ClientPacketDistributor.sendToServer(new GunPackets.FireStop(true));
            }
            syncZoom(player, false);
            // The weapon is drawn: it comes up into the hand.
            if (held.getItem() instanceof AnimatedGunItem drawn) {
                com.techguns.techguns3.combat.GunAnimSync.triggerLocal(held,
                        AnimatedGunItem.CONTROLLER_DRAW, "draw");
            }
        }
        if (!holding) {
            lmbDown = false;
            syncZoom(player, false);
            GunFeel.get().setAiming(false);
            GunFeel.get().setSprinting(false);
            return;
        }

        // Reload key (edge-triggered by vanilla).
        if (TGKeyMappings.reload().consumeClick()) {
            reloadStartTick = player.level().getGameTime();
            ClientPacketDistributor.sendToServer(new GunPackets.ReloadRequest(true));
        }

        // Look the weapon over.
        if (TGKeyMappings.inspect().consumeClick()) {
            com.techguns.techguns3.combat.GunAnimSync.triggerLocal(held,
                    AnimatedGunItem.CONTROLLER_INSPECT, "inspect");
        }

        boolean zooming = player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem().getItem() instanceof GenericGunItem;
        syncZoom(player, zooming);
        GunFeel.get().setAiming(zooming);
        GunFeel.get().setSprinting(player.isSprinting());
        // The scope zoom of the held weapon. The spring smooths the transition
        // in a frame rate independent way.
        float zoomTarget = 1.0f;
        if (zooming) {
            var stats = ((GenericGunItem) player.getUseItem().getItem()).stats();
            if (stats.canZoom()) {
                zoomTarget = stats.zoomFov();
            }
        }
        GunFeel.get().setZoomTarget(zoomTarget);

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
                // An empty magazine is the server's business; predicting a shot
                // here would kick the weapon for nothing.
                if (now - lastAutoFeedbackTick >= period && gun.loadedRounds(held) > 0) {
                    lastAutoFeedbackTick = now;
                    predictShotFeedback(player, held, gun, stats, extras);
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
            if (gun.loadedRounds(held) <= 0) {
                // Empty magazine: the server starts an automatic reload. The
                // weapon clicks instead of kicking, there is nothing to fire.
                reloadStartTick = player.level().getGameTime();
                com.techguns.techguns3.combat.GunAnimSync.triggerLocal(held,
                        AnimatedGunItem.CONTROLLER_DRYFIRE, "dryfire");
                return;
            }
            predictShotFeedback(player, held, gun, stats, extras);
        } else {
            ClientPacketDistributor.sendToServer(new GunPackets.FireStart(true, zooming));
            lastAutoFeedbackTick = player.level().getGameTime();
            if (gun.loadedRounds(held) <= 0) {
                // Empty magazine: the server starts an automatic reload. The
                // weapon clicks instead of kicking, there is nothing to fire.
                reloadStartTick = player.level().getGameTime();
                com.techguns.techguns3.combat.GunAnimSync.triggerLocal(held,
                        AnimatedGunItem.CONTROLLER_DRYFIRE, "dryfire");
                return;
            }
            // Sustained beams start silently; the hold tick owns the tremor.
            if (!extras.continuousBeam()) {
                predictShotFeedback(player, held, gun, stats, extras);
            }
        }
    }

    private static void syncZoom(LocalPlayer player, boolean zooming) {
        if (zooming == zoomSent) return;
        zoomSent = zooming;
        ClientPacketDistributor.sendToServer(new GunPackets.ZoomState(zooming));
    }

    /**
     * Instant local feedback for a predicted shot: the weapon animation, the
     * view-model muzzle flash, the weapon recoil spring and the camera kick.
     *
     * <p>The muzzle flash is drawn on the weapon model, so it stays glued to
     * the barrel instead of hovering at a fixed point in the world. Damage,
     * ammo and the authoritative flash for everybody else stay on the
     * server.</p>
     *
     * <p>Takes pre-resolved stats/extras: both build fresh records per call.</p>
     */
    private static void predictShotFeedback(LocalPlayer player, ItemStack held, GenericGunItem gun,
                                            com.techguns.techguns3.item.GunStats stats,
                                            com.techguns.techguns3.item.GunExtras extras) {
        // The weapon reacts on the same frame as the trigger pull; the server
        // broadcast skips the shooter for exactly this reason.
        com.techguns.techguns3.combat.GunAnimSync.triggerLocal(
                held, com.techguns.techguns3.item.AnimatedGunItem.CONTROLLER_FIRE, "fire");

        float yawJitter = (player.getRandom().nextFloat() * 2.0f - 1.0f) * stats.recoilYawJitter();
        if (TGConfig.cameraRecoil()) {
            // Negative pitch kicks the muzzle up (XRot decreases looking up).
            player.turn(yawJitter, -stats.recoilPitchDeg());
            // Heavy guns thump the camera (decays in TGShake).
            TGShake.addTrauma(Math.min(0.45f, stats.recoilPitchDeg() * 0.12f));
        }
        // The weapon itself kicks back and settles on its own.
        GunFeel.get().kick(stats.recoilPitchDeg(), yawJitter * 2.0f, 1.0f);
        if (!TGConfig.cinematicFx()) return;
        try {
            // Same barrel tip the server fires from (single source of truth).
            var tip = com.techguns.techguns3.combat.GunServerLogic.muzzlePos(player);
            var look = player.getLookAngle();
            var level = player.level();
            var preset = com.techguns.techguns3.fx.GunFxPresets.forKind(
                    stats.projectileKind(), stats.muzzleFlashScale());
            // View model flash: glued to the barrel of the held weapon.
            MuzzleFlashFx.trigger(
                    com.techguns.techguns3.fx.GunFxPresets.muzzleTexture(stats, extras),
                    preset.muzzleColor(),
                    0.45f + 0.35f * stats.muzzleFlashScale());
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
                // Hot air at the muzzle. The flash itself is drawn on the
                // weapon model, a world glow would only be a blob in the
                // player's face.
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
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!(event.getPlayer() instanceof LocalPlayer player)) return;
        float fov = event.getFovModifier();
        if (player.getMainHandItem().getItem() instanceof GenericGunItem) {
            // The zoom multiplies the field of view of the game, it only means
            // something while a gun is in the hand.
            fov *= GunFeel.get().zoom();
            // TG2 scope-recoil feel: brief punch-out on every shot, then settle.
            // The punch is a spring, so it eases back instead of cutting off.
            fov *= 1.0f + 0.35f * GunFeel.get().fovPunch();
        }
        event.setNewFovModifier(fov);
    }

    /**
     * Advances the weapon feel once per rendered frame.
     *
     * <p>The springs of the feel have to advance exactly once per frame, and
     * the FOV event is not the right place for it: it does not fire while a
     * screen is open, so the motion of the weapon would freeze in the
     * inventory.</p>
     */
    @SubscribeEvent
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        GunFeel.get().update(event.getPartialTick());
        // The offset of the weapon is put on the pose stack of the hand, the
        // other hand has to take it away again. It is reset here, because the
        // hand that carries the gun is not always drawn.
        GunAimHandler.beginFrame();
    }
}
