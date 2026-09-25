package com.techguns.techguns3.network;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.combat.GunServerLogic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;

/**
 * Client -&gt; server gun intents. Modern replacement for the 1.12 polling
 * ({@code ClientProxy.keyFirePressed*} + per-tick {@code shootGunPrimary} calls).
 *
 * <p>Only state transitions cross the network — no per-tick spam:
 * <ul>
 *   <li>{@link FireStart} — LMB went down with an automatic gun (hold-to-fire begins).</li>
 *   <li>{@link FireStop} — LMB released, weapon switched, player died, etc.</li>
 *   <li>{@link FirePressed} — single LMB click with a semi-automatic gun.</li>
 *   <li>{@link ReloadRequest} — {@code R} key or auto-reload on empty trigger pull.</li>
 *   <li>{@link ZoomState} — RMB aim hold started/stopped (drives server-side spread bonus).</li>
 * </ul>
 * The server owns cooldowns, ammo and projectiles ({@link GunServerLogic});
 * the client only predicts visuals (recoil kick, muzzle particles, sounds stay server-side
 * so other players hear them too).</p>
 */
public final class GunPackets {
    private GunPackets() {}

    /** LMB hold started with an automatic weapon. {@code mainHand} is always true for now. */
    public record FireStart(boolean mainHand, boolean zooming) implements CustomPacketPayload {
        public static final Type<FireStart> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "gun_fire_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FireStart> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, FireStart::mainHand,
                        ByteBufCodecs.BOOL, FireStart::zooming, FireStart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(FireStart msg, IPayloadContext ctx) {
            if (ctx instanceof ServerPayloadContext server) {
                server.enqueueWork(() -> GunServerLogic.onFireStart(server.player(), msg));
            }
        }
    }

    /** LMB released / hold invalidated. */
    public record FireStop(boolean mainHand) implements CustomPacketPayload {
        public static final Type<FireStop> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "gun_fire_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FireStop> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, FireStop::mainHand, FireStop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(FireStop msg, IPayloadContext ctx) {
            if (ctx instanceof ServerPayloadContext server) {
                server.enqueueWork(() -> GunServerLogic.onFireStop(server.player(), msg));
            }
        }
    }

    /** Single LMB click with a semi-automatic weapon (rising edge only). */
    public record FirePressed(boolean mainHand, boolean zooming) implements CustomPacketPayload {
        public static final Type<FirePressed> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "gun_fire_pressed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FirePressed> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, FirePressed::mainHand,
                        ByteBufCodecs.BOOL, FirePressed::zooming, FirePressed::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(FirePressed msg, IPayloadContext ctx) {
            if (ctx instanceof ServerPayloadContext server) {
                server.enqueueWork(() -> GunServerLogic.onFirePressed(server.player(), msg));
            }
        }
    }

    /** Manual ({@code R}) or automatic (empty mag) reload request. */
    public record ReloadRequest(boolean mainHand) implements CustomPacketPayload {
        public static final Type<ReloadRequest> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "gun_reload"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReloadRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ReloadRequest::mainHand, ReloadRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ReloadRequest msg, IPayloadContext ctx) {
            if (ctx instanceof ServerPayloadContext server) {
                server.enqueueWork(() -> GunServerLogic.onReloadRequest(server.player(), msg));
            }
        }
    }

    /** RMB aim state change. Lets the server apply the zoom spread bonus honestly. */
    public record ZoomState(boolean zooming) implements CustomPacketPayload {
        public static final Type<ZoomState> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "gun_zoom"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ZoomState> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ZoomState::zooming, ZoomState::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ZoomState msg, IPayloadContext ctx) {
            if (ctx instanceof ServerPayloadContext server) {
                server.enqueueWork(() -> GunServerLogic.onZoomState(server.player(), msg));
            }
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(FireStart.TYPE, FireStart.CODEC, FireStart::handle);
        registrar.playToServer(FireStop.TYPE, FireStop.CODEC, FireStop::handle);
        registrar.playToServer(FirePressed.TYPE, FirePressed.CODEC, FirePressed::handle);
        registrar.playToServer(ReloadRequest.TYPE, ReloadRequest.CODEC, ReloadRequest::handle);
        registrar.playToServer(ZoomState.TYPE, ZoomState.CODEC, ZoomState::handle);
    }
}
