package com.techguns.techguns3.combat;

import com.techguns.techguns3.item.AnimatedGunItem;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative gun loop. Replaces the 1.12 per-tick client polling
 * ({@code shootGunPrimary} driven by {@code keyFirePressed}) and the wave-1
 * {@code onUseTick} hold-to-fire hack.
 *
 * <p>Clients send only transitions ({@code FireStart/FireStop/FirePressed/Reload/Zoom});
 * this class owns fire rate, ammo and projectiles. Transient input state lives in a
 * plain map — it is intentionally <em>not</em> an attachment (no persistence, no sync).</p>
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID)
public final class GunServerLogic {
    private GunServerLogic() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** Visual-only tesla flicker between damage shots (damage shots keep their own arcs). */
    private static final long TESLA_REGEN_TICKS = 4;

    private record State(boolean firing, long nextShotTick, long reloadUntilTick, boolean zooming,
                           long spinId, long lastArcTick,
                           long chargeStartTick, int lockTargetId, long lockTicks, long lockSeenTick,
                           long loopTick) {
        State withFiring(boolean v) { return new State(v, nextShotTick, reloadUntilTick, zooming, spinId, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withNextShot(long v) { return new State(firing, v, reloadUntilTick, zooming, spinId, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withReloadUntil(long v) { return new State(firing, nextShotTick, v, zooming, spinId, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withZooming(boolean v) { return new State(firing, nextShotTick, reloadUntilTick, v, spinId, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withSpinId(long v) { return new State(firing, nextShotTick, reloadUntilTick, zooming, v, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withLastArc(long v) { return new State(firing, nextShotTick, reloadUntilTick, zooming, spinId, v, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withCharge(long v) { return new State(firing, nextShotTick, reloadUntilTick, zooming, spinId, lastArcTick, v, lockTargetId, lockTicks, lockSeenTick, loopTick); }
        State withLock(int id, long ticks, long seen) { return new State(firing, nextShotTick, reloadUntilTick, zooming, spinId, lastArcTick, chargeStartTick, id, ticks, seen, loopTick); }
        State withLoopTick(long v) { return new State(firing, nextShotTick, reloadUntilTick, zooming, spinId, lastArcTick, chargeStartTick, lockTargetId, lockTicks, lockSeenTick, v); }
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    /** Persistent sustained-beam entities (NDR) by shooter. Refreshed each tick, discarded on release. */
    private static final Map<UUID, com.techguns.techguns3.entity.BeamEntity> BEAMS = new ConcurrentHashMap<>();

    /** TFG charge orbs by shooter. Grown each tick while held, discarded on release. */
    private static final Map<UUID, com.techguns.techguns3.entity.ChargeOrbEntity> CHARGE_ORBS = new ConcurrentHashMap<>();

    /** Drop the sustained beam when the trigger releases, the weapon changes or the player leaves. */
    private static void discardBeam(Player player) {
        com.techguns.techguns3.entity.BeamEntity beam = BEAMS.remove(player.getUUID());
        if (beam != null) beam.discard();
    }

    private static void discardChargeOrb(Player player) {
        com.techguns.techguns3.entity.ChargeOrbEntity orb = CHARGE_ORBS.remove(player.getUUID());
        if (orb != null) orb.discard();
    }

    private static State stateOf(Player player) {
        return STATES.getOrDefault(player.getUUID(), new State(false, 0, 0, false, 0, 0, -1, -1, 0, 0, 0));
    }

    /** TFG-style hold-to-charge gun (charge extras without guided lock-on). Single extras lookup. */
    private static boolean isChargeGun(GenericGunItem gun) {
        var extras = gun.extras();
        return extras.hasCharge() && !extras.guided();
    }

    private static void setState(Player player, State state) {
        STATES.put(player.getUUID(), state);
    }

    // --- packet entry points (already on server thread via enqueueWork) ---

    public static void onFireStart(Player player, GunPackets.FireStart msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) return;
        long now = player.level().getGameTime();
        var extras = gun.extras();
        if (extras.hasCharge() && !extras.guided()) {
            // TFG-style: hold to charge, release to fire (GenericGunCharge).
            State state = stateOf(player);
            if (state.chargeStartTick() >= 0) return;
            if (gun.loadedRounds(held) <= 0) {
                setState(player, state.withNextShot(now + 5));
                startReload(serverPlayer, held, gun);
                return;
            }
            setState(player, state.withCharge(now).withZooming(msg.zooming()).withFiring(true).withLoopTick(now));
            playCharge(serverPlayer, held, gun);
            return;
        }
        if (gun.stats().semiAuto()) {
            // Semi-auto guns never hold: treat a stray Start as one press.
            tryFireOnce(serverPlayer, held, gun, msg.zooming());
            return;
        }
        State state = stateOf(player);
        setState(player, state.withFiring(true).withZooming(msg.zooming()).withLoopTick(now));
        LOG.debug("[TechGuns3] hold-fire started: {} with {} (zooming={})",
                player.getScoreboardName(), held.getItem(), msg.zooming());
        var stats = gun.stats();
        if (stats.spinsBarrels()) startSpin(serverPlayer, held, gun);
        if (extras.hasLoop() && stats.extraSound() != null) {
            // CE loop start (FLAMETHROWER_START / BEAMGUN_START): ignition jingle
            // on trigger pull, looped fire sound follows while held.
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    stats.extraSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static void onFireStop(Player player, GunPackets.FireStop msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        State state = stateOf(player);
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GenericGunItem gun && state.chargeStartTick() >= 0
                && isChargeGun(gun)) {
            long heldTicks = player.level().getGameTime() - state.chargeStartTick();
            setState(player, state.withCharge(-1).withFiring(false));
            stopCharge(serverPlayer, held, gun);
            discardChargeOrb(serverPlayer);
            tryFireCharged(serverPlayer, held, gun, state.zooming(), heldTicks);
            return;
        }
        if (!state.firing() && state.spinId() == 0 && state.chargeStartTick() < 0) return;
        setState(player, state.withFiring(false).withCharge(-1).withLock(-1, 0, 0));
        LOG.debug("[TechGuns3] hold-fire stopped: {}", player.getScoreboardName());
        stopSpinIfNeeded(serverPlayer);
        discardBeam(serverPlayer);
                discardChargeOrb(serverPlayer);
    }

    public static void onFirePressed(Player player, GunPackets.FirePressed msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) return;
        long now = player.level().getGameTime();
        State state = stateOf(player);
        // Latency tolerance: allow clicks that arrive marginally early, drop blatant spam.
        if (now + 2 < state.nextShotTick() || now < state.reloadUntilTick()) {
            LOG.debug("[TechGuns3] press dropped for {}: now={} nextShot={} reloadUntil={}",
                    player.getScoreboardName(), now, state.nextShotTick(), state.reloadUntilTick());
            return;
        }
        setState(player, state.withZooming(msg.zooming()));
        LOG.debug("[TechGuns3] single shot: {} with {} (zooming={})",
                player.getScoreboardName(), held.getItem(), msg.zooming());
        tryFireOnce(serverPlayer, held, gun, msg.zooming());
    }

    public static void onReloadRequest(Player player, GunPackets.ReloadRequest msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GenericGunItem gun) {
            startReload(serverPlayer, held, gun);
        }
    }

    public static void onZoomState(Player player, GunPackets.ZoomState msg) {
        if (!(player instanceof ServerPlayer)) return;
        setState(player, stateOf(player).withZooming(msg.zooming()));
    }

    // --- server tick: automatic fire while LMB is held ---

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.isSpectator() || !player.isAlive()) {
            State state = stateOf(player);
            if (state.firing() || state.spinId() != 0 || state.chargeStartTick() >= 0) {
                setState(player, state.withFiring(false).withCharge(-1).withLock(-1, 0, 0));
                stopSpinIfNeeded(serverPlayer);
                discardBeam(serverPlayer);
                discardChargeOrb(serverPlayer);
            }
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) {
            if (state.firing() || state.chargeStartTick() >= 0) {
                setState(player, state.withFiring(false).withCharge(-1).withLock(-1, 0, 0));
                stopSpinIfNeeded(serverPlayer);
                discardBeam(serverPlayer);
                discardChargeOrb(serverPlayer);
            }
            return;
        }
        long now = player.level().getGameTime();
        // One lookup per tick: stats()/extras() build fresh records each call.
        var extras = gun.extras();
        var stats = gun.stats();
        // Guided lock-on ticks while the trigger is held (grim) or a charge is held (TFG n/a).
        if (extras.guided() && (state.firing() || state.chargeStartTick() >= 0)) {
            tickLockOn(serverPlayer, gun, state);
            state = stateOf(player);
        }
        if (!state.firing()) return;
        if (stats.semiAuto()) {
            setState(player, state.withFiring(false));
            stopSpinIfNeeded(serverPlayer);
            return;
        }
        // Charged guns (TFG) never auto-fire: they wait for release.
        // While held, grow the charge orb + ramp the whine (TFGChargeStart FX).
        if (extras.hasCharge() && !extras.guided()) {
            tickChargeOrb(serverPlayer, held, gun, state);
            return;
        }
        // Sustained beam (NDR): damage every tick + persistent visual, no discrete shots.
        if (extras.continuousBeam()) {
            tickSustainedBeam(serverPlayer, held, gun, state);
            return;
        }
        if (now < state.reloadUntilTick() || now < state.nextShotTick()) return;
        if (stats.projectileKind() == com.techguns.techguns3.item.GunStats.ProjectileKind.ELECTRIC
                && now - state.lastArcTick() >= TESLA_REGEN_TICKS) {
            // Flicker stream between damage shots: visual-only arcs, same aim.
            setState(player, state.withLastArc(now));
            com.techguns.techguns3.combat.ElectricCombat.drawArcOnly(
                    serverPlayer.level(), serverPlayer, stats, muzzlePos(player),
                    player.getYRot(), player.getXRot());
            state = stateOf(player);
            if (now < state.nextShotTick()) return;
        }
        // Looping guns (flamethrower/NDR MaxLoopDelay): replay the fire sound
        // on a slow loop while held, independent of the per-shot rate.
        if (extras.hasLoop() && now - state.loopTick() >= extras.loopTicks()) {
            setState(player, stateOf(player).withLoopTick(now));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    stats.fireSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            state = stateOf(player);
        }
        if (stats.projectileKind() == com.techguns.techguns3.item.GunStats.ProjectileKind.FIRE) {
            // Unbroken flame tongue: a 5-tick textured flame refreshed every tick
            // while held, so the jet never gaps between the 2-tick pellets.
            Vec3 tongue = muzzlePos(player).add(player.getLookAngle().scale(0.6));
            player.level().addFreshEntity(new com.techguns.techguns3.entity.MuzzleFlashEntity(
                    player.level(), tongue.x, tongue.y, tongue.z,
                    com.techguns.techguns3.fx.GunFxPresets.TRACER_FIRE, 0.9f,
                    com.techguns.techguns3.item.GunExtras.FLASH_FLAME));
        }
        tryFireOnce(serverPlayer, held, gun, state.zooming());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        com.techguns.techguns3.entity.BeamEntity beam = BEAMS.remove(event.getEntity().getUUID());
        if (beam != null) beam.discard();
        com.techguns.techguns3.entity.ChargeOrbEntity orb = CHARGE_ORBS.remove(event.getEntity().getUUID());
        if (orb != null) orb.discard();
    }

    // --- core shot / reload ---

    private static void tryFireOnce(ServerPlayer player, ItemStack gunStack, GenericGunItem gun, boolean zooming) {
        tryFireOnce(player, gunStack, gun, zooming, 0.0f, 1.0f);
    }

    /**
     * Sustained-beam tick (NDRProjectile.onUpdate 1:1): full damage + radiation
     * every tick while held, 1 round per {@code fireCooldownTicks}, one visual
     * beam refreshed in place instead of a shot per cooldown.
     */
    private static void tickSustainedBeam(ServerPlayer player, ItemStack gunStack, GenericGunItem gun, State state) {
        long now = player.level().getGameTime();
        var stats = gun.stats();
        var extras = gun.extras();
        if (now < state.reloadUntilTick()) return;
        if (now >= state.nextShotTick()) {
            int loaded = gun.loadedRounds(gunStack);
            if (loaded <= 0) {
                setState(player, stateOf(player).withNextShot(now + 5).withFiring(false));
                discardBeam(player);
                stopSpinIfNeeded(player);
                startReload(player, gunStack, gun);
                return;
            }
            if (!player.isCreative()) {
                gunStack.set(TGDataComponents.AMMO.get(), loaded - 1);
            }
            setState(player, stateOf(player).withNextShot(now + Math.max(1, stats.fireCooldownTicks())));
            state = stateOf(player);
        }
        Vec3 muzzle = muzzlePos(player);
        com.techguns.techguns3.combat.BeamCombat.tickDamage(player.level(), player, stats,
                extras, muzzle, player.getYRot(), player.getXRot());
        // Persistent visual: spawn once, re-anchor + re-aim every tick (CE teleport).
        com.techguns.techguns3.entity.BeamEntity beam = BEAMS.get(player.getUUID());
        Vec3 dir = player.getLookAngle().normalize();
        double range = Math.max(10.0, stats.maxRange() * stats.bulletSpeed());
        Vec3 end = muzzle.add(dir.scale(range));
        var wall = player.level().clip(new net.minecraft.world.level.ClipContext(muzzle, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = wall.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? end : wall.getLocation();
        if (beam == null || beam.isRemoved()) {
            beam = new com.techguns.techguns3.entity.BeamEntity(player.level(), muzzle, beamEnd,
                    com.techguns.techguns3.fx.GunFxPresets.TRACER_NDR);
            player.level().addFreshEntity(beam);
            BEAMS.put(player.getUUID(), beam);
        } else {
            beam.updateEnd(muzzle, beamEnd);
        }
    }

    /**
     * TFG charge hold (TFGChargeStart FX 1:1 in spirit): a plasma orb grows at
     * the muzzle with charge 0..1 while LMB is held, plus a rising whine.
     * Release (FireStop) fires the scaled shot and discards the orb.
     */
    private static void tickChargeOrb(ServerPlayer player, ItemStack gunStack, GenericGunItem gun, State state) {
        if (state.chargeStartTick() < 0) return;
        long now = player.level().getGameTime();
        var stats = gun.stats();
        var extras = gun.extras();
        float charge = Math.min(1.0f, (now - state.chargeStartTick())
                / (float) Math.max(1, extras.chargeTicks()));
        Vec3 muzzle = muzzlePos(player);
        com.techguns.techguns3.entity.ChargeOrbEntity orb = CHARGE_ORBS.get(player.getUUID());
        if (orb == null || orb.isRemoved()) {
            orb = new com.techguns.techguns3.entity.ChargeOrbEntity(player.level(), muzzle, charge);
            player.level().addFreshEntity(orb);
            CHARGE_ORBS.put(player.getUUID(), orb);
            setState(player, stateOf(player).withLoopTick(now));
        } else {
            orb.updateCharge(muzzle, charge);
        }
        // Rising whine: replay the charge loop higher as it grows.
        if (stats.extraSound() != null && now - stateOf(player).loopTick() >= 12) {
            setState(player, stateOf(player).withLoopTick(now));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    stats.extraSound().get(), SoundSource.PLAYERS, 1.0f, 0.75f + 0.65f * charge);
        }
        // Inward-sucking sparks while charging (implosion read).
        if (player.level().getRandom().nextFloat() < 0.6f) {
            com.techguns.techguns3.fx.FxParticles.glow(player.level(),
                    muzzle.x, muzzle.y, muzzle.z, 2,
                    0x39FF5E, 0x1A7A2E, 0.14f, 0.03f, 12, -0.02f, 0.96f, 1.2, 0.8);
        }
    }

    /** Charged release (TFG): {@code heldTicks} scales damage/size/ammo like GenericGunCharge. */
    private static void tryFireCharged(ServerPlayer player, ItemStack gunStack, GenericGunItem gun,
                                       boolean zooming, long heldTicks) {
        var extras = gun.extras();
        float charge = extras.hasCharge()
                ? Math.min(1.0f, heldTicks / (float) Math.max(1, extras.chargeTicks()))
                : 0.0f;
        // TFG: size 1 + charge*3 (+1 when full); damage scales mildly with charge.
        float scale = 0.45f + charge * 0.55f;
        tryFireOnce(player, gunStack, gun, zooming, charge, scale);
    }

    private static void tryFireOnce(ServerPlayer player, ItemStack gunStack, GenericGunItem gun,
                                    boolean zooming, float charge01, float chargeScale) {
        var stats = gun.stats();
        var extras = gun.extras();
        // Sustained beams never fire discrete shots; the hold tick owns them.
        if (extras.continuousBeam()) return;
        long now = player.level().getGameTime();
        State state = stateOf(player);
        if (now < state.reloadUntilTick()) return;
        int loaded = gun.loadedRounds(gunStack);
        int want = extras.hasCharge()
                ? Math.round(extras.ammoBase() + (extras.ammoFull() - extras.ammoBase()) * charge01)
                : 1;
        want = Math.max(1, want);
        if (loaded < want) {
            // Empty trigger pull: haptic cooldown + auto-reload, like the 1.12 dry-fire path.
            // Automatics keep the trigger held so the burst resumes after reloading.
            setState(player, state.withNextShot(now + 5));
            stopSpinIfNeeded(player);
            LOG.debug("[TechGuns3] trigger on empty {} ({}), requesting reload",
                    gunStack.getItem(), player.getScoreboardName());
            startReload(player, gunStack, gun);
            return;
        }
        if (!player.isCreative()) {
            gunStack.set(TGDataComponents.AMMO.get(), loaded - want);
        }
        setState(player, stateOf(player).withNextShot(now + Math.max(1, stats.fireCooldownTicks())));
        player.getCooldowns().addCooldown(gunStack, Math.max(1, stats.fireCooldownTicks()));
        if (gun instanceof AnimatedGunItem animated && player.level() instanceof ServerLevel server) {
            animated.triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(gunStack, server),
                    "action", "fire");
        }
        boolean zoomed = zooming && stats.canZoom();
        float spreadMult = zoomed ? stats.zoomSpreadMult() : 1.0f;
        String variant = gunStack.getOrDefault(TGDataComponents.AMMO_VARIANT.get(), null);
        int homingId = -1;
        if (extras.guided()) {
            homingId = stateOf(player).lockTargetId();
            if (homingId < 0) {
                // No lock yet: try an instant acquisition so tap-shots still track.
                var snap = acquireLockTarget(player, gun);
                if (snap != null) homingId = snap.getId();
            }
        }
        BallisticCombat.fire(player.level(), player, stats, extras, variant,
                charge01, chargeScale, homingId, muzzlePos(player),
                player.getYRot(), player.getXRot(), SoundSource.PLAYERS, spreadMult);
    }

    /** Returns true if a reload actually started. */
    public static boolean startReload(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        long now = player.level().getGameTime();
        State state = stateOf(player);
        var stats = gun.stats();
        var extras = gun.extras();
        if (now < state.reloadUntilTick()) return false;
        if (gun.loadedRounds(gunStack) >= stats.magazineSize()) return false;
        if (player.isCreative()) {
            gunStack.set(TGDataComponents.AMMO.get(), stats.magazineSize());
            long until = now + Math.max(1, stats.reloadTimeTicks());
            setState(player, state.withReloadUntil(until));
            player.getCooldowns().addCooldown(gunStack, stats.reloadTimeTicks());
            stopSpinIfNeeded(player);
            playReloadAnim(player, gunStack, gun);
            return true;
        }
        // Variant priority: extras-first (nuke > hv > normal, explosive > incendiary > normal),
        // so the strongest carried ammo loads automatically (CE toggleAmmoType, simplified).
        java.util.List<Item> priority = new java.util.ArrayList<>(gun.acceptedAmmoItems());
        if (extras.hasExtraAmmo()) {
            priority.sort((a, b) -> {
                boolean aExtra = isExtraAmmo(gun, a);
                boolean bExtra = isExtraAmmo(gun, b);
                if (aExtra == bExtra) return 0;
                return aExtra ? -1 : 1;
            });
        }
        Item wanted = null;
        int found = 0;
        int slots = player.getInventory().getContainerSize();
        for (Item candidate : priority) {
            int count = 0;
            for (int i = 0; i < slots; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(candidate)) count += stack.getCount();
            }
            if (count > 0) { wanted = candidate; found = count; break; }
        }
        if (wanted == null || found <= 0) return false;
        int take = stats.magazineFed() ? 1 : Math.min(stats.magazineSize(), found);
        int remaining = take;
        for (int i = 0; i < slots && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(wanted)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
        int consumed = take - remaining;
        if (consumed <= 0) return false;
        Item emptyMagazine = emptyFor(gun, wanted);
        if (stats.magazineFed() && emptyMagazine != null) {
            ItemStack returned = new ItemStack(emptyMagazine);
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        int loaded = stats.magazineFed() ? stats.magazineSize()
                : Math.min(consumed, stats.magazineSize());
        gunStack.set(TGDataComponents.AMMO.get(), loaded);
        // Remember the variant so fire() picks nuke/hv/explosive/incendiary projectiles.
        String variantId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(wanted).getPath();
        String defaultId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gun.ammoItem()).getPath();
        if (variantId.equals(defaultId)) {
            gunStack.remove(TGDataComponents.AMMO_VARIANT.get());
        } else {
            gunStack.set(TGDataComponents.AMMO_VARIANT.get(), variantId);
        }
        long until = now + Math.max(1, stats.reloadTimeTicks());
        setState(player, stateOf(player).withReloadUntil(until));
        player.getCooldowns().addCooldown(gunStack, stats.reloadTimeTicks());
        stopSpinIfNeeded(player);
        playReloadAnim(player, gunStack, gun);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                stats.reloadSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private static boolean isExtraAmmo(GenericGunItem gun, Item item) {
        if (!gun.extras().hasExtraAmmo()) return false;
        for (String id : gun.extras().extraAmmoIds()) {
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().equals(id)) return true;
        }
        return false;
    }

    private static Item emptyFor(GenericGunItem gun, Item ammo) {
        // Variant mags share the family's empty item where CE has no separate empty.
        Item base = gun.emptyMagazineItem();
        if (ammo == gun.ammoItem()) return base;
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo).getPath();
        if (path.contains("as50")) {
            try {
                var loc = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        com.techguns.techguns3.TechGuns3.MODID, "as50_magazine_empty");
                var empty = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(loc);
                if (empty != null && empty != net.minecraft.world.item.Items.AIR) return empty;
            } catch (Exception e) {
                LOG.debug("[TechGuns3] AS50 empty-mag lookup failed", e);
            }
        }
        return base;
    }

    private static void playReloadAnim(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        if (gun instanceof AnimatedGunItem animated && player.level() instanceof ServerLevel server) {
            animated.triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(gunStack, server),
                    "action", "reload");
        }
    }

    private static void startSpin(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        State state = stateOf(player);
        if (state.spinId() != 0) return;
        if (!(player.level() instanceof ServerLevel server)) return;
        long id = com.geckolib.animatable.GeoItem.getOrAssignId(gunStack, server);
        setState(player, state.withSpinId(id));
        if (gun instanceof AnimatedGunItem animated) {
            animated.triggerAnim(player, id, "barrels", "spin");
        }
    }

    /** Stops the barrel loop using the id captured at spin start (survives weapon switches). */
    private static void stopSpinIfNeeded(ServerPlayer player) {
        State state = stateOf(player);
        if (state.spinId() == 0) return;
        setState(player, stateOf(player).withSpinId(0));
        if (player.level() instanceof ServerLevel && TGItems.MINIGUN.get() instanceof AnimatedGunItem minigun) {
            minigun.stopTriggeredAnim(player, state.spinId(), "barrels", "spin");
        }
    }

    private static void playCharge(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        // TFG charge loop: sound + looping charge anim (CE setChargeSound/ChargeFX).
        try {
            var charge = gun.stats().extraSound();
            if (charge != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        charge.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            LOG.debug("[TechGuns3] charge sound failed for {}", gunStack.getItem(), e);
        }
        if (gun instanceof AnimatedGunItem animated && player.level() instanceof ServerLevel server) {
            try {
                animated.triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(gunStack, server),
                        "action", "charge");
            } catch (Exception e) {
                LOG.debug("[TechGuns3] charge anim failed for {}", gunStack.getItem(), e);
            }
        }
    }

    private static void stopCharge(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        if (gun instanceof AnimatedGunItem animated && player.level() instanceof ServerLevel) {
            try {
                long id = com.geckolib.animatable.GeoItem.getOrAssignId(gunStack,
                        (ServerLevel) player.level());
                animated.stopTriggeredAnim(player, id, "action", "charge");
            } catch (Exception e) {
                LOG.debug("[TechGuns3] stop charge anim failed for {}", gunStack.getItem(), e);
            }
        }
    }

    /**
     * Guided lock-on tick (GuidedMissileLauncher.setLockOn 20/80): hold the aim on a
     * target for {@code lockTime} ticks to lock, keep it for {@code lockPersist} ticks
     * after losing sight. Beeps like the original (search every 12t, locked every 4t).
     */
    private static void tickLockOn(ServerPlayer player, GenericGunItem gun, State state) {
        var extras = gun.extras();
        long now = player.level().getGameTime();
        var target = acquireLockTarget(player, gun);
        if (target != null) {
            if (target.getId() == state.lockTargetId()) {
                long ticks = state.lockTicks() + 1;
                setState(player, stateOf(player).withLock(target.getId(), ticks, now));
                if (ticks == extras.lockTime()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            com.techguns.techguns3.registry.TGSounds.LOCKED_BEEP.get(),
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                } else if (ticks > extras.lockTime() && now % 4 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            com.techguns.techguns3.registry.TGSounds.LOCKED_BEEP.get(),
                            SoundSource.PLAYERS, 0.5f, 1.0f);
                }
            } else {
                setState(player, stateOf(player).withLock(target.getId(), 1, now));
                if (now % 12 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            com.techguns.techguns3.registry.TGSounds.LOCKON_BEEP.get(),
                            SoundSource.PLAYERS, 0.5f, 1.0f);
                }
            }
        } else if (state.lockTargetId() >= 0 && now - state.lockSeenTick() < extras.lockPersist()) {
            // Coast: keep the lock while within persist window.
            if (now % 12 == 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.techguns.techguns3.registry.TGSounds.LOCKON_BEEP.get(),
                        SoundSource.PLAYERS, 0.4f, 1.0f);
            }
        } else if (state.lockTargetId() >= 0) {
            setState(player, stateOf(player).withLock(-1, 0, 0));
        }
    }

    /** Nearest valid target in the aim cone within lock range (CE traceTarget, simplified). */
    private static LivingEntity acquireLockTarget(ServerPlayer player, GenericGunItem gun) {
        var extras = gun.extras();
        if (!(player.level() instanceof ServerLevel level)) return null;
        Vec3 muzzle = muzzlePos(player);
        Vec3 dir = player.getLookAngle().normalize();
        double range = extras.lockRange();
        LivingEntity best = null;
        double bestDist = range * range;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(muzzle, muzzle.add(dir.scale(range))).inflate(3.0),
                e -> e != player && e.isAlive() && !player.isAlliedTo(e)
                        && e.canBeHitByProjectile())) {
            Vec3 toTarget = candidate.getEyePosition().subtract(muzzle);
            double dist = toTarget.lengthSqr();
            if (dist < 0.25 || dist > bestDist) continue;
            if (toTarget.normalize().dot(dir) < Math.cos(Math.toRadians(6.0))) continue;
            var wall = level.clip(new net.minecraft.world.level.ClipContext(muzzle,
                    candidate.getEyePosition(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) continue;
            best = candidate;
            bestDist = dist;
        }
        return best;
    }

    /**
     * Muzzle at the barrel tip, like 1.12 ({@code EnumBulletFirePos.RIGHT}:
     * side offset plus a short forward offset along the look vector).
     * Deliberately close to the body (~0.5m ahead of the eyes) so point-blank
     * targets stand in FRONT of the spawn point instead of behind it; the
     * shooter itself is always excluded from hits. First-person visuals are
     * guarded render-side (tracer shrink, flash fade), never by pushing the
     * gameplay spawn out of the fight.
     */
    public static Vec3 muzzlePos(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double yawRad = Math.toRadians(player.getYRot());
        return eye.add(look.scale(0.5))
                .add(-Math.cos(yawRad) * 0.15, -0.05, -Math.sin(yawRad) * 0.15);
    }

    /** Barrel tip for non-player shooters (NPCs, turrets): ahead of the eye, same close rule. */
    public static Vec3 muzzlePos(net.minecraft.world.entity.LivingEntity shooter) {
        if (shooter instanceof Player player) return muzzlePos(player);
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        return eye.add(look.scale(0.5)).add(0, -0.05, 0);
    }
}
