package com.techguns.techguns3.client;

import com.techguns.techguns3.item.GunDefinition;
import com.techguns.techguns3.registry.GunRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client mirror of gun definitions for tooltip/HUD/FOV/recoil preview.
 * Filled by the sync packet; before first sync it serves registry builtins
 * so single-player boot and tooltips never see an empty map.
 *
 * <p>Deliberately NOT {@code @OnlyIn}: NeoForge no longer strips that
 * annotation at runtime, and this class is genuinely reachable from common
 * code ({@code item.GunDefProvider} guards it by dist at the call site,
 * plus the sync-packet handler only ever runs on the client).</p>
 */
public final class GunClientCache {
    private GunClientCache() {}

    private static final Map<Identifier, GunDefinition> CACHE = new ConcurrentHashMap<>();

    public static void apply(Map<Identifier, GunDefinition> fresh) {
        CACHE.clear();
        if (fresh != null) CACHE.putAll(fresh);
    }

    /** True only when this id arrived via sync (not a registry fallback). */
    public static boolean has(Identifier gunId) {
        return CACHE.containsKey(gunId);
    }

    public static GunDefinition get(Identifier gunId) {
        GunDefinition def = CACHE.get(gunId);
        if (def != null) return def;
        // Pre-sync fallback: builtins keep HUD/FOV working in menus.
        return GunRegistry.get(gunId);
    }

    public static Map<Identifier, GunDefinition> snapshot() {
        if (CACHE.isEmpty()) return GunRegistry.snapshot();
        return Collections.unmodifiableMap(new HashMap<>(CACHE));
    }
}
