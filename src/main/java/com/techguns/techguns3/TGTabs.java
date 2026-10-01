package com.techguns.techguns3;

import com.techguns.techguns3.registry.TGBlocks;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Single MVP creative tab. 1.12 used the vanilla combat tab + Galacticraft tabs;
 * on 26.3 we get our own tab so guns/ammo/ores stay together.
 */
public final class TGTabs {
    private TGTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TechGuns3.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.techguns3"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> TGItems.PISTOL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Wave-1 guns
                        output.accept(TGItems.PISTOL.get());
                        output.accept(TGItems.AK47.get());
                        output.accept(TGItems.COMBAT_SHOTGUN.get());
                        output.accept(TGItems.MINIGUN.get());
                        output.accept(TGItems.TESLAGUN.get());
                        output.accept(TGItems.BIOGUN.get());
                        output.accept(TGItems.FLAMETHROWER.get());
                        // Wave-2 exotic guns
                        output.accept(TGItems.TFG.get());
                        output.accept(TGItems.NUCLEARDEATHRAY.get());
                        output.accept(TGItems.GAUSSRIFLE.get());
                        output.accept(TGItems.ROCKETLAUNCHER.get());
                        output.accept(TGItems.GUIDEDMISSILELAUNCHER.get());
                        output.accept(TGItems.GRIMREAPER.get());
                        output.accept(TGItems.SONICSHOTGUN.get());
                        output.accept(TGItems.LASERGUN.get());
                        output.accept(TGItems.LASERPISTOL.get());
                        output.accept(TGItems.PULSERIFLE.get());
                        output.accept(TGItems.VECTOR.get());
                        output.accept(TGItems.PDW.get());
                        output.accept(TGItems.AS50.get());
                        // Wave-1 ammo
                        output.accept(TGItems.PISTOL_ROUNDS.get());
                        output.accept(TGItems.RIFLE_ROUNDS.get());
                        output.accept(TGItems.SHOTGUN_ROUNDS.get());
                        output.accept(TGItems.PISTOL_MAGAZINE.get());
                        output.accept(TGItems.ASSAULT_RIFLE_MAGAZINE.get());
                        output.accept(TGItems.SMG_MAGAZINE.get());
                        output.accept(TGItems.PISTOL_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.ASSAULT_RIFLE_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.SMG_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.TURRET_ARMOR_IRON.get());
                        output.accept(TGItems.MINIGUN_DRUM.get());
                        output.accept(TGItems.MINIGUN_DRUM_EMPTY.get());
                        output.accept(TGItems.ENERGY_CELL.get());
                        output.accept(TGItems.ENERGY_CELL_EMPTY.get());
                        output.accept(TGItems.BIO_TANK.get());
                        output.accept(TGItems.BIO_TANK_EMPTY.get());
                        output.accept(TGItems.FUEL_TANK.get());
                        output.accept(TGItems.FUEL_TANK_EMPTY.get());
                        // Wave-2 ammo
                        output.accept(TGItems.NUCLEAR_POWERCELL.get());
                        output.accept(TGItems.NUCLEAR_POWERCELL_EMPTY.get());
                        output.accept(TGItems.ROCKET.get());
                        output.accept(TGItems.ROCKET_NUKE.get());
                        output.accept(TGItems.ROCKET_HV.get());
                        output.accept(TGItems.ADVANCED_MAGAZINE.get());
                        output.accept(TGItems.ADVANCED_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.ADVANCED_ROUNDS.get());
                        output.accept(TGItems.AS50_MAGAZINE.get());
                        output.accept(TGItems.AS50_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.AS50_MAGAZINE_INCENDIARY.get());
                        output.accept(TGItems.AS50_MAGAZINE_EXPLOSIVE.get());
                        output.accept(TGItems.SNIPER_ROUNDS.get());
                        output.accept(TGItems.SNIPER_ROUNDS_INCENDIARY.get());
                        output.accept(TGItems.SNIPER_ROUNDS_EXPLOSIVE.get());
                        output.accept(TGItems.GAUSS_SLUGS.get());
                        output.accept(TGItems.GAUSS_MAGAZINE.get());
                        output.accept(TGItems.GAUSS_MAGAZINE_EMPTY.get());
                        output.accept(TGItems.REDSTONE_BATTERY.get());
                        output.accept(TGItems.REDSTONE_BATTERY_EMPTY.get());
                        // Wave-1 ingots
                        output.accept(TGItems.TIN_INGOT.get());
                        output.accept(TGItems.LEAD_INGOT.get());
                        output.accept(TGItems.TITANIUM_INGOT.get());
                        output.accept(TGItems.URANIUM_INGOT.get());
                        // Wave-1 blocks
                        output.accept(TGBlocks.COPPER_ORE_ITEM.get());
                        output.accept(TGBlocks.TIN_ORE_ITEM.get());
                        output.accept(TGBlocks.LEAD_ORE_ITEM.get());
                        output.accept(TGBlocks.URANIUM_ORE_ITEM.get());
                        output.accept(TGBlocks.TITANIUM_ORE_ITEM.get());
                        output.accept(TGBlocks.METAL_PANEL_ITEM.get());
                        output.accept(TGBlocks.TURRET_BASE_ITEM.get());
                    })
                    .build());
}
