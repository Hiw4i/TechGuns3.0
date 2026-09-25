package com.techguns.techguns3.combat;

import com.techguns.techguns3.item.AnimatedGunItem;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

    private record State(boolean firing, long nextShotTick, long reloadUntilTick, boolean zooming,
                           long spinId) {
        State withFiring(boolean v) { return new State(v, nextShotTick, reloadUntilTick, zooming, spinId); }
        State withNextShot(long v) { return new State(firing, v, reloadUntilTick, zooming, spinId); }
        State withReloadUntil(long v) { return new State(firing, nextShotTick, v, zooming, spinId); }
        State withZooming(boolean v) { return new State(firing, nextShotTick, reloadUntilTick, v, spinId); }
        State withSpinId(long v) { return new State(firing, nextShotTick, reloadUntilTick, zooming, v); }
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static State stateOf(Player player) {
        return STATES.getOrDefault(player.getUUID(), new State(false, 0, 0, false, 0));
    }

    private static void setState(Player player, State state) {
        STATES.put(player.getUUID(), state);
    }

    // --- packet entry points (already on server thread via enqueueWork) ---

    public static void onFireStart(Player player, GunPackets.FireStart msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) return;
        if (gun.stats().semiAuto()) {
            // Semi-auto guns never hold: treat a stray Start as one press.
            tryFireOnce(serverPlayer, held, gun, msg.zooming());
            return;
        }
        State state = stateOf(player);
        setState(player, state.withFiring(true).withZooming(msg.zooming()));
        LOG.info("[TechGuns3] hold-fire started: {} with {} (zooming={})",
                player.getScoreboardName(), held.getItem(), msg.zooming());
        if (gun.stats().spinsBarrels()) startSpin(serverPlayer, held, gun);
    }

    public static void onFireStop(Player player, GunPackets.FireStop msg) {
        if (!msg.mainHand() || !(player instanceof ServerPlayer serverPlayer)) return;
        State state = stateOf(player);
        if (!state.firing() && state.spinId() == 0) return;
        setState(player, state.withFiring(false));
        LOG.info("[TechGuns3] hold-fire stopped: {}", player.getScoreboardName());
        stopSpinIfNeeded(serverPlayer);
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
            if (state.firing() || state.spinId() != 0) {
                setState(player, state.withFiring(false));
                stopSpinIfNeeded(serverPlayer);
            }
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null || !state.firing()) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GenericGunItem gun)) {
            setState(player, state.withFiring(false));
            stopSpinIfNeeded(serverPlayer);
            return;
        }
        if (gun.stats().semiAuto()) {
            setState(player, state.withFiring(false));
            stopSpinIfNeeded(serverPlayer);
            return;
        }
        long now = player.level().getGameTime();
        if (now < state.reloadUntilTick() || now < state.nextShotTick()) return;
        tryFireOnce(serverPlayer, held, gun, state.zooming());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    // --- core shot / reload ---

    private static void tryFireOnce(ServerPlayer player, ItemStack gunStack, GenericGunItem gun, boolean zooming) {
        long now = player.level().getGameTime();
        State state = stateOf(player);
        if (now < state.reloadUntilTick()) return;
        int loaded = gun.loadedRounds(gunStack);
        if (loaded <= 0) {
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
            gunStack.set(TGDataComponents.AMMO.get(), loaded - 1);
        }
        setState(player, stateOf(player).withNextShot(now + Math.max(1, gun.stats().fireCooldownTicks())));
        player.getCooldowns().addCooldown(gunStack, Math.max(1, gun.stats().fireCooldownTicks()));
        if (gun instanceof AnimatedGunItem animated && player.level() instanceof ServerLevel server) {
            animated.triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(gunStack, server),
                    "action", "fire");
        }
        boolean zoomed = zooming && gun.stats().canZoom();
        float spreadMult = zoomed ? gun.stats().zoomSpreadMult() : 1.0f;
        BallisticCombat.fire(player.level(), player, gun.stats(), muzzlePos(player),
                player.getYRot(), player.getXRot(), SoundSource.PLAYERS, spreadMult);
    }

    /** Returns true if a reload actually started. */
    public static boolean startReload(ServerPlayer player, ItemStack gunStack, GenericGunItem gun) {
        long now = player.level().getGameTime();
        State state = stateOf(player);
        if (now < state.reloadUntilTick()) return false;
        if (gun.loadedRounds(gunStack) >= gun.stats().magazineSize()) return false;
        if (player.isCreative()) {
            gunStack.set(TGDataComponents.AMMO.get(), gun.stats().magazineSize());
            long until = now + Math.max(1, gun.stats().reloadTimeTicks());
            setState(player, state.withReloadUntil(until));
            player.getCooldowns().addCooldown(gunStack, gun.stats().reloadTimeTicks());
            stopSpinIfNeeded(player);
            playReloadAnim(player, gunStack, gun);
            return true;
        }
        Item wanted = gun.ammoItem();
        int found = 0;
        int slots = player.getInventory().getContainerSize();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(wanted)) found += stack.getCount();
        }
        if (found <= 0) return false;
        int take = gun.stats().magazineFed() ? 1 : Math.min(gun.stats().magazineSize(), found);
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
        Item emptyMagazine = gun.emptyMagazineItem();
        if (gun.stats().magazineFed() && emptyMagazine != null) {
            ItemStack returned = new ItemStack(emptyMagazine);
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        int loaded = gun.stats().magazineFed() ? gun.stats().magazineSize()
                : Math.min(consumed, gun.stats().magazineSize());
        gunStack.set(TGDataComponents.AMMO.get(), loaded);
        long until = now + Math.max(1, gun.stats().reloadTimeTicks());
        setState(player, stateOf(player).withReloadUntil(until));
        player.getCooldowns().addCooldown(gunStack, gun.stats().reloadTimeTicks());
        stopSpinIfNeeded(player);
        playReloadAnim(player, gunStack, gun);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                gun.stats().reloadSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
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

    /** Eye position shifted to the right-hand side, like the 1.12 spawn offset. */
    public static Vec3 muzzlePos(Player player) {
        Vec3 eye = player.getEyePosition();
        double yawRad = Math.toRadians(player.getYRot());
        return eye.add(-Math.cos(yawRad) * 0.16, -0.1, -Math.sin(yawRad) * 0.16);
    }
}
