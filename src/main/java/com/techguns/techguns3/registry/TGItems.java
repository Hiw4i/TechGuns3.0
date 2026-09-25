package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
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
                            true, 0.85f, 0.5f, 0.7f, 0.3f, 0.55f, false),
                    PISTOL_MAGAZINE::get, "pistol_magazine"));
    public static final DeferredItem<GenericGunItem> REVOLVER = ITEMS.registerItem("revolver",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 6, 6, 45, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                            12.0f, 20.0f, 6.0f, 0.0f, true, false,
                            TGSounds.REVOLVER_FIRE::get, TGSounds.REVOLVER_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.85f, 0.5f, 1.0f, 0.4f, 0.6f, false),
                    PISTOL_ROUNDS::get, "pistol_rounds"));
    public static final DeferredItem<AnimatedGunItem> AK47 = ITEMS.registerItem("ak47",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 3, 30, 45, 60, 0.030f, 4.0f, 0.0, 1, 0.0f,
                            20.0f, 30.0f, 4.5f, 0.5f, false, true,
                            TGSounds.AK47_FIRE::get, TGSounds.AK47_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, false),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine"));
    public static final DeferredItem<GenericGunItem> M4 = ITEMS.registerItem("m4",
            props -> new GenericGunItem(props,
                    new GunStats(11.0f, 3, 30, 45, 60, 0.015f, 4.0f, 0.0, 1, 0.0f,
                            25.0f, 40.0f, 7.0f, 0.5f, false, true,
                            TGSounds.M4_FIRE::get, TGSounds.M4_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.7f, 0.45f, 0.7f, 0.3f, 0.75f, false),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine"));
    public static final DeferredItem<GenericGunItem> THOMPSON = ITEMS.registerItem("thompson",
            props -> new GenericGunItem(props,
                    new GunStats(5.0f, 3, 20, 40, 40, 0.050f, 1.75f, 0.0, 1, 0.0f,
                            15.0f, 24.0f, 3.0f, 0.0f, false, true,
                            TGSounds.THOMPSON_FIRE::get, TGSounds.THOMPSON_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.8f, 0.5f, 0.6f, 0.35f, 0.6f, false),
                    SMG_MAGAZINE::get, "smg_magazine"));
    public static final DeferredItem<AnimatedGunItem> COMBAT_SHOTGUN = ITEMS.registerItem("combat_shotgun",
            props -> new AnimatedGunItem(props,
                    new GunStats(8.0f, 14, 8, 50, 15, 0.010f, 2.5f, 0.0, 8, 0.150f,
                            2.0f, 5.0f, 3.0f, 0.5f, true, false,
                            TGSounds.COMBAT_SHOTGUN_FIRE::get, TGSounds.COMBAT_SHOTGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.8f, 0.5f, 2.8f, 0.8f, 0.9f, false),
                    SHOTGUN_ROUNDS::get, "shotgun_rounds"));
    public static final DeferredItem<AnimatedGunItem> MINIGUN = ITEMS.registerItem("minigun",
            props -> new AnimatedGunItem(props,
                    new GunStats(12.0f, 1, 200, 100, 75, 0.025f, 3.0f, 0.0, 1, 0.0f,
                            30.0f, 50.0f, 7.0f, 0.5f, false, true,
                            TGSounds.MINIGUN_FIRE::get, TGSounds.MINIGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.85f, 0.6f, 0.3f, 0.2f, 0.8f, true),
                    MINIGUN_DRUM::get, "minigun_drum"));
    public static final DeferredItem<AnimatedGunItem> TESLAGUN = ITEMS.registerItem("teslagun",
            props -> new AnimatedGunItem(props,
                    new GunStats(9.0f, 8, 25, 45, 60, 0.0f, 80.0f, 0.0, 1, 0.0f,
                            0.0f, 0.0f, 9.0f, 0.0f, false, true,
                            TGSounds.TESLAGUN_FIRE::get, TGSounds.TESLAGUN_RELOAD::get, null,
                            GunStats.ProjectileKind.ELECTRIC,
                            true, 0.8f, 0.5f, 0.4f, 0.1f, 0.6f, false),
                    ENERGY_CELL::get, "energy_cell"));
    public static final DeferredItem<GenericGunItem> BOLT_ACTION = ITEMS.registerItem("bolt_action",
            props -> new GenericGunItem(props,
                    new GunStats(30.0f, 25, 6, 50, 90, 0.050f, 6.5f, 0.0, 1, 0.0f,
                            40.0f, 60.0f, 20.0f, 1.0f, true, false,
                            TGSounds.BOLT_ACTION_FIRE::get, TGSounds.BOLT_ACTION_RELOAD::get,
                            TGSounds.BOLT_ACTION_RECHAMBER::get,
                            GunStats.ProjectileKind.BALLISTIC,
                            true, 0.35f, 0.25f, 2.2f, 0.5f, 0.9f, false),
                    RIFLE_ROUNDS::get, "rifle_rounds"));
}
