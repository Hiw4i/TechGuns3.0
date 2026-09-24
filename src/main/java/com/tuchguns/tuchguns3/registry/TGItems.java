package com.tuchguns.tuchguns3.registry;

import com.tuchguns.tuchguns3.TuchGuns3;
import com.tuchguns.tuchguns3.item.GenericGunItem;
import com.tuchguns.tuchguns3.item.GunStats;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MVP wave-1 items. Gun tunables are the 1.12 {@code GenericGun(...)} constructor args
 * (damage / minFiretime / clipsize / reloadtime / TTL / accuracy / speed / damage drop /
 * penetration), so balance survives the port and later waves only re-add behaviour.
 *
 * <p>Ammo model, simplified for wave 1: magazine-fed guns reload one magazine item into a
 * full mag; round-fed guns consume loose rounds up to the clip size. Empty magazines,
 * the Ammo Press and incendiary variants return with the machine wave.</p>
 */
public final class TGItems {
    private TGItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TuchGuns3.MODID);

    // --- wave-1 ammo (declared first: guns hold suppliers to these) ---
    public static final DeferredItem<Item> PISTOL_ROUNDS = ITEMS.registerSimpleItem("pistol_rounds");
    public static final DeferredItem<Item> RIFLE_ROUNDS = ITEMS.registerSimpleItem("rifle_rounds");
    public static final DeferredItem<Item> SHOTGUN_ROUNDS = ITEMS.registerSimpleItem("shotgun_rounds");
    public static final DeferredItem<Item> PISTOL_MAGAZINE = ITEMS.registerSimpleItem("pistol_magazine");
    public static final DeferredItem<Item> ASSAULT_RIFLE_MAGAZINE = ITEMS.registerSimpleItem("assault_rifle_magazine");
    public static final DeferredItem<Item> SMG_MAGAZINE = ITEMS.registerSimpleItem("smg_magazine");

    // --- wave-1 ingots (smelted from the new ores; copper uses the vanilla chain) ---
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.registerSimpleItem("titanium_ingot");
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.registerSimpleItem("uranium_ingot");

    // --- wave-1 guns (all plain GenericProjectile shooters in 1.12, no charge/loop logic) ---
    public static final DeferredItem<GenericGunItem> PISTOL = ITEMS.registerItem("pistol",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 4, 18, 35, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                            18.0f, 25.0f, 5.0f, 0.0f, true, true,
                            TGSounds.PISTOL_FIRE::get, TGSounds.PISTOL_RELOAD::get, null),
                    PISTOL_MAGAZINE::get, "pistol_magazine"));
    public static final DeferredItem<GenericGunItem> REVOLVER = ITEMS.registerItem("revolver",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 6, 6, 45, 40, 0.025f, 2.0f, 0.0, 1, 0.0f,
                            12.0f, 20.0f, 6.0f, 0.0f, true, false,
                            TGSounds.REVOLVER_FIRE::get, TGSounds.REVOLVER_RELOAD::get, null),
                    PISTOL_ROUNDS::get, "pistol_rounds"));
    public static final DeferredItem<GenericGunItem> AK47 = ITEMS.registerItem("ak47",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 3, 30, 45, 60, 0.030f, 4.0f, 0.0, 1, 0.0f,
                            20.0f, 30.0f, 4.5f, 0.5f, false, true,
                            TGSounds.AK47_FIRE::get, TGSounds.AK47_RELOAD::get, null),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine"));
    public static final DeferredItem<GenericGunItem> M4 = ITEMS.registerItem("m4",
            props -> new GenericGunItem(props,
                    new GunStats(11.0f, 3, 30, 45, 60, 0.015f, 4.0f, 0.0, 1, 0.0f,
                            25.0f, 40.0f, 7.0f, 0.5f, false, true,
                            TGSounds.M4_FIRE::get, TGSounds.M4_RELOAD::get, null),
                    ASSAULT_RIFLE_MAGAZINE::get, "assault_rifle_magazine"));
    public static final DeferredItem<GenericGunItem> THOMPSON = ITEMS.registerItem("thompson",
            props -> new GenericGunItem(props,
                    new GunStats(5.0f, 3, 20, 40, 40, 0.050f, 1.75f, 0.0, 1, 0.0f,
                            15.0f, 24.0f, 3.0f, 0.0f, false, true,
                            TGSounds.THOMPSON_FIRE::get, TGSounds.THOMPSON_RELOAD::get, null),
                    SMG_MAGAZINE::get, "smg_magazine"));
    public static final DeferredItem<GenericGunItem> COMBAT_SHOTGUN = ITEMS.registerItem("combat_shotgun",
            props -> new GenericGunItem(props,
                    new GunStats(8.0f, 14, 8, 50, 15, 0.010f, 2.5f, 0.0, 8, 0.150f,
                            2.0f, 5.0f, 3.0f, 0.5f, true, false,
                            TGSounds.COMBAT_SHOTGUN_FIRE::get, TGSounds.COMBAT_SHOTGUN_RELOAD::get, null),
                    SHOTGUN_ROUNDS::get, "shotgun_rounds"));
    public static final DeferredItem<GenericGunItem> BOLT_ACTION = ITEMS.registerItem("bolt_action",
            props -> new GenericGunItem(props,
                    new GunStats(30.0f, 25, 6, 50, 90, 0.050f, 6.5f, 0.0, 1, 0.0f,
                            40.0f, 60.0f, 20.0f, 1.0f, true, false,
                            TGSounds.BOLT_ACTION_FIRE::get, TGSounds.BOLT_ACTION_RELOAD::get,
                            TGSounds.BOLT_ACTION_RECHAMBER::get),
                    RIFLE_ROUNDS::get, "rifle_rounds"));
}
