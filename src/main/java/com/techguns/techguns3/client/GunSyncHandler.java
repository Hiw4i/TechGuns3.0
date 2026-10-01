package com.techguns.techguns3.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GunDefinition;
import com.techguns.techguns3.network.GunPackets;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies {@code GunSync} payloads into {@link GunClientCache}.
 * Runs on the client main thread (enqueued by the payload handler).
 *
 * <p>Deliberately NOT {@code @OnlyIn} (see {@link GunClientCache}): the
 * playToClient handler only ever runs on the client.</p>
 */
public final class GunSyncHandler {
    private GunSyncHandler() {}

    public static void apply(GunPackets.GunSync msg) {
        Map<Identifier, GunDefinition> decoded = new HashMap<>();
        for (var entry : msg.gunsJson().entrySet()) {
            Identifier gunId = entry.getKey();
            try {
                var json = JsonParser.parseString(entry.getValue());
                var result = GunDefinition.CODEC.parse(JsonOps.INSTANCE, json);
                if (result.isSuccess()) {
                    GunDefinition def = result.getOrThrow();
                    String err = def.validate();
                    if (err != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] sync gun {} invalid: {}", gunId, err);
                        continue;
                    }
                    decoded.put(gunId, def);
                } else {
                    TechGuns3.LOGGER.error("[TechGuns3] sync gun {} parse error: {}", gunId,
                            result.error().map(Object::toString).orElse("unknown"));
                }
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TechGuns3] sync gun {} failed", gunId, e);
            }
        }
        if (!decoded.isEmpty()) {
            GunClientCache.apply(decoded);
            TechGuns3.LOGGER.info("[TechGuns3] synced {} gun definitions", decoded.size());
        } else {
            TechGuns3.LOGGER.warn("[TechGuns3] gun sync empty, keeping client cache");
        }
    }
}
