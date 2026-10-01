package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GunDefinition;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative gun definitions.
 *
 * <p>{@code LIVE} is replaced wholesale on every datapack reload
 * ({@code resource.GunDataLoader}); readers always go through {@link #get}
 * so the next shot after {@code /reload} already uses new numbers.
 * When JSON is missing or broken we keep the last good map, and when there
 * is no map at all (first boot before reload) we use Java builtins that
 * mirror the old {@code TGItems} numbers.</p>
 */
public final class GunRegistry {
    private GunRegistry() {}

    /**
     * Live map, swapped atomically on every datapack reload. Readers never
     * observe a half-applied map (no clear()+putAll() window where a shot
     * would fall through to the emergency fallback mid-reload).
     */
    private static volatile Map<Identifier, GunDefinition> LIVE = Map.of();
    private static volatile boolean loaded;
    /** Gun ids already warned about (per-miss logging would spam every shot). */
    private static final Set<Identifier> WARNED_MISSING = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> WARNED_BUILTIN = ConcurrentHashMap.newKeySet();

    /** Apply a freshly parsed datapack map (already validated). */
    public static synchronized void apply(Map<Identifier, GunDefinition> fresh) {
        if (fresh == null || fresh.isEmpty()) {
            TechGuns3.LOGGER.warn("[TechGuns3] gun datapack empty, keeping previous {} defs", LIVE.size());
            return;
        }
        LIVE = Map.copyOf(fresh);
        WARNED_MISSING.clear();
        WARNED_BUILTIN.clear();
        loaded = true;
        TechGuns3.LOGGER.info("[TechGuns3] loaded {} gun definitions", LIVE.size());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static GunDefinition get(Identifier gunId) {
        GunDefinition def = LIVE.get(gunId);
        if (def != null) return def;
        GunDefinition fallback = Builtins.ALL.get(gunId);
        if (fallback != null) {
            if (WARNED_BUILTIN.add(gunId)) {
                TechGuns3.LOGGER.debug("[TechGuns3] gun {} not in datapack, using builtin", gunId);
            }
            return fallback;
        }
        // Last resort: generic pistol-shaped fallback so a missing JSON never crashes firing.
        if (WARNED_MISSING.add(gunId)) {
            TechGuns3.LOGGER.warn("[TechGuns3] no definition for {}, using emergency fallback", gunId);
        }
        return Builtins.EMERGENCY;
    }

    public static Map<Identifier, GunDefinition> snapshot() {
        if (!LIVE.isEmpty()) return Collections.unmodifiableMap(new HashMap<>(LIVE));
        return Collections.unmodifiableMap(new HashMap<>(Builtins.ALL));
    }

    /** Java mirrors of the old TGItems numbers. Only used before/without datapack. */
    public static final class Builtins {
        private Builtins() {}

        public static Identifier id(String path) {
            return Identifier.fromNamespaceAndPath(TechGuns3.MODID, path);
        }

        public static final GunDefinition EMERGENCY = GunDefinition.builtin(
                "pistol_magazine", "pistol_magazine_empty",
                8.0f, 4, 18, 35, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                18.0f, 25.0f, 5.0f, 0.0f, true, true,
                "pistol_fire", "pistol_reload", null, "BALLISTIC",
                true, 0.85f, 0.5f, 0.7f, 0.3f, 0.55f, false, 0.0f, null, 0, 0.85f,
                null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                null, 2, 0, false).withTwoHanded(false);

        public static final Map<Identifier, GunDefinition> ALL = buildAll();

        private static Map<Identifier, GunDefinition> buildAll() {
            Map<Identifier, GunDefinition> m = new HashMap<>();
            // wave-1 (mirrors old TGItems.java)
            m.put(id("pistol"), GunDefinition.builtin(
                    "pistol_magazine", "pistol_magazine_empty",
                    8.0f, 4, 18, 35, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                    18.0f, 25.0f, 5.0f, 0.0f, true, true,
                    "pistol_fire", "pistol_reload", null, "BALLISTIC",
                    true, 0.85f, 0.5f, 0.7f, 0.3f, 0.55f, false, 0.0f, null, 0, 0.85f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 2, 0, false).withTwoHanded(false));
            m.put(id("revolver"), GunDefinition.builtin(
                    "pistol_rounds", null,
                    8.0f, 6, 6, 45, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                    12.0f, 20.0f, 6.0f, 0.0f, true, false,
                    "revolver_fire", "revolver_reload", null, "BALLISTIC",
                    true, 0.85f, 0.5f, 1.0f, 0.4f, 0.6f, false, 0.0f, null, 0, 0.9f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 2, 0, false).withTwoHanded(false));
            m.put(id("ak47"), GunDefinition.builtin(
                    "assault_rifle_magazine", "assault_rifle_magazine_empty",
                    8.0f, 3, 30, 45, 60, 0.030f, 4.0f, 0.0, 1, 0.0f,
                    20.0f, 30.0f, 4.5f, 0.5f, false, true,
                    "ak47_fire", "ak47_reload", null, "BALLISTIC",
                    true, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 1, 0, false));
            m.put(id("m4"), GunDefinition.builtin(
                    "assault_rifle_magazine", "assault_rifle_magazine_empty",
                    11.0f, 3, 30, 45, 60, 0.015f, 4.0f, 0.0, 1, 0.0f,
                    25.0f, 40.0f, 7.0f, 0.5f, false, true,
                    "m4_fire", "m4_reload", null, "BALLISTIC",
                    true, 0.7f, 0.45f, 0.7f, 0.3f, 0.75f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 1, 0, false));
            m.put(id("thompson"), GunDefinition.builtin(
                    "smg_magazine", "smg_magazine_empty",
                    5.0f, 3, 20, 40, 40, 0.050f, 1.75f, 0.0, 1, 0.0f,
                    15.0f, 24.0f, 3.0f, 0.0f, false, true,
                    "thompson_fire", "thompson_reload", null, "BALLISTIC",
                    true, 0.8f, 0.5f, 0.6f, 0.35f, 0.6f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 1, 0, false));
            m.put(id("combat_shotgun"), GunDefinition.builtin(
                    "shotgun_rounds", null,
                    8.0f, 14, 8, 50, 15, 0.010f, 2.5f, 0.0, 8, 0.150f,
                    2.0f, 5.0f, 3.0f, 0.5f, true, false,
                    "combat_shotgun_fire", "combat_shotgun_reload", null, "BALLISTIC",
                    true, 0.8f, 0.5f, 2.8f, 0.8f, 0.9f, false, 0.0f, "combat_shotgun_pump", 14, 1.7f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 2, 0, false));
            m.put(id("minigun"), GunDefinition.builtin(
                    "minigun_drum", "minigun_drum_empty",
                    12.0f, 1, 200, 100, 75, 0.025f, 3.0f, 0.0, 1, 0.0f,
                    30.0f, 50.0f, 7.0f, 0.5f, false, true,
                    "minigun_fire", "minigun_reload", null, "BALLISTIC",
                    true, 0.85f, 0.6f, 0.3f, 0.2f, 0.8f, true, 0.0f, null, 0, 1.05f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 3, 0, false));
            m.put(id("teslagun"), GunDefinition.builtin(
                    "energy_cell", "energy_cell_empty",
                    9.0f, 8, 25, 45, 60, 0.0f, 80.0f, 0.0, 1, 0.0f,
                    0.0f, 0.0f, 9.0f, 0.0f, false, true,
                    "teslagun_fire", "teslagun_reload", null, "ELECTRIC",
                    true, 0.8f, 0.5f, 0.4f, 0.1f, 0.6f, false, 0.09f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 6, 0, false));
            m.put(id("bolt_action"), GunDefinition.builtin(
                    "rifle_rounds", null,
                    30.0f, 25, 6, 50, 90, 0.050f, 6.5f, 0.0, 1, 0.0f,
                    40.0f, 60.0f, 20.0f, 1.0f, true, false,
                    "bolt_action_fire", "bolt_action_reload", null, "BALLISTIC",
                    true, 0.35f, 0.25f, 2.2f, 0.5f, 0.9f, false, 0.0f, "bolt_action_rechamber", 20, 1.15f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 2, 0, false));
            m.put(id("biogun"), GunDefinition.builtin(
                    "bio_tank", "bio_tank_empty",
                    25.0f, 6, 30, 45, 40, 0.015f, 1.0f, 0.01, 1, 0.0f,
                    8.0f, 15.0f, 20.0f, 0.5f, false, true,
                    "biogun_fire", "biogun_reload", null, "BIO",
                    true, 0.8f, 0.5f, 0.8f, 0.3f, 0.7f, false, 0.34f, null, 0, 1.6f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 11, 0, false));
            m.put(id("flamethrower"), GunDefinition.builtin(
                    "fuel_tank", "fuel_tank_empty",
                    8.0f, 2, 100, 45, 22, 0.05f, 0.55f, 0.01, 1, 0.0f,
                    5.0f, 22.0f, 4.0f, 0.0f, false, true,
                    "flamethrower_fire", "flamethrower_reload", "flamethrower_start", "FIRE",
                    true, 0.8f, 0.6f, 0.5f, 0.25f, 0.8f, false, 0.0f, null, 0, 1.3f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 10, 10, false));
            // wave-2
            m.put(id("tfg"), GunDefinition.builtin(
                    "nuclear_powercell", "nuclear_powercell_empty",
                    70.0f, 12, 20, 45, 17, 0.015f, 6.0f, 0.0, 1, 0.0f,
                    8.0f, 15.0f, 30.0f, 2.0f, false, true,
                    "tfg_fire", "biogun_reload", "tfg_charge", "PLASMA",
                    true, 0.8f, 0.5f, 1.2f, 0.4f, 1.0f, false, 0.0f, null, 0, 2.0f,
                    new GunDefinition.ExplosiveDef(46.0f, 23.0f, 5.0f, 10.0f, 0.5f, false),
                    false, 60, 1, 5, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 9, 0, false));
            m.put(id("nucleardeathray"), GunDefinition.builtin(
                    "nuclear_powercell", "nuclear_powercell_empty",
                    5.0f, 5, 20, 50, 100, 0.0f, 1.0f, 0.0, 1, 0.0f,
                    11.0f, 11.0f, 5.0f, 1.0f, false, true,
                    "ndr_fire", "ndr_reload", "ndr_start", "BEAM",
                    true, 0.8f, 0.5f, 0.4f, 0.1f, 0.8f, false, 0.0f, null, 0, 1.0f,
                    null, true, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 40,
                    null, 8, 10, true));
            m.put(id("gaussrifle"), GunDefinition.builtin(
                    "gauss_magazine", "gauss_magazine_empty",
                    90.0f, 30, 8, 60, 18, 0.025f, 5.0f, 0.0, 1, 0.0f,
                    90.0f, 90.0f, 90.0f, 3.5f, true, true,
                    "gauss_fire", "gauss_reload", null, "GAUSS",
                    true, 0.35f, 0.0f, 2.4f, 0.5f, 0.9f, false, 0.0f, "gauss_rechamber", 12, 1.1f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 7, 0, false));
            m.put(id("rocketlauncher"), GunDefinition.builtin(
                    "rocket", null,
                    60.0f, 10, 1, 40, 200, 0.05f, 1.0f, 0.01, 1, 0.0f,
                    3.0f, 5.0f, 50.0f, 0.0f, true, false,
                    "rocket_fire", "rocket_reload", null, "EXPLOSIVE",
                    true, 0.8f, 0.6f, 2.0f, 0.5f, 1.0f, false, 0.0f, null, 0, 2.2f,
                    new GunDefinition.ExplosiveDef(60.0f, 50.0f, 3.0f, 5.0f, 0.5f, false),
                    false, 0, 1, 1, 3.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    List.of("rocket_nuke", "rocket_hv"), 2, 0, false));
            m.put(id("guidedmissilelauncher"), GunDefinition.builtin(
                    "rocket", null,
                    60.0f, 10, 1, 40, 100, 0.05f, 1.0f, 0.01, 1, 0.0f,
                    2.0f, 4.0f, 50.0f, 0.0f, true, false,
                    "guided_fire", "rocket_reload", null, "EXPLOSIVE",
                    true, 0.8f, 0.6f, 2.0f, 0.5f, 1.0f, false, 0.0f, null, 0, 2.2f,
                    new GunDefinition.ExplosiveDef(60.0f, 50.0f, 2.0f, 4.0f, 0.25f, false),
                    false, 0, 1, 1, 2.0f, false, 0.16f, true, 20, 80, 150.0, 0,
                    List.of("rocket_hv"), 2, 0, false));
            m.put(id("grimreaper"), GunDefinition.builtin(
                    "rocket", null,
                    60.0f, 6, 4, 40, 200, 0.05f, 1.0f, 0.01, 1, 0.0f,
                    3.0f, 5.0f, 50.0f, 0.0f, false, false,
                    "guided_fire", "rocket_reload", null, "EXPLOSIVE",
                    true, 0.8f, 0.6f, 1.6f, 0.4f, 1.0f, false, 0.0f, null, 0, 2.2f,
                    new GunDefinition.ExplosiveDef(60.0f, 50.0f, 3.0f, 5.0f, 0.25f, false),
                    false, 0, 1, 1, 2.0f, false, 0.16f, true, 20, 80, 150.0, 0,
                    List.of("rocket_hv"), 2, 0, false));
            m.put(id("sonicshotgun"), GunDefinition.builtin(
                    "energy_cell", "energy_cell_empty",
                    50.0f, 12, 8, 40, 20, 0.0f, 2.0f, 0.0, 16, 0.075f,
                    5.0f, 15.0f, 35.0f, 1.0f, true, true,
                    "sonic_fire", "sonic_reload", null, "SONIC",
                    true, 0.8f, 0.6f, 2.4f, 0.7f, 0.9f, false, 0.0f, null, 0, 1.4f,
                    null, false, 0, 1, 1, 2.0f, true, 0.0f, false, 20, 80, 150.0, 0,
                    null, 7, 0, false));
            m.put(id("lasergun"), GunDefinition.builtin(
                    "energy_cell", "energy_cell_empty",
                    16.0f, 5, 45, 45, 90, 0.0f, 1.0f, 0.0, 1, 0.0f,
                    90.0f, 90.0f, 16.0f, 0.0f, false, true,
                    "laser_fire", "laser_reload", null, "LASER",
                    true, 0.75f, 0.75f, 0.5f, 0.15f, 0.7f, false, 0.0f, null, 0, 1.0f,
                    null, true, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 5, 0, false));
            m.put(id("laserpistol"), GunDefinition.builtin(
                    "redstone_battery", "redstone_battery_empty",
                    9.0f, 6, 20, 40, 90, 0.025f, 1.0f, 0.0, 1, 0.0f,
                    90.0f, 90.0f, 9.0f, 0.0f, false, true,
                    "laserpistol_fire", "laserpistol_reload", null, "LASER",
                    true, 0.8f, 0.5f, 0.5f, 0.2f, 0.6f, false, 0.0f, null, 0, 0.9f,
                    null, true, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 5, 0, false).withTwoHanded(false));
            m.put(id("pulserifle"), GunDefinition.builtin(
                    "advanced_magazine", "advanced_magazine_empty",
                    18.0f, 7, 12, 45, 17, 0.024f, 4.5f, 0.0, 3, 0.015f,
                    30.0f, 45.0f, 15.0f, 1.0f, false, true,
                    "pulse_fire", "pulse_reload", null, "ADVANCED",
                    true, 0.35f, 0.5f, 0.8f, 0.3f, 0.75f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 4, 0, false));
            m.put(id("vector"), GunDefinition.builtin(
                    "smg_magazine", "smg_magazine_empty",
                    10.0f, 1, 25, 40, 20, 0.05f, 2.0f, 0.0, 1, 0.0f,
                    17.0f, 25.0f, 8.0f, 0.5f, false, true,
                    "vector_fire", "vector_reload", null, "BALLISTIC",
                    true, 0.75f, 0.35f, 0.5f, 0.3f, 0.65f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 1, 0, false));
            m.put(id("pdw"), GunDefinition.builtin(
                    "advanced_magazine", "advanced_magazine_empty",
                    10.0f, 1, 40, 40, 12, 0.03f, 3.5f, 0.0, 1, 0.0f,
                    18.0f, 25.0f, 7.0f, 1.0f, false, true,
                    "pdw_fire", "pdw_reload", null, "ADVANCED",
                    true, 0.8f, 0.5f, 0.4f, 0.25f, 0.6f, false, 0.0f, null, 0, 1.0f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    null, 4, 0, false));
            m.put(id("as50"), GunDefinition.builtin(
                    "as50_magazine", "as50_magazine_empty",
                    45.0f, 10, 10, 80, 14, 0.0625f, 6.5f, 0.0, 1, 0.0f,
                    40.0f, 60.0f, 35.0f, 2.0f, true, true,
                    "as50_fire", "as50_reload", null, "BALLISTIC",
                    true, 0.35f, 0.125f, 2.2f, 0.5f, 0.9f, false, 0.0f, null, 0, 1.15f,
                    null, false, 0, 1, 1, 0.0f, false, 0.0f, false, 20, 80, 150.0, 0,
                    List.of("as50_magazine_explosive", "as50_magazine_incendiary"), 1, 0, false));
            return Collections.unmodifiableMap(m);
        }
    }
}
