package com.techguns.techguns3.mob;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live server-side mob profiles.
 *
 * <p>Replaced wholesale on every datapack reload by
 * {@link com.techguns.techguns3.resource.MobDataLoader}; readers always go
 * through {@link #get(Identifier)} so the next AI tick after {@code /reload}
 * already uses the new numbers. A missing profile is logged once and the mob
 * degrades to an unarmed walker instead of crashing the world tick.</p>
 */
public final class MobRegistry {
    private MobRegistry() {}

    private static volatile Map<Identifier, MobProfile> LIVE = Map.of();
    private static volatile boolean loaded;
    private static final Set<Identifier> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    public static synchronized void apply(Map<Identifier, MobProfile> fresh) {
        if (fresh == null || fresh.isEmpty()) {
            TechGuns3.LOGGER.warn("[TechGuns3] mob datapack empty, keeping previous {} profiles", LIVE.size());
            return;
        }
        LIVE = Map.copyOf(fresh);
        WARNED_MISSING.clear();
        loaded = true;
        TechGuns3.LOGGER.info("[TechGuns3] loaded {} mob profiles", LIVE.size());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static MobProfile get(Identifier id) {
        if (id == null) return null;
        MobProfile profile = LIVE.get(id);
        if (profile == null && WARNED_MISSING.add(id)) {
            TechGuns3.LOGGER.warn("[TechGuns3] no mob profile for {}, the mob will not use Techguns AI", id);
        }
        return profile;
    }

    public static MobProfile getFor(EntityType<?> type) {
        if (type == null) return null;
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static Map<Identifier, MobProfile> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(LIVE));
    }
}
