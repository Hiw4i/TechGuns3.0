package com.techguns.techguns3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Replaces 1.12 {@code techguns.TGConfig} (Forge {@code Configuration}) with NeoForge {@code ModConfigSpec}.
 *
 * <p>Only the MVP-relevant options are present. Machine spawn rates, radiation toggles,
 * dungeon weights etc. from the original 32k-line config come back in later waves.</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID)
public final class TGConfig {
    private TGConfig() {}

    private static final ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    // --- common ---
    private static final ModConfigSpec.DoubleValue GUN_DAMAGE_MULTIPLIER = COMMON
            .comment("Global damage multiplier for all Techguns firearms. 1.0 = vanilla Techguns balance.")
            .defineInRange("gunDamageMultiplier", 1.0, 0.1, 10.0);
    private static final ModConfigSpec.DoubleValue FX_DENSITY = COMMON
            .comment("Particle count multiplier for Techguns effects (sparks, smoke, embers).")
            .defineInRange("fxDensity", 1.0, 0.1, 1.0);
    private static final ModConfigSpec.BooleanValue BLOOD_ON_HIT = COMMON
            .comment("Spray blood when Techguns projectiles hit flesh. Turn off for a bloodless build.")
            .define("bloodOnHit", true);
    private static final ModConfigSpec.BooleanValue ENABLE_GORE = COMMON
            .comment("Master gore toggle: blood spray plus gibs (body chunks) on Techguns kills. "
                    + "Server-authoritative, applies to every viewer the same way.")
            .define("enableGore", true);
    private static final ModConfigSpec.DoubleValue GORE_CHANCE_MULTIPLIER = COMMON
            .comment("Scales every Techguns gib roll (1.0 = default Techguns balance, 0 disables gibs).")
            .defineInRange("goreChanceMultiplier", 1.0, 0.0, 2.0);
    private static final ModConfigSpec.IntValue MAX_GIBS = COMMON
            .comment("Cap of flying body chunks per gibbed kill (performance guard for miniguns).")
            .defineInRange("maxGibs", 10, 0, 12);

    private static final ModConfigSpec.BooleanValue GEN_COPPER = COMMON
            .comment("Generate TechGuns copper ore (replaces 1.12 EnumOreType.ORE_COPPER in basicore).")
            .define("generateCopperOre", true);
    private static final ModConfigSpec.BooleanValue GEN_TIN = COMMON
            .comment("Generate TechGuns tin ore.")
            .define("generateTinOre", true);
    private static final ModConfigSpec.BooleanValue GEN_LEAD = COMMON
            .comment("Generate TechGuns lead ore.")
            .define("generateLeadOre", true);
    private static final ModConfigSpec.BooleanValue GEN_URANIUM = COMMON
            .comment("Generate TechGuns uranium ore (light level 4 in 1.12).")
            .define("generateUraniumOre", true);
    private static final ModConfigSpec.BooleanValue GEN_TITANIUM = COMMON
            .comment("Generate TechGuns titanium ore.")
            .define("generateTitaniumOre", true);
    private static final ModConfigSpec.BooleanValue EXPLOSIONS_DAMAGE_BLOCKS = COMMON
            .comment("Wave-2 explosive guns (rockets, TFG, guided, AS50-explosive) damage blocks. "
                    + "Turn off to keep explosions entity-only (no grief).")
            .define("explosionsDamageBlocks", true);

    // --- armed mobs (Techguns NPCs) ---
    private static final ModConfigSpec.BooleanValue MOBS_ENABLED = COMMON
            .comment("Master switch for Techguns armed NPCs. When off, they spawn but stay passive.")
            .define("mobs.enabled", true);
    private static final ModConfigSpec.BooleanValue MOBS_AI_ENABLED = COMMON
            .comment("Run the Techguns combat AI (aim, cover, flank, reload). Off = vanilla stroll only.")
            .define("mobs.aiEnabled", true);
    private static final ModConfigSpec.BooleanValue MOBS_GRIEFING = COMMON
            .comment("Allow armed NPCs to break and place blocks. Own AI feature; the per-mob profile "
                    + "still has to enable mine_blocks / build.")
            .define("mobs.griefing", true);
    private static final ModConfigSpec.BooleanValue MOBS_DYNAMITE = COMMON
            .comment("Allow armed NPCs to place and detonate TNT (breaching walls, suicide rushes).")
            .define("mobs.dynamite", true);
    private static final ModConfigSpec.BooleanValue MOBS_PICKUP_WEAPONS = COMMON
            .comment("Allow armed NPCs to pick up dropped guns and upgrade their weapon.")
            .define("mobs.pickupWeapons", true);
    private static final ModConfigSpec.BooleanValue MOBS_KEEP_GUNS_ON_DEATH = COMMON
            .comment("NPCs always drop their firearm on death (otherwise the vanilla low drop chance applies).")
            .define("mobs.keepGunsOnDeath", true);
    private static final ModConfigSpec.DoubleValue MOBS_DIFFICULTY = COMMON
            .comment("Global mob AI difficulty multiplier. Scales accuracy and reaction speed, not health. "
                    + "1.0 = profile values, 0.5 = sloppy, 2.0 = ruthless.")
            .defineInRange("mobs.difficulty", 1.0, 0.25, 4.0);
    private static final ModConfigSpec.DoubleValue MOBS_SPAWN_RATE = COMMON
            .comment("Multiplier applied to the natural spawn weight of Techguns armed NPCs. "
                    + "0 disables natural spawning (spawn eggs still work).")
            .defineInRange("mobs.spawnRateMultiplier", 1.0, 0.0, 10.0);

    // --- client ---
    private static final ModConfigSpec.BooleanValue AMMO_HUD = CLIENT
            .comment("Show the TechGuns ammo HUD overlay (reworked HUD from CE, simplified in MVP).")
            .define("ammoHud", true);
    private static final ModConfigSpec.BooleanValue CAMERA_RECOIL = CLIENT
            .comment("Apply a small client-side camera kick when firing. Damage stays server-side.")
            .define("cameraRecoil", true);
    private static final ModConfigSpec.BooleanValue CINEMATIC_FX = CLIENT
            .comment("Cinematic muzzle/tracer treatment: baked jittered flash, short tracers, "
                    + "first-person guards so FX never fills the screen. Turn off for minimal FX.")
            .define("cinematicFx", true);
    private static final ModConfigSpec.BooleanValue SHELL_CASINGS = CLIENT
            .comment("Eject brass shell casings on every shot (visual only).")
            .define("shellCasings", true);
    private static final ModConfigSpec.DoubleValue ADS_POSE = CLIENT
            .comment("How far the weapon comes up to the shoulder while aiming (RMB). "
                    + "0 disables the aim pose, 1 is the default, 2 is a very high mount.")
            .defineInRange("adsPose", 1.0, 0.0, 2.0);
    private static final ModConfigSpec.DoubleValue WEAPON_SWAY = CLIENT
            .comment("Mouse sway of the held weapon: the model keeps following the camera for "
                    + "a moment after the mouse moved. 0 disables it.")
            .defineInRange("weaponSway", 1.0, 0.0, 3.0);
    private static final ModConfigSpec.DoubleValue WEAPON_BOB = CLIENT
            .comment("Walk bob of the held weapon. 0 disables it.")
            .defineInRange("weaponBob", 1.0, 0.0, 3.0);
    private static final ModConfigSpec.BooleanValue ENHANCED_FX = CLIENT
            .comment("Use the own effect shaders (soft edges on glows, fog that dims the light "
                    + "instead of tinting it). Turn off to fall back to the vanilla core shaders.")
            .define("enhancedFx", true);

    static final ModConfigSpec COMMON_SPEC = COMMON.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

    // Volatile: reloaded on the config thread, read from gameplay/render threads.
    private static volatile double gunDamageMultiplier = 1.0;
    private static volatile double fxDensity = 1.0;
    private static volatile boolean bloodOnHit = true;
    private static volatile boolean enableGore = true;
    private static volatile double goreChanceMultiplier = 1.0;
    private static volatile int maxGibs = 10;
    private static volatile boolean generateCopper = true;
    private static volatile boolean generateTin = true;
    private static volatile boolean generateLead = true;
    private static volatile boolean generateUranium = true;
    private static volatile boolean generateTitanium = true;
    private static volatile boolean explosionsDamageBlocks = true;
    private static volatile boolean mobsEnabled = true;
    private static volatile boolean mobsAiEnabled = true;
    private static volatile boolean mobsGriefing = true;
    private static volatile boolean mobsDynamite = true;
    private static volatile boolean mobsPickupWeapons = true;
    private static volatile boolean mobsKeepGunsOnDeath = true;
    private static volatile double mobsDifficulty = 1.0;
    private static volatile double mobsSpawnRate = 1.0;
    private static volatile boolean ammoHud = true;
    private static volatile boolean cameraRecoil = true;
    private static volatile boolean cinematicFx = true;
    private static volatile boolean shellCasings = true;
    private static volatile double adsPose = 1.0;
    private static volatile double weaponSway = 1.0;
    private static volatile double weaponBob = 1.0;
    private static volatile boolean enhancedFx = true;

    public static double gunDamageMultiplier() { return gunDamageMultiplier; }
    public static double fxDensity() { return fxDensity; }
    public static boolean bloodOnHit() { return bloodOnHit; }
    public static boolean goreEnabled() { return enableGore; }
    public static double goreChanceMultiplier() { return goreChanceMultiplier; }
    public static int maxGibs() { return maxGibs; }
    public static boolean generateCopper() { return generateCopper; }
    public static boolean generateTin() { return generateTin; }
    public static boolean generateLead() { return generateLead; }
    public static boolean generateUranium() { return generateUranium; }
    public static boolean generateTitanium() { return generateTitanium; }
    public static boolean explosionsDamageBlocks() { return explosionsDamageBlocks; }
    public static boolean mobsEnabled() { return mobsEnabled; }
    public static boolean mobsAiEnabled() { return mobsAiEnabled && mobsEnabled; }
    public static boolean mobsGriefing() { return mobsGriefing; }
    public static boolean mobsDynamite() { return mobsDynamite; }
    public static boolean mobsPickupWeapons() { return mobsPickupWeapons; }
    public static boolean mobsKeepGunsOnDeath() { return mobsKeepGunsOnDeath; }
    public static double mobsDifficulty() { return mobsDifficulty; }
    public static double mobsSpawnRate() { return mobsSpawnRate; }
    public static boolean ammoHud() { return ammoHud; }
    public static boolean cameraRecoil() { return cameraRecoil; }
    public static boolean cinematicFx() { return cinematicFx; }
    public static boolean shellCasings() { return shellCasings; }
    public static double adsPose() { return adsPose; }
    public static double weaponSway() { return weaponSway; }
    public static double weaponBob() { return weaponBob; }
    public static boolean enhancedFx() { return enhancedFx; }

    // Loading/Reloading only: the base ModConfigEvent also covers Unloading,
    // where the spec is already torn down and .get() would throw on halt.
    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        apply(event);
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        apply(event);
    }

    private static void apply(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            gunDamageMultiplier = GUN_DAMAGE_MULTIPLIER.get();
            fxDensity = FX_DENSITY.get();
            bloodOnHit = BLOOD_ON_HIT.get();
            enableGore = ENABLE_GORE.get();
            goreChanceMultiplier = GORE_CHANCE_MULTIPLIER.get();
            maxGibs = MAX_GIBS.get();
            generateCopper = GEN_COPPER.get();
            generateTin = GEN_TIN.get();
            generateLead = GEN_LEAD.get();
            generateUranium = GEN_URANIUM.get();
            generateTitanium = GEN_TITANIUM.get();
            explosionsDamageBlocks = EXPLOSIONS_DAMAGE_BLOCKS.get();
            mobsEnabled = MOBS_ENABLED.get();
            mobsAiEnabled = MOBS_AI_ENABLED.get();
            mobsGriefing = MOBS_GRIEFING.get();
            mobsDynamite = MOBS_DYNAMITE.get();
            mobsPickupWeapons = MOBS_PICKUP_WEAPONS.get();
            mobsKeepGunsOnDeath = MOBS_KEEP_GUNS_ON_DEATH.get();
            mobsDifficulty = MOBS_DIFFICULTY.get();
            mobsSpawnRate = MOBS_SPAWN_RATE.get();
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            ammoHud = AMMO_HUD.get();
            cameraRecoil = CAMERA_RECOIL.get();
            cinematicFx = CINEMATIC_FX.get();
            shellCasings = SHELL_CASINGS.get();
            adsPose = ADS_POSE.get();
            weaponSway = WEAPON_SWAY.get();
            weaponBob = WEAPON_BOB.get();
            enhancedFx = ENHANCED_FX.get();
        }
    }
}
