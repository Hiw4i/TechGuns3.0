package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.AnimatedGunItem;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items. Gun tunables live in {@code data/techguns3/guns/*.json}
 * (see {@code item.GunDefinition} + {@code resource.GunDataLoader});
 * here each gun only carries its id, default ammo and emergency sound
 * fallbacks used when its JSON entry is missing.
 *
 * <p>Ammo model: magazine-fed guns reload one magazine item into a
 * full mag and return an empty magazine; round-fed guns consume loose rounds.</p>
 */
public final class TGItems {
    private TGItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TechGuns3.MODID);

    private static Identifier gunId(String path) {
        return Identifier.fromNamespaceAndPath(TechGuns3.MODID, path);
    }

    // --- wave-1 ammo (declared first: guns hold suppliers to these) ---
    public static final DeferredItem<Item> PISTOL_ROUNDS = ITEMS.registerSimpleItem("pistol_rounds");
    public static final DeferredItem<Item> RIFLE_ROUNDS = ITEMS.registerSimpleItem("rifle_rounds");
    public static final DeferredItem<Item> SHOTGUN_ROUNDS = ITEMS.registerSimpleItem("shotgun_rounds");
    public static final DeferredItem<Item> PISTOL_MAGAZINE = ITEMS.registerSimpleItem("pistol_magazine");
    public static final DeferredItem<Item> ASSAULT_RIFLE_MAGAZINE = ITEMS.registerSimpleItem("assault_rifle_magazine");
    public static final DeferredItem<Item> SMG_MAGAZINE = ITEMS.registerSimpleItem("smg_magazine");
    public static final DeferredItem<Item> PISTOL_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("pistol_magazine_empty");
    public static final DeferredItem<Item> ASSAULT_RIFLE_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("assault_rifle_magazine_empty");
    public static final DeferredItem<Item> SMG_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("smg_magazine_empty");
    public static final DeferredItem<Item> MINIGUN_DRUM = ITEMS.registerSimpleItem("minigun_drum");
    public static final DeferredItem<Item> MINIGUN_DRUM_EMPTY = ITEMS.registerSimpleItem("minigun_drum_empty");
    public static final DeferredItem<Item> ENERGY_CELL = ITEMS.registerSimpleItem("energy_cell");
    public static final DeferredItem<Item> ENERGY_CELL_EMPTY = ITEMS.registerSimpleItem("energy_cell_empty");
    public static final DeferredItem<Item> BIO_TANK = ITEMS.registerSimpleItem("bio_tank");
    public static final DeferredItem<Item> BIO_TANK_EMPTY = ITEMS.registerSimpleItem("bio_tank_empty");
    public static final DeferredItem<Item> FUEL_TANK = ITEMS.registerSimpleItem("fuel_tank");
    public static final DeferredItem<Item> FUEL_TANK_EMPTY = ITEMS.registerSimpleItem("fuel_tank_empty");
    // --- wave-2 ammo (nuclear cell, rockets, advanced/as50/gauss, redstone battery) ---
    public static final DeferredItem<Item> NUCLEAR_POWERCELL = ITEMS.registerSimpleItem("nuclear_powercell");
    public static final DeferredItem<Item> NUCLEAR_POWERCELL_EMPTY = ITEMS.registerSimpleItem("nuclear_powercell_empty");
    public static final DeferredItem<Item> ROCKET = ITEMS.registerSimpleItem("rocket");
    public static final DeferredItem<Item> ROCKET_NUKE = ITEMS.registerSimpleItem("rocket_nuke");
    public static final DeferredItem<Item> ROCKET_HV = ITEMS.registerSimpleItem("rocket_hv");
    public static final DeferredItem<Item> ADVANCED_MAGAZINE = ITEMS.registerSimpleItem("advanced_magazine");
    public static final DeferredItem<Item> ADVANCED_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("advanced_magazine_empty");
    public static final DeferredItem<Item> ADVANCED_ROUNDS = ITEMS.registerSimpleItem("advanced_rounds");
    public static final DeferredItem<Item> AS50_MAGAZINE = ITEMS.registerSimpleItem("as50_magazine");
    public static final DeferredItem<Item> AS50_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("as50_magazine_empty");
    public static final DeferredItem<Item> AS50_MAGAZINE_INCENDIARY = ITEMS.registerSimpleItem("as50_magazine_incendiary");
    public static final DeferredItem<Item> AS50_MAGAZINE_EXPLOSIVE = ITEMS.registerSimpleItem("as50_magazine_explosive");
    public static final DeferredItem<Item> SNIPER_ROUNDS = ITEMS.registerSimpleItem("sniper_rounds");
    public static final DeferredItem<Item> SNIPER_ROUNDS_INCENDIARY = ITEMS.registerSimpleItem("sniper_rounds_incendiary");
    public static final DeferredItem<Item> SNIPER_ROUNDS_EXPLOSIVE = ITEMS.registerSimpleItem("sniper_rounds_explosive");
    public static final DeferredItem<Item> GAUSS_SLUGS = ITEMS.registerSimpleItem("gauss_slugs");
    public static final DeferredItem<Item> GAUSS_MAGAZINE = ITEMS.registerSimpleItem("gauss_magazine");
    public static final DeferredItem<Item> GAUSS_MAGAZINE_EMPTY = ITEMS.registerSimpleItem("gauss_magazine_empty");
    public static final DeferredItem<Item> REDSTONE_BATTERY = ITEMS.registerSimpleItem("redstone_battery");
    public static final DeferredItem<Item> REDSTONE_BATTERY_EMPTY = ITEMS.registerSimpleItem("redstone_battery_empty");
    public static final DeferredItem<Item> TURRET_ARMOR_IRON = ITEMS.registerItem("turret_armor_iron",
            props -> new Item(props.durability(256)));

    // --- wave-1 ingots (smelted from the new ores; copper uses the vanilla chain) ---
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.registerSimpleItem("titanium_ingot");
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.registerSimpleItem("uranium_ingot");

    // --- guns: thin shells, tunables in data/techguns3/guns/*.json ---
    // Third arg = default ammo, last two = emergency sound fallbacks.
    public static final DeferredItem<AnimatedGunItem> PISTOL = ITEMS.registerItem("pistol",
            props -> new AnimatedGunItem(props, gunId("pistol"),
                    PISTOL_MAGAZINE::get, "pistol_magazine",
                    TGSounds.PISTOL_FIRE::get, TGSounds.PISTOL_RELOAD::get));
    public static final DeferredItem<GenericGunItem> REVOLVER = ITEMS.registerItem("revolver",
            props -> new GenericGunItem(props, gunId("revolver"),
                    PISTOL_ROUNDS::get, "pistol_rounds",
                    TGSounds.REVOLVER_FIRE::get, TGSounds.REVOLVER_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> AK47 = ITEMS.registerItem("ak47",
            props -> new AnimatedGunItem(props, gunId("ak47"),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine",
                    TGSounds.AK47_FIRE::get, TGSounds.AK47_RELOAD::get));
    public static final DeferredItem<GenericGunItem> M4 = ITEMS.registerItem("m4",
            props -> new GenericGunItem(props, gunId("m4"),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine",
                    TGSounds.M4_FIRE::get, TGSounds.M4_RELOAD::get));
    public static final DeferredItem<GenericGunItem> THOMPSON = ITEMS.registerItem("thompson",
            props -> new GenericGunItem(props, gunId("thompson"),
                    SMG_MAGAZINE::get, "smg_magazine",
                    TGSounds.THOMPSON_FIRE::get, TGSounds.THOMPSON_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> COMBAT_SHOTGUN = ITEMS.registerItem("combat_shotgun",
            props -> new AnimatedGunItem(props, gunId("combat_shotgun"),
                    SHOTGUN_ROUNDS::get, "shotgun_rounds",
                    TGSounds.COMBAT_SHOTGUN_FIRE::get, TGSounds.COMBAT_SHOTGUN_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> MINIGUN = ITEMS.registerItem("minigun",
            props -> new AnimatedGunItem(props, gunId("minigun"),
                    MINIGUN_DRUM::get, "minigun_drum",
                    TGSounds.MINIGUN_FIRE::get, TGSounds.MINIGUN_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> TESLAGUN = ITEMS.registerItem("teslagun",
            props -> new AnimatedGunItem(props, gunId("teslagun"),
                    ENERGY_CELL::get, "energy_cell",
                    TGSounds.TESLAGUN_FIRE::get, TGSounds.TESLAGUN_RELOAD::get));
    public static final DeferredItem<GenericGunItem> BOLT_ACTION = ITEMS.registerItem("bolt_action",
            props -> new GenericGunItem(props, gunId("bolt_action"),
                    RIFLE_ROUNDS::get, "rifle_rounds",
                    TGSounds.BOLT_ACTION_FIRE::get, TGSounds.BOLT_ACTION_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> BIOGUN = ITEMS.registerItem("biogun",
            props -> new AnimatedGunItem(props, gunId("biogun"),
                    BIO_TANK::get, "bio_tank",
                    TGSounds.BIOGUN_FIRE::get, TGSounds.BIOGUN_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> FLAMETHROWER = ITEMS.registerItem("flamethrower",
            props -> new AnimatedGunItem(props, gunId("flamethrower"),
                    FUEL_TANK::get, "fuel_tank",
                    TGSounds.FLAMETHROWER_FIRE::get, TGSounds.FLAMETHROWER_RELOAD::get));

    // --- wave-2 exotic guns ---
    public static final DeferredItem<AnimatedGunItem> TFG = ITEMS.registerItem("tfg",
            props -> new AnimatedGunItem(props, gunId("tfg"),
                    NUCLEAR_POWERCELL::get, "nuclear_powercell",
                    TGSounds.TFG_FIRE::get, TGSounds.BIOGUN_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> NUCLEARDEATHRAY = ITEMS.registerItem("nucleardeathray",
            props -> new AnimatedGunItem(props, gunId("nucleardeathray"),
                    NUCLEAR_POWERCELL::get, "nuclear_powercell",
                    TGSounds.NDR_FIRE::get, TGSounds.NDR_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> GAUSSRIFLE = ITEMS.registerItem("gaussrifle",
            props -> new AnimatedGunItem(props, gunId("gaussrifle"),
                    GAUSS_MAGAZINE::get, "gauss_magazine",
                    TGSounds.GAUSS_FIRE::get, TGSounds.GAUSS_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> ROCKETLAUNCHER = ITEMS.registerItem("rocketlauncher",
            props -> new AnimatedGunItem(props, gunId("rocketlauncher"),
                    ROCKET::get, "rocket",
                    TGSounds.ROCKET_FIRE::get, TGSounds.ROCKET_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> GUIDEDMISSILELAUNCHER = ITEMS.registerItem("guidedmissilelauncher",
            props -> new AnimatedGunItem(props, gunId("guidedmissilelauncher"),
                    ROCKET::get, "rocket",
                    TGSounds.GUIDED_FIRE::get, TGSounds.ROCKET_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> GRIMREAPER = ITEMS.registerItem("grimreaper",
            props -> new AnimatedGunItem(props, gunId("grimreaper"),
                    ROCKET::get, "rocket",
                    TGSounds.GUIDED_FIRE::get, TGSounds.ROCKET_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> SONICSHOTGUN = ITEMS.registerItem("sonicshotgun",
            props -> new AnimatedGunItem(props, gunId("sonicshotgun"),
                    ENERGY_CELL::get, "energy_cell",
                    TGSounds.SONIC_FIRE::get, TGSounds.SONIC_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> LASERGUN = ITEMS.registerItem("lasergun",
            props -> new AnimatedGunItem(props, gunId("lasergun"),
                    ENERGY_CELL::get, "energy_cell",
                    TGSounds.LASER_FIRE::get, TGSounds.LASER_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> LASERPISTOL = ITEMS.registerItem("laserpistol",
            props -> new AnimatedGunItem(props, gunId("laserpistol"),
                    REDSTONE_BATTERY::get, "redstone_battery",
                    TGSounds.LASERPISTOL_FIRE::get, TGSounds.LASERPISTOL_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> PULSERIFLE = ITEMS.registerItem("pulserifle",
            props -> new AnimatedGunItem(props, gunId("pulserifle"),
                    ADVANCED_MAGAZINE::get, "advanced_magazine",
                    TGSounds.PULSE_FIRE::get, TGSounds.PULSE_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> VECTOR = ITEMS.registerItem("vector",
            props -> new AnimatedGunItem(props, gunId("vector"),
                    SMG_MAGAZINE::get, "smg_magazine",
                    TGSounds.VECTOR_FIRE::get, TGSounds.VECTOR_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> PDW = ITEMS.registerItem("pdw",
            props -> new AnimatedGunItem(props, gunId("pdw"),
                    ADVANCED_MAGAZINE::get, "advanced_magazine",
                    TGSounds.PDW_FIRE::get, TGSounds.PDW_RELOAD::get));
    public static final DeferredItem<AnimatedGunItem> AS50 = ITEMS.registerItem("as50",
            props -> new AnimatedGunItem(props, gunId("as50"),
                    AS50_MAGAZINE::get, "as50_magazine",
                    TGSounds.AS50_FIRE::get, TGSounds.AS50_RELOAD::get));
}
