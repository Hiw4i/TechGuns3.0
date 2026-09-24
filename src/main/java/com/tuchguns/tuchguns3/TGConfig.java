package com.tuchguns.tuchguns3;

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
@EventBusSubscriber(modid = TuchGuns3.MODID)
public final class TGConfig {
    private TGConfig() {}

    private static final ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    // --- common ---
    private static final ModConfigSpec.DoubleValue GUN_DAMAGE_MULTIPLIER = COMMON
            .comment("Global damage multiplier for all TuchGuns firearms. 1.0 = vanilla Techguns balance.")
            .defineInRange("gunDamageMultiplier", 1.0, 0.1, 10.0);

    private static final ModConfigSpec.BooleanValue GEN_COPPER = COMMON
            .comment("Generate TuchGuns copper ore (replaces 1.12 EnumOreType.ORE_COPPER in basicore).")
            .define("generateCopperOre", true);
    private static final ModConfigSpec.BooleanValue GEN_TIN = COMMON
            .comment("Generate TuchGuns tin ore.")
            .define("generateTinOre", true);
    private static final ModConfigSpec.BooleanValue GEN_LEAD = COMMON
            .comment("Generate TuchGuns lead ore.")
            .define("generateLeadOre", true);
    private static final ModConfigSpec.BooleanValue GEN_URANIUM = COMMON
            .comment("Generate TuchGuns uranium ore (light level 4 in 1.12).")
            .define("generateUraniumOre", true);
    private static final ModConfigSpec.BooleanValue GEN_TITANIUM = COMMON
            .comment("Generate TuchGuns titanium ore.")
            .define("generateTitaniumOre", true);

    // --- client ---
    private static final ModConfigSpec.BooleanValue AMMO_HUD = CLIENT
            .comment("Show the TuchGuns ammo HUD overlay (reworked HUD from CE, simplified in MVP).")
            .define("ammoHud", true);

    static final ModConfigSpec COMMON_SPEC = COMMON.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

    private static double gunDamageMultiplier = 1.0;
    private static boolean generateCopper = true;
    private static boolean generateTin = true;
    private static boolean generateLead = true;
    private static boolean generateUranium = true;
    private static boolean generateTitanium = true;
    private static boolean ammoHud = true;

    public static double gunDamageMultiplier() { return gunDamageMultiplier; }
    public static boolean generateCopper() { return generateCopper; }
    public static boolean generateTin() { return generateTin; }
    public static boolean generateLead() { return generateLead; }
    public static boolean generateUranium() { return generateUranium; }
    public static boolean generateTitanium() { return generateTitanium; }
    public static boolean ammoHud() { return ammoHud; }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            gunDamageMultiplier = GUN_DAMAGE_MULTIPLIER.get();
            generateCopper = GEN_COPPER.get();
            generateTin = GEN_TIN.get();
            generateLead = GEN_LEAD.get();
            generateUranium = GEN_URANIUM.get();
            generateTitanium = GEN_TITANIUM.get();
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            ammoHud = AMMO_HUD.get();
        }
    }
}
