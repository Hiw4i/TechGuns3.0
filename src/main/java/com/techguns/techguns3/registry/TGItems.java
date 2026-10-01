package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.item.GunExtras;
import com.techguns.techguns3.item.AnimatedGunItem;
import com.techguns.techguns3.item.GunStats;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MVP wave-1 items. Gun tunables are the 1.12 {@code GenericGun(...)} constructor args
 * (damage / minFiretime / clipsize / reloadtime / TTL / accuracy / speed / damage drop /
 * penetration), so balance survives the port and later waves only re-add behaviour.
 *
 * <p>Ammo model for the first slice: magazine-fed guns reload one magazine item into a
 * full mag and return an empty magazine; round-fed guns consume loose rounds.
 * The Ammo Press and incendiary variants return with the machine wave.</p>
 */
public final class TGItems {
    private TGItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TechGuns3.MODID);

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

    // --- wave-1 guns (all plain GenericProjectile shooters in 1.12, no charge/loop logic) ---
    // Modern feel values per gun: canZoom, zoomFov, zoomSpreadMult, recoilPitchDeg,
    // recoilYawJitter, muzzleFlashScale, spinsBarrels. LMB = fire, RMB = zoom, R = reload.
    public static final DeferredItem<AnimatedGunItem> PISTOL = ITEMS.registerItem("pistol",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 4, 18, 35, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                            18.0f, 25.0f, 5.0f, 0.0f, true, true,
                            TGSounds.PISTOL_FIRE::get, TGSounds.PISTOL_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.85f, 0.5f, 0.7f, 0.3f, 0.55f, false, 0.0f, null, 0, 0.85f),
                    PISTOL_MAGAZINE::get, "pistol_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_GUN)));
    public static final DeferredItem<GenericGunItem> REVOLVER = ITEMS.registerItem("revolver",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 6, 6, 45, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                            12.0f, 20.0f, 6.0f, 0.0f, true, false,
                            TGSounds.REVOLVER_FIRE::get, TGSounds.REVOLVER_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.85f, 0.5f, 1.0f, 0.4f, 0.6f, false, 0.0f, null, 0, 0.9f),
                    PISTOL_ROUNDS::get, "pistol_rounds",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_GUN)));
    public static final DeferredItem<AnimatedGunItem> AK47 = ITEMS.registerItem("ak47",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 3, 30, 45, 60, 0.030f, 4.0f, 0.0, 1, 0.0f,
                            20.0f, 30.0f, 4.5f, 0.5f, false, true,
                            TGSounds.AK47_FIRE::get, TGSounds.AK47_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, false, 0.0f, null, 0, 1.0f),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_RIFLE)));
    public static final DeferredItem<GenericGunItem> M4 = ITEMS.registerItem("m4",
            props -> new GenericGunItem(props,
                    new GunStats(11.0f, 3, 30, 45, 60, 0.015f, 4.0f, 0.0, 1, 0.0f,
                            25.0f, 40.0f, 7.0f, 0.5f, false, true,
                            TGSounds.M4_FIRE::get, TGSounds.M4_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.7f, 0.45f, 0.7f, 0.3f, 0.75f, false, 0.0f, null, 0, 1.0f),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_RIFLE)));
    public static final DeferredItem<GenericGunItem> THOMPSON = ITEMS.registerItem("thompson",
            props -> new GenericGunItem(props,
                    new GunStats(5.0f, 3, 20, 40, 40, 0.050f, 1.75f, 0.0, 1, 0.0f,
                            15.0f, 24.0f, 3.0f, 0.0f, false, true,
                            TGSounds.THOMPSON_FIRE::get, TGSounds.THOMPSON_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.8f, 0.5f, 0.6f, 0.35f, 0.6f, false, 0.0f, null, 0, 1.0f),
                    SMG_MAGAZINE::get, "smg_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_RIFLE)));
    public static final DeferredItem<AnimatedGunItem> COMBAT_SHOTGUN = ITEMS.registerItem("combat_shotgun",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 14, 8, 50, 15, 0.010f, 2.5f, 0.0, 8, 0.150f,
                            2.0f, 5.0f, 3.0f, 0.5f, true, false,
                            TGSounds.COMBAT_SHOTGUN_FIRE::get, TGSounds.COMBAT_SHOTGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.8f, 0.5f, 2.8f, 0.8f, 0.9f, false, 0.0f, null, 0, 1.7f)
                            .withAction(TGSounds.COMBAT_SHOTGUN_PUMP::get, 14),
                    SHOTGUN_ROUNDS::get, "shotgun_rounds",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_GUN)));
    public static final DeferredItem<AnimatedGunItem> MINIGUN = ITEMS.registerItem("minigun",
            props -> new AnimatedGunItem(props,
                    new GunStats(12.0f, 1, 200, 100, 75, 0.025f, 3.0f, 0.0, 1, 0.0f,
                            30.0f, 50.0f, 7.0f, 0.5f, false, true,
                            TGSounds.MINIGUN_FIRE::get, TGSounds.MINIGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.85f, 0.6f, 0.3f, 0.2f, 0.8f, true, 0.0f, null, 0, 1.05f),
                    MINIGUN_DRUM::get, "minigun_drum",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_MINIGUN)));
    public static final DeferredItem<AnimatedGunItem> TESLAGUN = ITEMS.registerItem("teslagun",
            props -> new AnimatedGunItem(props,
                    new GunStats(9.0f, 8, 25, 45, 60, 0.0f, 80.0f, 0.0, 1, 0.0f,
                            0.0f, 0.0f, 9.0f, 0.0f, false, true,
                            TGSounds.TESLAGUN_FIRE::get, TGSounds.TESLAGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.ELECTRIC,
                            true, 0.8f, 0.5f, 0.4f, 0.1f, 0.6f, false, 0.09f, null, 0, 1.0f),
                    ENERGY_CELL::get, "energy_cell",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_LIGHTNING)));
    public static final DeferredItem<GenericGunItem> BOLT_ACTION = ITEMS.registerItem("bolt_action",
            props -> new GenericGunItem(props,
                    new GunStats(30.0f, 25, 6, 50, 90, 0.050f, 6.5f, 0.0, 1, 0.0f,
                            40.0f, 60.0f, 20.0f, 1.0f, true, false,
                            TGSounds.BOLT_ACTION_FIRE::get, TGSounds.BOLT_ACTION_RELOAD::get,
                            null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.35f, 0.25f, 2.2f, 0.5f, 0.9f, false, 0.0f, null, 0, 1.15f)
                            .withAction(TGSounds.BOLT_ACTION_RECHAMBER::get, 20),
                    RIFLE_ROUNDS::get, "rifle_rounds",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_GUN)));
    public static final DeferredItem<AnimatedGunItem> BIOGUN = ITEMS.registerItem("biogun",
            props -> new AnimatedGunItem(props,
                    new GunStats(25.0f, 6, 30, 45, 40, 0.015f, 1.0f, 0.01, 1, 0.0f,
                            8.0f, 15.0f, 20.0f, 0.5f, false, true,
                            TGSounds.BIOGUN_FIRE::get, TGSounds.BIOGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.BIO,
                            true, 0.8f, 0.5f, 0.8f, 0.3f, 0.7f, false, 0.34f, null, 0, 1.6f),
                    BIO_TANK::get, "bio_tank",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_GREENFLARE)));
    public static final DeferredItem<AnimatedGunItem> FLAMETHROWER = ITEMS.registerItem("flamethrower",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 2, 100, 45, 22, 0.05f, 0.55f, 0.01, 1, 0.0f,
                            5.0f, 22.0f, 4.0f, 0.0f, false, true,
                            TGSounds.FLAMETHROWER_FIRE::get, TGSounds.FLAMETHROWER_RELOAD::get, TGSounds.FLAMETHROWER_START::get,
                            GunStats.ProjectileKind.FIRE,
                            true, 0.8f, 0.6f, 0.5f, 0.25f, 0.8f, false, 0.0f, null, 0, 1.3f),
                    FUEL_TANK::get, "fuel_tank",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_FLAME).withLoop(10)));

    // --- wave-2 exotic guns (tunables 1:1 from Techguns2-CE TGuns.java) ---
    // TFG: charged green plasma, size 1..5, ammo 1..5 per shot.
    public static final DeferredItem<AnimatedGunItem> TFG = ITEMS.registerItem("tfg",
            props -> new AnimatedGunItem(props,
                    new GunStats(70.0f, 12, 20, 45, 17, 0.015f, 6.0f, 0.0, 1, 0.0f,
                            8.0f, 15.0f, 30.0f, 2.0f, false, true,
                            TGSounds.TFG_FIRE::get, TGSounds.BIOGUN_RELOAD::get, TGSounds.TFG_CHARGE::get,
                            GunStats.ProjectileKind.PLASMA,
                            true, 0.8f, 0.5f, 1.2f, 0.4f, 1.0f, false, 0.0f, null, 0, 2.0f),
                    NUCLEAR_POWERCELL::get, "nuclear_powercell",
                    GunExtras.explosive(46.0f, 23.0f, 5.0f, 10.0f, 0.5f).withCharge(60, 1, 5).withMuzzle(GunExtras.FLASH_TFG)));
    // Nuclear Death Ray: sustained radiation beam.
    public static final DeferredItem<AnimatedGunItem> NUCLEARDEATHRAY = ITEMS.registerItem("nucleardeathray",
            props -> new AnimatedGunItem(props,
                    new GunStats(5.0f, 5, 20, 50, 100, 0.0f, 1.0f, 0.0, 1, 0.0f,
                            11.0f, 11.0f, 5.0f, 1.0f, false, true,
                            TGSounds.NDR_FIRE::get, TGSounds.NDR_RELOAD::get, TGSounds.NDR_START::get,
                            GunStats.ProjectileKind.BEAM,
                            true, 0.8f, 0.5f, 0.4f, 0.1f, 0.8f, false, 0.0f, null, 0, 1.0f),
                    NUCLEAR_POWERCELL::get, "nuclear_powercell",
                    GunExtras.DEFAULT.withBeam(40).withMuzzle(GunExtras.FLASH_NUKEBEAM).withLoop(10).withContinuous()));
    // Gauss rifle: 90-damage railgun, 2.86x scope with zero spread when zoomed, bolt rechamber.
    public static final DeferredItem<AnimatedGunItem> GAUSSRIFLE = ITEMS.registerItem("gaussrifle",
            props -> new AnimatedGunItem(props,
                    new GunStats(90.0f, 30, 8, 60, 18, 0.025f, 5.0f, 0.0, 1, 0.0f,
                            90.0f, 90.0f, 90.0f, 3.5f, true, true,
                            TGSounds.GAUSS_FIRE::get, TGSounds.GAUSS_RELOAD::get, null,
                            GunStats.ProjectileKind.GAUSS,
                            true, 0.35f, 0.0f, 2.4f, 0.5f, 0.9f, false, 0.0f, null, 0, 1.1f)
                            .withAction(TGSounds.GAUSS_RECHAMBER::get, 12),
                    GAUSS_MAGAZINE::get, "gauss_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_SONIC)));
    // Rocket launcher: loose rockets, auto-selects nuke > hv > normal.
    public static final DeferredItem<AnimatedGunItem> ROCKETLAUNCHER = ITEMS.registerItem("rocketlauncher",
            props -> new AnimatedGunItem(props,
                    new GunStats(60.0f, 10, 1, 40, 200, 0.05f, 1.0f, 0.01, 1, 0.0f,
                            3.0f, 5.0f, 50.0f, 0.0f, true, false,
                            TGSounds.ROCKET_FIRE::get, TGSounds.ROCKET_RELOAD::get, null,
                            GunStats.ProjectileKind.EXPLOSIVE,
                            true, 0.8f, 0.6f, 2.0f, 0.5f, 1.0f, false, 0.0f, null, 0, 2.2f),
                    ROCKET::get, "rocket",
                    GunExtras.explosive(60.0f, 50.0f, 3.0f, 5.0f, 0.5f)
                            .withKnockback(3.0f, false).withExtraAmmo("rocket_nuke", "rocket_hv").withMuzzle(GunExtras.FLASH_GUN)));
    // Guided missile launcher: lock-on 20/80/150, fires on press with snap lock.
    public static final DeferredItem<AnimatedGunItem> GUIDEDMISSILELAUNCHER = ITEMS.registerItem("guidedmissilelauncher",
            props -> new AnimatedGunItem(props,
                    new GunStats(60.0f, 10, 1, 40, 100, 0.05f, 1.0f, 0.01, 1, 0.0f,
                            2.0f, 4.0f, 50.0f, 0.0f, true, false,
                            TGSounds.GUIDED_FIRE::get, TGSounds.ROCKET_RELOAD::get, null,
                            GunStats.ProjectileKind.EXPLOSIVE,
                            true, 0.8f, 0.6f, 2.0f, 0.5f, 1.0f, false, 0.0f, null, 0, 2.2f),
                    ROCKET::get, "rocket",
                    GunExtras.explosive(60.0f, 50.0f, 2.0f, 4.0f, 0.25f)
                            .withKnockback(2.0f, false).withGuided(0.16f, 20, 80, 150.0)
                            .withExtraAmmo("rocket_hv").withMuzzle(GunExtras.FLASH_GUN)));
    // Grim Reaper: automatic 4-rocket lock-on volley.
    public static final DeferredItem<AnimatedGunItem> GRIMREAPER = ITEMS.registerItem("grimreaper",
            props -> new AnimatedGunItem(props,
                    new GunStats(60.0f, 6, 4, 40, 200, 0.05f, 1.0f, 0.01, 1, 0.0f,
                            3.0f, 5.0f, 50.0f, 0.0f, false, false,
                            TGSounds.GUIDED_FIRE::get, TGSounds.ROCKET_RELOAD::get, null,
                            GunStats.ProjectileKind.EXPLOSIVE,
                            true, 0.8f, 0.6f, 1.6f, 0.4f, 1.0f, false, 0.0f, null, 0, 2.2f),
                    ROCKET::get, "rocket",
                    GunExtras.explosive(60.0f, 50.0f, 3.0f, 5.0f, 0.25f)
                            .withKnockback(2.0f, false).withGuided(0.16f, 20, 80, 150.0)
                            .withExtraAmmo("rocket_hv").withMuzzle(GunExtras.FLASH_GUN)));
    // Sonic shotgun: 16-blade cone, pierces i-frames, heavy knockback.
    public static final DeferredItem<AnimatedGunItem> SONICSHOTGUN = ITEMS.registerItem("sonicshotgun",
            props -> new AnimatedGunItem(props,
                    new GunStats(50.0f, 12, 8, 40, 20, 0.0f, 2.0f, 0.0, 16, 0.075f,
                            5.0f, 15.0f, 35.0f, 1.0f, true, true,
                            TGSounds.SONIC_FIRE::get, TGSounds.SONIC_RELOAD::get, null,
                            GunStats.ProjectileKind.SONIC,
                            true, 0.8f, 0.6f, 2.4f, 0.7f, 0.9f, false, 0.0f, null, 0, 1.4f),
                    ENERGY_CELL::get, "energy_cell",
                    GunExtras.DEFAULT.withKnockback(2.0f, true).withMuzzle(GunExtras.FLASH_SONIC)));
    // Lasergun: hitscan, no damage drop.
    public static final DeferredItem<AnimatedGunItem> LASERGUN = ITEMS.registerItem("lasergun",
            props -> new AnimatedGunItem(props,
                    new GunStats(16.0f, 5, 45, 45, 90, 0.0f, 1.0f, 0.0, 1, 0.0f,
                            90.0f, 90.0f, 16.0f, 0.0f, false, true,
                            TGSounds.LASER_FIRE::get, TGSounds.LASER_RELOAD::get, null,
                            GunStats.ProjectileKind.LASER,
                            true, 0.75f, 0.75f, 0.5f, 0.15f, 0.7f, false, 0.0f, null, 0, 1.0f),
                    ENERGY_CELL::get, "energy_cell",
                    GunExtras.DEFAULT.withBeam(0).withMuzzle(GunExtras.FLASH_LASER)));
    // Laser pistol: one-handed cheap beam on redstone batteries.
    public static final DeferredItem<AnimatedGunItem> LASERPISTOL = ITEMS.registerItem("laserpistol",
            props -> new AnimatedGunItem(props,
                    new GunStats(9.0f, 6, 20, 40, 90, 0.025f, 1.0f, 0.0, 1, 0.0f,
                            90.0f, 90.0f, 9.0f, 0.0f, false, true,
                            TGSounds.LASERPISTOL_FIRE::get, TGSounds.LASERPISTOL_RELOAD::get, null,
                            GunStats.ProjectileKind.LASER,
                            true, 0.8f, 0.5f, 0.5f, 0.2f, 0.6f, false, 0.0f, null, 0, 0.9f),
                    REDSTONE_BATTERY::get, "redstone_battery",
                    GunExtras.DEFAULT.withBeam(0).withMuzzle(GunExtras.FLASH_LASER)));
    // Pulse rifle: 3-round energy burst.
    public static final DeferredItem<AnimatedGunItem> PULSERIFLE = ITEMS.registerItem("pulserifle",
            props -> new AnimatedGunItem(props,
                    new GunStats(18.0f, 7, 12, 45, 17, 0.024f, 4.5f, 0.0, 3, 0.015f,
                            30.0f, 45.0f, 15.0f, 1.0f, false, true,
                            TGSounds.PULSE_FIRE::get, TGSounds.PULSE_RELOAD::get, null,
                            GunStats.ProjectileKind.ADVANCED,
                            true, 0.35f, 0.5f, 0.8f, 0.3f, 0.75f, false, 0.0f, null, 0, 1.0f),
                    ADVANCED_MAGAZINE::get, "advanced_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_BLUE)));
    // Vector: 1200/min bullet hose on SMG mags.
    public static final DeferredItem<AnimatedGunItem> VECTOR = ITEMS.registerItem("vector",
            props -> new AnimatedGunItem(props,
                    new GunStats(10.0f, 1, 25, 40, 20, 0.05f, 2.0f, 0.0, 1, 0.0f,
                            17.0f, 25.0f, 8.0f, 0.5f, false, true,
                            TGSounds.VECTOR_FIRE::get, TGSounds.VECTOR_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.75f, 0.35f, 0.5f, 0.3f, 0.65f, false, 0.0f, null, 0, 1.0f),
                    SMG_MAGAZINE::get, "smg_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_RIFLE)));
    // PDW: 1200/min advanced-rocket hose.
    public static final DeferredItem<AnimatedGunItem> PDW = ITEMS.registerItem("pdw",
            props -> new AnimatedGunItem(props,
                    new GunStats(10.0f, 1, 40, 40, 12, 0.03f, 3.5f, 0.0, 1, 0.0f,
                            18.0f, 25.0f, 7.0f, 1.0f, false, true,
                            TGSounds.PDW_FIRE::get, TGSounds.PDW_RELOAD::get, null,
                            GunStats.ProjectileKind.ADVANCED,
                            true, 0.8f, 0.5f, 0.4f, 0.25f, 0.6f, false, 0.0f, null, 0, 1.0f),
                    ADVANCED_MAGAZINE::get, "advanced_magazine",
                    GunExtras.DEFAULT.withMuzzle(GunExtras.FLASH_BLUE)));
    // AS50: 8x explosive-capable sniper.
    public static final DeferredItem<AnimatedGunItem> AS50 = ITEMS.registerItem("as50",
            props -> new AnimatedGunItem(props,
                    new GunStats(45.0f, 10, 10, 80, 14, 0.0625f, 6.5f, 0.0, 1, 0.0f,
                            40.0f, 60.0f, 35.0f, 2.0f, true, true,
                            TGSounds.AS50_FIRE::get, TGSounds.AS50_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.35f, 0.125f, 2.2f, 0.5f, 0.9f, false, 0.0f, null, 0, 1.15f),
                    AS50_MAGAZINE::get, "as50_magazine",
                    GunExtras.DEFAULT.withExtraAmmo("as50_magazine_explosive", "as50_magazine_incendiary").withMuzzle(GunExtras.FLASH_RIFLE)));
}
